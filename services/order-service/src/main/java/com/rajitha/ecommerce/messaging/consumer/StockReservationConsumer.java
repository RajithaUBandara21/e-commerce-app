package com.rajitha.ecommerce.messaging.consumer;

import com.rajitha.ecommerce.dto.OrderConfirmationDTO;
import com.rajitha.ecommerce.dto.StockReservationResultEventDTO;
import com.rajitha.ecommerce.enums.OrderStatus;
import com.rajitha.ecommerce.messaging.OrderProducer;
import com.rajitha.ecommerce.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockReservationConsumer {

    private final OrderRepository orderRepository;
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
}
