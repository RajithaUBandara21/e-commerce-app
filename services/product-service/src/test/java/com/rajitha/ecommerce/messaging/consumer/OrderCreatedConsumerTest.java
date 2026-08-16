package com.rajitha.ecommerce.messaging.consumer;

import com.rajitha.ecommerce.dto.OrderCreatedEventDTO;
import com.rajitha.ecommerce.dto.ProductPurchaseResponseDTO;
import com.rajitha.ecommerce.dto.PurchaseRequestDTO;
import com.rajitha.ecommerce.dto.StockReservationResultEventDTO;
import com.rajitha.ecommerce.exeption.ProductPurchaseException;
import com.rajitha.ecommerce.messaging.StockReservationResultProducer;
import com.rajitha.ecommerce.service.ProductService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class OrderCreatedConsumerTest {

    OrderCreatedConsumer orderCreatedConsumer;
    @Mock ProductService productService;
    @Mock StockReservationResultProducer stockReservationResultProducer;
    final MeterRegistry meterRegistry = new SimpleMeterRegistry();

    @BeforeEach
    void setUp() {
        orderCreatedConsumer = new OrderCreatedConsumer(productService, stockReservationResultProducer, meterRegistry);
    }

    @Test
    void shouldPublishSuccessfulReservationWhenStockAvailable(){

        List<PurchaseRequestDTO> products = List.of(new PurchaseRequestDTO(1, 2.0));
        List<ProductPurchaseResponseDTO> purchased = List.of(
                new ProductPurchaseResponseDTO(1, 1, "name", "description", "M", "Red", new BigDecimal("100"), 2.0, "seller-1")
        );

        OrderCreatedEventDTO event = OrderCreatedEventDTO.builder()
                .orderReference("reference")
                .totalAmount(new BigDecimal("200"))
                .paymentMethode("BITCOIN")
                .products(products)
                .build();

        Mockito.when(productService.purchaseProductService(products)).thenReturn(purchased);

        orderCreatedConsumer.consumeOrderCreated(event);

        var captor = ArgumentCaptor.forClass(StockReservationResultEventDTO.class);
        Mockito.verify(stockReservationResultProducer, Mockito.times(1)).sendStockReservationResult(captor.capture());

        Assertions.assertTrue(captor.getValue().success());
        Assertions.assertEquals("reference", captor.getValue().orderReference());
        Assertions.assertEquals(purchased, captor.getValue().products());
        Assertions.assertEquals(1.0, meterRegistry.counter("stock_reservation_total", "result", "success").count());
    }

    @Test
    void shouldPublishFailedReservationWhenStockInsufficient(){

        List<PurchaseRequestDTO> products = List.of(new PurchaseRequestDTO(1, 200.0));

        OrderCreatedEventDTO event = OrderCreatedEventDTO.builder()
                .orderReference("reference")
                .products(products)
                .build();

        Mockito.when(productService.purchaseProductService(products))
                .thenThrow(new ProductPurchaseException("Insufficient stock quantity for product variant with id1"));

        orderCreatedConsumer.consumeOrderCreated(event);

        var captor = ArgumentCaptor.forClass(StockReservationResultEventDTO.class);
        Mockito.verify(stockReservationResultProducer, Mockito.times(1)).sendStockReservationResult(captor.capture());

        Assertions.assertFalse(captor.getValue().success());
        Assertions.assertEquals("reference", captor.getValue().orderReference());
        Assertions.assertEquals("Insufficient stock quantity for product variant with id1", captor.getValue().reason());
        Assertions.assertEquals(1.0, meterRegistry.counter("stock_reservation_total", "result", "failure").count());
    }
}
