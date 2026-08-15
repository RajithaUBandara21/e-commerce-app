package com.rajitha.ecommerce.messaging.consumer;

import com.rajitha.ecommerce.dto.PurchaseRequestDTO;
import com.rajitha.ecommerce.dto.StockReleaseEventDTO;
import com.rajitha.ecommerce.service.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class StockReleaseConsumerTest {

    @InjectMocks
    StockReleaseConsumer stockReleaseConsumer;
    @Mock ProductService productService;

    @Test
    void shouldDelegateToProductServiceReleaseStock(){

        List<PurchaseRequestDTO> products = List.of(new PurchaseRequestDTO(1, 2.0));
        StockReleaseEventDTO event = StockReleaseEventDTO.builder().orderReference("reference").products(products).build();

        stockReleaseConsumer.consumeStockRelease(event);

        Mockito.verify(productService, Mockito.times(1)).releaseStock(products);
    }
}
