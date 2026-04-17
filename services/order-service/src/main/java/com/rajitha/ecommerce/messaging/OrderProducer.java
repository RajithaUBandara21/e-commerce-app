package com.rajitha.ecommerce.messaging;

import com.rajitha.ecommerce.dto.OrderConfirmationDTO;
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
public class OrderProducer  {

    private final KafkaTemplate<String, OrderConfirmationDTO> kafkaTemplate;

    public void sendOrderConformation(  OrderConfirmationDTO orderConfirmationDTO){
    log.info("Sending order conformation : {}", orderConfirmationDTO);
        Message<OrderConfirmationDTO> message = MessageBuilder
                .withPayload(orderConfirmationDTO)
                .setHeader(KafkaHeaders.TOPIC, "order-topic")
                .build();
        kafkaTemplate.send(message);
    }

}
