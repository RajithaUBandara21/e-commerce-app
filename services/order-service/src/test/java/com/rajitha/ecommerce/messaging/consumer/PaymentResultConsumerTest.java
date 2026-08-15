package com.rajitha.ecommerce.messaging.consumer;

import com.rajitha.ecommerce.dto.OrderLineResponseDTO;
import com.rajitha.ecommerce.dto.PaymentResultEventDTO;
import com.rajitha.ecommerce.dto.StockReleaseEventDTO;
import com.rajitha.ecommerce.entity.Order;
import com.rajitha.ecommerce.enums.OrderStatus;
import com.rajitha.ecommerce.messaging.StockReleaseProducer;
import com.rajitha.ecommerce.repository.OrderRepository;
import com.rajitha.ecommerce.service.OrderLineService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class PaymentResultConsumerTest {

    @InjectMocks
    PaymentResultConsumer paymentResultConsumer;
    @Mock OrderRepository orderRepository;
    @Mock OrderLineService orderLineService;
    @Mock StockReleaseProducer stockReleaseProducer;

    @Test
    void shouldConfirmOrderWhenPaymentSucceeds(){

        Order order = Order.builder().Id(1).reference("reference").status(OrderStatus.PENDING_PAYMENT).build();

        Mockito.when(orderRepository.findByReference("reference")).thenReturn(Optional.of(order));

        paymentResultConsumer.consumePaymentResult(PaymentResultEventDTO.builder().orderReference("reference").success(true).build());

        Assertions.assertEquals(OrderStatus.CONFIRMED, order.getStatus());
        Mockito.verify(orderRepository, Mockito.times(1)).save(order);
        Mockito.verify(stockReleaseProducer, Mockito.never()).sendStockRelease(Mockito.any(StockReleaseEventDTO.class));
    }

    @Test
    void shouldMarkOrderPaymentFailedAndReleaseStockWhenPaymentFails(){

        Order order = Order.builder().Id(1).reference("reference").status(OrderStatus.PENDING_PAYMENT).build();

        Mockito.when(orderRepository.findByReference("reference")).thenReturn(Optional.of(order));
        Mockito.when(orderLineService.findOrderLineByOrderId(1))
                .thenReturn(List.of(OrderLineResponseDTO.builder().id(1).variantId(7).quantity(2.0).build()));

        paymentResultConsumer.consumePaymentResult(
                PaymentResultEventDTO.builder().orderReference("reference").success(false).reason("card declined").build()
        );

        Assertions.assertEquals(OrderStatus.PAYMENT_FAILED, order.getStatus());
        Mockito.verify(orderRepository, Mockito.times(1)).save(order);

        var captor = org.mockito.ArgumentCaptor.forClass(StockReleaseEventDTO.class);
        Mockito.verify(stockReleaseProducer, Mockito.times(1)).sendStockRelease(captor.capture());
        Assertions.assertEquals("reference", captor.getValue().orderReference());
        Assertions.assertEquals(1, captor.getValue().products().size());
        Assertions.assertEquals(7, captor.getValue().products().get(0).variantId());
        Assertions.assertEquals(2.0, captor.getValue().products().get(0).quantity());
    }

    @Test
    void shouldIgnoreUnknownOrderReference(){
        Mockito.when(orderRepository.findByReference("missing")).thenReturn(Optional.empty());

        paymentResultConsumer.consumePaymentResult(PaymentResultEventDTO.builder().orderReference("missing").success(true).build());

        Mockito.verify(orderRepository, Mockito.never()).save(Mockito.any(Order.class));
    }
}
