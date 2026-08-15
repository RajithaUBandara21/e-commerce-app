package com.rajitha.ecommerce.service.serviceImpl;

import com.rajitha.ecommerce.client.feign.CustomerClient;
import com.rajitha.ecommerce.dto.*;
import com.rajitha.ecommerce.entity.Order;
import com.rajitha.ecommerce.enums.OrderStatus;
import com.rajitha.ecommerce.enums.PaymentMethode;
import com.rajitha.ecommerce.exception.BusinessException;
import com.rajitha.ecommerce.mapper.OrderMapper;
import com.rajitha.ecommerce.messaging.OrderCreatedProducer;
import com.rajitha.ecommerce.repository.OrderRepository;
import com.rajitha.ecommerce.service.OrderLineService;
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
class OrderServiceImplTest {
    @InjectMocks
    OrderServiceImpl orderServiceImpl;
    @Mock OrderMapper orderMapper;
    @Mock CustomerClient customerClient;
    @Mock OrderRepository orderRepository;
    @Mock OrderLineService orderLineService;
    @Mock OrderCreatedProducer orderCreatedProducer;

    @Test
    public void shouldCreateOrderAsPendingPaymentAndPublishOrderCreatedEvent(){

        PurchaseRequestDTO purchaseRequestDTO = PurchaseRequestDTO.builder()
                .variantId(1)
                .quantity(2)
                .build();

        List<PurchaseRequestDTO> products = List.of(purchaseRequestDTO);

        OrderRequestDTO orderRequestDTO = OrderRequestDTO.builder()
                .id(1)
                .reference("reference")
                .totalAmount(new BigDecimal("1235"))
                .customerId("Id-1")
                .products(products)
                .paymentMethode(PaymentMethode.BITCOIN)
                .build();

        Order order = Order.builder()
                .Id(1)
                .reference("reference")
                .totalAmount(new BigDecimal("1235"))
                .status(OrderStatus.PENDING_PAYMENT)
                .build();

        CustomerResponseDTO customerResponseDTO = CustomerResponseDTO.builder()
                .id("Id-1")
                .firstName("firstName")
                .lastName("lastName")
                .email("rajithaubandara@gmail.com")
                .address(AddressDTO.builder()
                        .street("main")
                        .houseNumber("1235")
                        .zipCode("123")
                        .build())
                .build();

        Mockito.when(orderMapper.toOder(orderRequestDTO)).thenReturn(order);
        Mockito.when(orderRepository.save(Mockito.any(Order.class))).thenReturn(order);
        Mockito.when(customerClient.findCustomerById("Id-1")).thenReturn(Optional.of(customerResponseDTO));
        Mockito.when(orderLineService.saveOrderLine(Mockito.any(OrderLineRequestDTO.class))).thenReturn(order.getId());

        var orderResponse = orderServiceImpl.createOrder(orderRequestDTO, null);

        Assertions.assertNotNull(orderResponse);
        Assertions.assertEquals(order.getId(), orderResponse);
        Assertions.assertEquals(OrderStatus.PENDING_PAYMENT, order.getStatus());

        Mockito.verify(orderRepository, Mockito.times(1)).save(Mockito.any(Order.class));
        Mockito.verify(orderMapper, Mockito.times(1)).toOder(Mockito.any(OrderRequestDTO.class));
        Mockito.verify(orderLineService, Mockito.times(1)).saveOrderLine(Mockito.any(OrderLineRequestDTO.class));

        Mockito.verify(orderCreatedProducer, Mockito.times(1)).sendOrderCreated(
                OrderCreatedEventDTO.builder()
                        .orderReference("reference")
                        .totalAmount(new BigDecimal("1235"))
                        .paymentMethode(PaymentMethode.BITCOIN)
                        .customer(customerResponseDTO)
                        .products(products)
                        .build()
        );
    }

    @Test
    public void shouldNotCreateOrderWhenCustomerDoesNotExist(){

        OrderRequestDTO orderRequestDTO = OrderRequestDTO.builder()
                .id(1)
                .reference("reference")
                .totalAmount(new BigDecimal("1235"))
                .customerId("missing-customer")
                .products(List.of(PurchaseRequestDTO.builder().variantId(1).quantity(2).build()))
                .paymentMethode(PaymentMethode.BITCOIN)
                .build();

        Mockito.when(customerClient.findCustomerById("missing-customer")).thenReturn(Optional.empty());

        Assertions.assertThrows(
                BusinessException.class,
                () -> orderServiceImpl.createOrder(orderRequestDTO, null)
        );

        Mockito.verify(orderRepository, Mockito.never()).save(Mockito.any(Order.class));
        Mockito.verify(orderCreatedProducer, Mockito.never()).sendOrderCreated(Mockito.any(OrderCreatedEventDTO.class));
    }

    @Test
    public void shouldReturnExistingOrderAndSkipSideEffectsWhenIdempotencyKeyAlreadyProcessed(){

        OrderRequestDTO orderRequestDTO = OrderRequestDTO.builder()
                .id(1)
                .reference("reference")
                .totalAmount(new BigDecimal("1235"))
                .customerId("Id-1")
                .products(List.of(PurchaseRequestDTO.builder().variantId(1).quantity(2).build()))
                .paymentMethode(PaymentMethode.BITCOIN)
                .build();

        Order existingOrder = Order.builder()
                .Id(42)
                .reference("reference")
                .idempotencyKey("retry-key-1")
                .status(OrderStatus.CONFIRMED)
                .build();

        Mockito.when(orderRepository.findByIdempotencyKey("retry-key-1"))
                .thenReturn(Optional.of(existingOrder));

        var orderResponse = orderServiceImpl.createOrder(orderRequestDTO, "retry-key-1");

        Assertions.assertEquals(42, orderResponse);

        Mockito.verify(orderRepository, Mockito.never()).save(Mockito.any(Order.class));
        Mockito.verify(customerClient, Mockito.never()).findCustomerById(Mockito.anyString());
        Mockito.verify(orderCreatedProducer, Mockito.never()).sendOrderCreated(Mockito.any(OrderCreatedEventDTO.class));
    }
}
