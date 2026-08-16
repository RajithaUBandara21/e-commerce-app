package com.rajitha.ecommerce.service.serviceImpl;

import com.rajitha.ecommerce.client.feign.SellerClient;
import com.rajitha.ecommerce.dto.PurchaseResponseDTO;
import com.rajitha.ecommerce.dto.SellerPayoutResponseDTO;
import com.rajitha.ecommerce.entity.SellerPayout;
import com.rajitha.ecommerce.enums.PayoutStatus;
import com.rajitha.ecommerce.mapper.SellerPayoutMapper;
import com.rajitha.ecommerce.repository.SellerPayoutRepository;
import com.rajitha.ecommerce.service.SellerPayoutService;
import com.rajitha.ecommerce.service.StripeTransferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SellerPayoutServiceImpl implements SellerPayoutService {

    private final SellerPayoutRepository sellerPayoutRepository;
    private final SellerPayoutMapper sellerPayoutMapper;
    private final SellerClient sellerClient;
    private final StripeTransferService stripeTransferService;

    // Flat platform commission — not a pricing-rules engine. Field initializer
    // doubles as the default for plain-Java unit tests that don't go through
    // Spring's @Value resolution (new SellerPayoutServiceImpl(...) directly).
    @Value("${platform.commission-rate:0.10}")
    private BigDecimal commissionRate = new BigDecimal("0.10");

    @Override
    public void recordPayoutsForOrder(String orderReference, List<PurchaseResponseDTO> products) {
        if (products == null || products.isEmpty()) {
            return;
        }

        Map<String, BigDecimal> grossBySeller = products.stream()
                .filter(product -> product.sellerId() != null)
                .collect(Collectors.groupingBy(
                        PurchaseResponseDTO::sellerId,
                        Collectors.reducing(BigDecimal.ZERO,
                                product -> product.price().multiply(BigDecimal.valueOf(product.quantity())),
                                BigDecimal::add)
                ));

        grossBySeller.forEach((sellerId, gross) -> {
            var commission = gross.multiply(commissionRate).setScale(2, RoundingMode.HALF_UP);
            var net = gross.setScale(2, RoundingMode.HALF_UP).subtract(commission);

            sellerPayoutRepository.save(SellerPayout.builder()
                    .sellerId(sellerId)
                    .orderReference(orderReference)
                    .grossAmount(gross.setScale(2, RoundingMode.HALF_UP))
                    .commissionAmount(commission)
                    .netAmount(net)
                    .status(PayoutStatus.PENDING)
                    .build());

            log.info("Recorded payout for seller {} on order {}: gross={}, commission={}, net={}",
                    sellerId, orderReference, gross, commission, net);
        });
    }

    @Override
    public List<SellerPayoutResponseDTO> findBySellerId(String sellerId) {
        return sellerPayoutRepository.findBySellerId(sellerId).stream()
                .map(sellerPayoutMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<SellerPayoutResponseDTO> findAll() {
        return sellerPayoutRepository.findAll().stream()
                .map(sellerPayoutMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public void settlePendingPayouts() {
        var pending = sellerPayoutRepository.findByStatus(PayoutStatus.PENDING);
        log.info("Settling {} pending payout(s)", pending.size());

        for (var payout : pending) {
            settleOne(payout);
        }
    }

    private void settleOne(SellerPayout payout) {
        try {
            var seller = sellerClient.findByKeycloakUserId(payout.getSellerId());

            if (!seller.payoutsEnabled() || seller.stripeAccountId() == null) {
                payout.setStatus(PayoutStatus.FAILED);
                payout.setFailureReason("Seller has not completed Stripe Connect onboarding (payoutsEnabled=false)");
                sellerPayoutRepository.save(payout);
                return;
            }

            var result = stripeTransferService.transfer(
                    payout.getNetAmount(), "usd", seller.stripeAccountId(), payout.getOrderReference());

            if (result.success()) {
                payout.setStatus(PayoutStatus.PAID);
                payout.setStripeTransferId(result.stripeTransferId());
            } else {
                payout.setStatus(PayoutStatus.FAILED);
                payout.setFailureReason(result.failureReason());
            }
            sellerPayoutRepository.save(payout);
        } catch (Exception e) {
            // Seller lookup failure (seller-service down, unknown seller, etc.) — leave
            // this one for the next settlement run rather than crashing the whole batch.
            log.warn("Could not settle payout {} for seller {} :: {}", payout.getId(), payout.getSellerId(), e.getMessage());
        }
    }
}
