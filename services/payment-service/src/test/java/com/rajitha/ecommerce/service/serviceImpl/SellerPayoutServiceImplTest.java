package com.rajitha.ecommerce.service.serviceImpl;

import com.rajitha.ecommerce.client.feign.SellerClient;
import com.rajitha.ecommerce.dto.PurchaseResponseDTO;
import com.rajitha.ecommerce.dto.SellerLookupResponseDTO;
import com.rajitha.ecommerce.entity.SellerPayout;
import com.rajitha.ecommerce.enums.PayoutStatus;
import com.rajitha.ecommerce.mapper.SellerPayoutMapper;
import com.rajitha.ecommerce.repository.SellerPayoutRepository;
import com.rajitha.ecommerce.service.StripeTransferService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class SellerPayoutServiceImplTest {

    @InjectMocks
    private SellerPayoutServiceImpl sellerPayoutService;

    @Mock
    private SellerPayoutRepository sellerPayoutRepository;
    @Mock
    private SellerPayoutMapper sellerPayoutMapper;
    @Mock
    private SellerClient sellerClient;
    @Mock
    private StripeTransferService stripeTransferService;

    @Test
    void shouldSplitGrossAmountBySellerAndApplyDefaultTenPercentCommission() {
        var products = List.of(
                PurchaseResponseDTO.builder().variantId(1).sellerId("seller-1")
                        .price(new BigDecimal("50.00")).quantity(2.0).build(),
                PurchaseResponseDTO.builder().variantId(2).sellerId("seller-2")
                        .price(new BigDecimal("30.00")).quantity(1.0).build()
        );

        sellerPayoutService.recordPayoutsForOrder("order-ref", products);

        var captor = ArgumentCaptor.forClass(SellerPayout.class);
        Mockito.verify(sellerPayoutRepository, Mockito.times(2)).save(captor.capture());

        var bySeller = captor.getAllValues().stream()
                .collect(java.util.stream.Collectors.toMap(SellerPayout::getSellerId, p -> p));

        var seller1Payout = bySeller.get("seller-1");
        Assertions.assertEquals(new BigDecimal("100.00"), seller1Payout.getGrossAmount());
        Assertions.assertEquals(new BigDecimal("10.00"), seller1Payout.getCommissionAmount());
        Assertions.assertEquals(new BigDecimal("90.00"), seller1Payout.getNetAmount());
        Assertions.assertEquals(PayoutStatus.PENDING, seller1Payout.getStatus());
        Assertions.assertEquals("order-ref", seller1Payout.getOrderReference());

        var seller2Payout = bySeller.get("seller-2");
        Assertions.assertEquals(new BigDecimal("30.00"), seller2Payout.getGrossAmount());
        Assertions.assertEquals(new BigDecimal("3.00"), seller2Payout.getCommissionAmount());
        Assertions.assertEquals(new BigDecimal("27.00"), seller2Payout.getNetAmount());
    }

    @Test
    void shouldCombineMultipleLinesFromTheSameSellerIntoOnePayout() {
        var products = List.of(
                PurchaseResponseDTO.builder().variantId(1).sellerId("seller-1")
                        .price(new BigDecimal("20.00")).quantity(1.0).build(),
                PurchaseResponseDTO.builder().variantId(2).sellerId("seller-1")
                        .price(new BigDecimal("10.00")).quantity(3.0).build()
        );

        sellerPayoutService.recordPayoutsForOrder("order-ref", products);

        var captor = ArgumentCaptor.forClass(SellerPayout.class);
        Mockito.verify(sellerPayoutRepository, Mockito.times(1)).save(captor.capture());

        Assertions.assertEquals(new BigDecimal("50.00"), captor.getValue().getGrossAmount());
    }

    @Test
    void shouldSkipLinesWithNoSellerId() {
        var products = List.of(
                PurchaseResponseDTO.builder().variantId(1).sellerId(null)
                        .price(new BigDecimal("20.00")).quantity(1.0).build()
        );

        sellerPayoutService.recordPayoutsForOrder("order-ref", products);

        Mockito.verify(sellerPayoutRepository, Mockito.never()).save(Mockito.any(SellerPayout.class));
    }

    @Test
    void shouldDoNothingForEmptyOrNullProductList() {
        sellerPayoutService.recordPayoutsForOrder("order-ref", List.of());
        sellerPayoutService.recordPayoutsForOrder("order-ref", null);

        Mockito.verify(sellerPayoutRepository, Mockito.never()).save(Mockito.any(SellerPayout.class));
    }

    @Test
    void shouldMarkPayoutPaidWhenTransferSucceeds() {
        var payout = SellerPayout.builder().id(1).sellerId("seller-1").orderReference("order-ref")
                .netAmount(new BigDecimal("90.00")).status(PayoutStatus.PENDING).build();

        var seller = new SellerLookupResponseDTO(1, "seller-1", "ACTIVE", "acct_123", true, true);

        Mockito.when(sellerPayoutRepository.findByStatus(PayoutStatus.PENDING)).thenReturn(List.of(payout));
        Mockito.when(sellerClient.findByKeycloakUserId("seller-1")).thenReturn(seller);
        Mockito.when(stripeTransferService.transfer(new BigDecimal("90.00"), "usd", "acct_123", "order-ref"))
                .thenReturn(StripeTransferService.TransferResult.success("tr_123"));

        sellerPayoutService.settlePendingPayouts();

        Assertions.assertEquals(PayoutStatus.PAID, payout.getStatus());
        Assertions.assertEquals("tr_123", payout.getStripeTransferId());
        Mockito.verify(sellerPayoutRepository).save(payout);
    }

    @Test
    void shouldMarkPayoutFailedWhenSellerHasNotFinishedOnboarding() {
        var payout = SellerPayout.builder().id(1).sellerId("seller-1").orderReference("order-ref")
                .netAmount(new BigDecimal("90.00")).status(PayoutStatus.PENDING).build();

        var seller = new SellerLookupResponseDTO(1, "seller-1", "ACTIVE", null, false, false);

        Mockito.when(sellerPayoutRepository.findByStatus(PayoutStatus.PENDING)).thenReturn(List.of(payout));
        Mockito.when(sellerClient.findByKeycloakUserId("seller-1")).thenReturn(seller);

        sellerPayoutService.settlePendingPayouts();

        Assertions.assertEquals(PayoutStatus.FAILED, payout.getStatus());
        Assertions.assertNotNull(payout.getFailureReason());
        Mockito.verify(stripeTransferService, Mockito.never()).transfer(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    void shouldMarkPayoutFailedWhenTransferFails() {
        var payout = SellerPayout.builder().id(1).sellerId("seller-1").orderReference("order-ref")
                .netAmount(new BigDecimal("90.00")).status(PayoutStatus.PENDING).build();

        var seller = new SellerLookupResponseDTO(1, "seller-1", "ACTIVE", "acct_123", true, true);

        Mockito.when(sellerPayoutRepository.findByStatus(PayoutStatus.PENDING)).thenReturn(List.of(payout));
        Mockito.when(sellerClient.findByKeycloakUserId("seller-1")).thenReturn(seller);
        Mockito.when(stripeTransferService.transfer(new BigDecimal("90.00"), "usd", "acct_123", "order-ref"))
                .thenReturn(StripeTransferService.TransferResult.failure("insufficient platform balance"));

        sellerPayoutService.settlePendingPayouts();

        Assertions.assertEquals(PayoutStatus.FAILED, payout.getStatus());
        Assertions.assertEquals("insufficient platform balance", payout.getFailureReason());
    }

    @Test
    void shouldLeavePayoutPendingWhenSellerLookupThrows() {
        var payout = SellerPayout.builder().id(1).sellerId("seller-1").orderReference("order-ref")
                .netAmount(new BigDecimal("90.00")).status(PayoutStatus.PENDING).build();

        Mockito.when(sellerPayoutRepository.findByStatus(PayoutStatus.PENDING)).thenReturn(List.of(payout));
        Mockito.when(sellerClient.findByKeycloakUserId("seller-1")).thenThrow(new RuntimeException("seller-service unavailable"));

        sellerPayoutService.settlePendingPayouts();

        Assertions.assertEquals(PayoutStatus.PENDING, payout.getStatus());
        Mockito.verify(sellerPayoutRepository, Mockito.never()).save(Mockito.any(SellerPayout.class));
    }
}
