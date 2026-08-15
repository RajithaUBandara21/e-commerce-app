package com.rajitha.ecommerce.messaging.consumer;

import com.rajitha.ecommerce.dto.CustomerResponseDTO;
import com.rajitha.ecommerce.dto.OrderConfirmationDTO;
import com.rajitha.ecommerce.dto.PurchaseResponseDTO;
import com.rajitha.ecommerce.dto.StockReservationResultEventDTO;
import com.rajitha.ecommerce.entity.Order;
import com.rajitha.ecommerce.enums.OrderStatus;
import com.rajitha.ecommerce.enums.PaymentMethode;
import com.rajitha.ecommerce.messaging.OrderProducer;
import com.rajitha.ecommerce.repository.OrderRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class StockReservationConsumerTest {

    @InjectMocks
    StockReservationConsumer stockReservationConsumer;
    @Mock OrderRepository orderRepository;
    @Mock OrderProducer orderProducer;

    @Test
    void shouldSendOrderConformationWhenStockReservationSucceeds(){

        CustomerResponseDTO customer = CustomerResponseDTO.builder()
                .id("Id-1").firstName("first").lastName("last").email("a@b.com").build();

        StockReservationResultEventDTO event = StockReservationResultEventDTO.builder()
                .orderReference("reference")
                .success(true)
                .totalAmount(new BigDecimal("100"))
                .paymentMethode(PaymentMethode.BITCOIN)
                .customer(customer)
                .products(List.of(PurchaseResponseDTO.builder().variantId(1).productId(1).name("name").price(new BigDecimal("100")).quantity(1.0).build()))
                .build();

        stockReservationConsumer.consumeStockReservationResult(event);

        Mockito.verify(orderProducer, Mockito.times(1)).sendOrderConformation(Mockito.any(OrderConfirmationDTO.class));
        Mockito.verify(orderRepository, Mockito.never()).save(Mockito.any(Order.class));
    }

    @Test
    void shouldCancelOrderWhenStockReservationFails(){

        Order order = Order.builder().Id(1).reference("reference").status(OrderStatus.PENDING_PAYMENT).build();

        StockReservationResultEventDTO event = StockReservationResultEventDTO.builder()
                .orderReference("reference")
                .success(false)
                .reason("Insufficient stock")
                .build();

        Mockito.when(orderRepository.findByReference("reference")).thenReturn(Optional.of(order));

        stockReservationConsumer.consumeStockReservationResult(event);

        Assertions.assertEquals(OrderStatus.CANCELLED, order.getStatus());
        Mockito.verify(orderRepository, Mockito.times(1)).save(order);
        Mockito.verify(orderProducer, Mockito.never()).sendOrderConformation(Mockito.any(OrderConfirmationDTO.class));
    }
}
