package com.rajitha.ecommerce.messaging;

import com.rajitha.ecommerce.dto.StockReleaseEventDTO;
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
public class StockReleaseProducer {

    private final KafkaTemplate<String, StockReleaseEventDTO> kafkaTemplate;

    public void sendStockRelease(StockReleaseEventDTO stockReleaseEventDTO) {
        log.info("Sending stock release event : {}", stockReleaseEventDTO);
        Message<StockReleaseEventDTO> message = MessageBuilder
                .withPayload(stockReleaseEventDTO)
                .setHeader(KafkaHeaders.TOPIC, "stock-release-topic")
                .build();
        kafkaTemplate.send(message);
    }
}
