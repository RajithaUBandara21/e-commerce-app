package com.rajitha.ecommerce.messaging.consumer;

import com.rajitha.ecommerce.dto.PaymentResultEventDTO;
import com.rajitha.ecommerce.dto.PurchaseRequestDTO;
import com.rajitha.ecommerce.dto.StockReleaseEventDTO;
import com.rajitha.ecommerce.enums.OrderStatus;
import com.rajitha.ecommerce.messaging.StockReleaseProducer;
import com.rajitha.ecommerce.repository.OrderRepository;
import com.rajitha.ecommerce.service.OrderLineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentResultConsumer {

    private final OrderRepository orderRepository;
    private final OrderLineService orderLineService;
    private final StockReleaseProducer stockReleaseProducer;

    @KafkaListener(topics = "payment-topic", groupId = "orderServicePaymentGroup")
    public void consumePaymentResult(PaymentResultEventDTO event) {
        log.info("Received payment result :: <{}>", event);

        var order = orderRepository.findByReference(event.orderReference()).orElse(null);
        if (order == null) {
            log.warn("No order found for reference <{}>, ignoring payment result", event.orderReference());
            return;
        }

        if (event.success()) {
            order.setStatus(OrderStatus.CONFIRMED);
            orderRepository.save(order);
            return;
        }

        order.setStatus(OrderStatus.PAYMENT_FAILED);
        orderRepository.save(order);

        var linesToRelease = orderLineService.findOrderLineByOrderId(order.getId()).stream()
                .map(line -> new PurchaseRequestDTO(line.variantId(), line.quantity()))
                .toList();

        stockReleaseProducer.sendStockRelease(
                StockReleaseEventDTO.builder()
                        .orderReference(order.getReference())
                        .products(linesToRelease)
                        .build()
        );
    }
}
