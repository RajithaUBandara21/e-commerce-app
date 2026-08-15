package com.rajitha.ecommerce.messaging;

import com.rajitha.ecommerce.dto.StockReservationResultEventDTO;
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
public class StockReservationResultProducer {

    private final KafkaTemplate<String, StockReservationResultEventDTO> kafkaTemplate;

    public void sendStockReservationResult(StockReservationResultEventDTO event) {
        log.info("Sending stock reservation result : {}", event);
        Message<StockReservationResultEventDTO> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, "stock-topic")
                .build();
        kafkaTemplate.send(message);
    }
}
