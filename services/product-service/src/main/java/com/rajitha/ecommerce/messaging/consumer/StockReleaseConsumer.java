package com.rajitha.ecommerce.messaging.consumer;

import com.rajitha.ecommerce.dto.StockReleaseEventDTO;
import com.rajitha.ecommerce.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockReleaseConsumer {

    private final ProductService productService;

    @KafkaListener(topics = "stock-release-topic", groupId = "productServiceReleaseGroup")
    public void consumeStockRelease(StockReleaseEventDTO event) {
        log.info("Received stock release event :: <{}>", event);
        productService.releaseStock(event.products());
    }
}
