package com.rajitha.ecommerce.messaging.consumer;

import com.rajitha.ecommerce.dto.OrderCreatedEventDTO;
import com.rajitha.ecommerce.dto.StockReservationResultEventDTO;
import com.rajitha.ecommerce.exeption.ProductPurchaseException;
import com.rajitha.ecommerce.messaging.StockReservationResultProducer;
import com.rajitha.ecommerce.service.ProductService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderCreatedConsumer {

    private final ProductService productService;
    private final StockReservationResultProducer stockReservationResultProducer;
    private final MeterRegistry meterRegistry;

    @KafkaListener(topics = "order-created-topic", groupId = "productServiceOrderGroup")
    public void consumeOrderCreated(OrderCreatedEventDTO event) {
        log.info("Received order created event :: <{}>", event);

        try {
            var purchased = productService.purchaseProductService(event.products());
            stockReservationResultProducer.sendStockReservationResult(
                    StockReservationResultEventDTO.builder()
                            .orderReference(event.orderReference())
                            .success(true)
                            .totalAmount(event.totalAmount())
                            .paymentMethode(event.paymentMethode())
                            .stripePaymentMethodId(event.stripePaymentMethodId())
                            .customer(event.customer())
                            .products(purchased)
                            .build()
            );
            reservationCounter("success").increment();
        } catch (ProductPurchaseException e) {
            log.warn("Stock reservation failed for order <{}> :: {}", event.orderReference(), e.getMessage());
            stockReservationResultProducer.sendStockReservationResult(
                    StockReservationResultEventDTO.builder()
                            .orderReference(event.orderReference())
                            .success(false)
                            .reason(e.getMessage())
                            .build()
            );
            reservationCounter("failure").increment();
        }
    }

    // Saga decision point: how often checkout actually reserves stock vs. loses
    // to a stockout/optimistic-lock race — not derivable from generic HTTP/Kafka
    // auto-instrumentation, since this is a business outcome, not a technical one.
    private Counter reservationCounter(String result) {
        return Counter.builder("stock_reservation_total")
                .description("Stock reservation attempts from the checkout saga, by result")
                .tag("result", result)
                .register(meterRegistry);
    }
}
