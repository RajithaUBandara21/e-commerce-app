package com.rajitha.ecommerce.messaging;

import com.rajitha.ecommerce.dto.OrderCreatedEventDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderCreatedProducer {

    private final KafkaTemplate<String, OrderCreatedEventDTO> kafkaTemplate;

    public void sendOrderCreated(OrderCreatedEventDTO orderCreatedEventDTO) {
        log.info("Sending order created event : {}", orderCreatedEventDTO);
        Message<OrderCreatedEventDTO> message = MessageBuilder
                .withPayload(orderCreatedEventDTO)
                .setHeader(KafkaHeaders.TOPIC, "order-created-topic")
                .build();
        kafkaTemplate.send(message);
    }
}
