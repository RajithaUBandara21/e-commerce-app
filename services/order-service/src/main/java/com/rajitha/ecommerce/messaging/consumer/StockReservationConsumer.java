package com.rajitha.ecommerce.messaging.consumer;

import com.rajitha.ecommerce.dto.OrderConfirmationDTO;
import com.rajitha.ecommerce.dto.PurchaseResponseDTO;
import com.rajitha.ecommerce.dto.StockReservationResultEventDTO;
import com.rajitha.ecommerce.enums.OrderStatus;
import com.rajitha.ecommerce.messaging.OrderProducer;
import com.rajitha.ecommerce.repository.OrderLineRepository;
import com.rajitha.ecommerce.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockReservationConsumer {

    private final OrderRepository orderRepository;
    private final OrderLineRepository orderLineRepository;
    private final OrderProducer orderProducer;

    @KafkaListener(topics = "stock-topic", groupId = "orderServiceStockGroup")
    public void consumeStockReservationResult(StockReservationResultEventDTO event) {
        log.info("Received stock reservation result :: <{}>", event);

        if (!event.success()) {
            orderRepository.findByReference(event.orderReference()).ifPresent(order -> {
                order.setStatus(OrderStatus.CANCELLED);
                orderRepository.save(order);
            });
            return;
        }

        enrichOrderLinesWithSellerId(event);

        orderProducer.sendOrderConformation(
                OrderConfirmationDTO.builder()
                        .orderReference(event.orderReference())
                        .totalAmount(event.totalAmount())
                        .paymentMethode(event.paymentMethode())
                        .customerResponseDTO(event.customer())
                        .products(event.products())
                        .build()
        );
    }

    // OrderLines are created at order-request time from just {variantId, quantity} —
    // order-service doesn't know which seller owns a variant until product-service
    // resolves it during stock reservation. This is the only point that resolution
    // is available, so it's where sellerId gets backfilled onto the already-persisted
    // lines (used by payment-service's payout ledger and, later, seller order views).
    private void enrichOrderLinesWithSellerId(StockReservationResultEventDTO event) {
        if (event.products() == null || event.products().isEmpty()) {
            return;
        }
        orderRepository.findByReference(event.orderReference()).ifPresent(order -> {
            Map<Integer, String> sellerByVariantId = event.products().stream()
                    .filter(product -> product.sellerId() != null)
                    .collect(Collectors.toMap(PurchaseResponseDTO::variantId, PurchaseResponseDTO::sellerId, (a, b) -> a));

            var lines = orderLineRepository.findOrderLinesByOrderId(order.getId());
            lines.forEach(line -> {
                var sellerId = sellerByVariantId.get(line.getVariantId());
                if (sellerId != null) {
                    line.setSellerId(sellerId);
                    orderLineRepository.save(line);
                }
            });
        });
    }
}
