package com.rajitha.ecommerce.kafka;

import com.rajitha.ecommerce.dto.OrderConformationDTO;
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

    private final KafkaTemplate<String, OrderConformationDTO> kafkaTemplate;

    public void sendOrderConformation(  OrderConformationDTO orderConformationDTO){
    log.info("Sending order conformation : {}", orderConformationDTO);
        Message<OrderConformationDTO> message = MessageBuilder.withPayload(orderConformationDTO)
                .setHeader(KafkaHeaders.TOPIC, "order-topic")
                .build();
        kafkaTemplate.send(message);
    }

}
