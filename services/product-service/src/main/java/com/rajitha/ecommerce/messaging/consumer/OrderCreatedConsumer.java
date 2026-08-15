package com.rajitha.ecommerce.messaging.consumer;

import com.rajitha.ecommerce.dto.OrderCreatedEventDTO;
import com.rajitha.ecommerce.dto.StockReservationResultEventDTO;
import com.rajitha.ecommerce.exeption.ProductPurchaseException;
import com.rajitha.ecommerce.messaging.StockReservationResultProducer;
import com.rajitha.ecommerce.service.ProductService;
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
        } catch (ProductPurchaseException e) {
            log.warn("Stock reservation failed for order <{}> :: {}", event.orderReference(), e.getMessage());
            stockReservationResultProducer.sendStockReservationResult(
                    StockReservationResultEventDTO.builder()
                            .orderReference(event.orderReference())
                            .success(false)
                            .reason(e.getMessage())
                            .build()
            );
        }
    }
}
