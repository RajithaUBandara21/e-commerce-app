package com.rajitha.ecommerce.service.serviceImpl;

import com.rajitha.ecommerce.client.feign.CustomerClient;
import com.rajitha.ecommerce.client.feign.PaymentClient;
import com.rajitha.ecommerce.client.rest.ProductClient;
import com.rajitha.ecommerce.dto.*;
import com.rajitha.ecommerce.entity.Order;
import com.rajitha.ecommerce.enums.OrderStatus;
import com.rajitha.ecommerce.enums.PaymentMethode;
import com.rajitha.ecommerce.exception.BusinessException;
import com.rajitha.ecommerce.mapper.OrderMapper;
import com.rajitha.ecommerce.messaging.OrderProducer;
import com.rajitha.ecommerce.repository.OrderRepository;
import com.rajitha.ecommerce.service.OrderLineService;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {
    @InjectMocks
    OrderServiceImpl orderServiceImpl;
    @Mock OrderMapper orderMapper;
    @Mock CustomerClient customerClient;
    @Mock ProductClient productClient ;
    @Mock OrderRepository orderRepository;
    @Mock OrderLineService orderLineService;
    @Mock OrderProducer orderProducer;
    @Mock PaymentClient paymentClient;


    @Test

    public void shouldCreateOrder(){



        PurchaseRequestDTO purchaseRequestDTO = PurchaseRequestDTO.builder()
                .variantId(1)
                .quantity(2)
                .build();

        List<PurchaseRequestDTO> orders = List.of(purchaseRequestDTO);

        PurchaseResponseDTO purchaseResponseDTO = PurchaseResponseDTO.builder()
                .variantId(1)
                .productId(1)
                .name("name")
                .description("description")
                .size("M")
                .color("Red")
                .price(new BigDecimal("123"))
                .quantity(12.0)
                .build();

        List<PurchaseResponseDTO> orderResponses = List.of(purchaseResponseDTO);


        OrderRequestDTO orderRequestDTO = OrderRequestDTO.builder()
                .id(1)
                .reference("reference")
                .totalAmount(new BigDecimal("1235"))
                .customerId("Id-1")
                .products(orders)
                .paymentMethode(PaymentMethode.BITCOIN)
                .build();

        Order order = Order.builder()
                .Id(1)
                .reference("reference")
                .totalAmount(new BigDecimal("1235"))
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

        Mockito.when(orderRepository.save(Mockito.any(Order.class)))
                .thenReturn(order);


        Mockito.when(customerClient.findCustomerById("Id-1"))
                .thenReturn(Optional.of(customerResponseDTO));


        Mockito.when(productClient.purchaseProducts(orders))
                .thenReturn(orderResponses);

        Mockito.when(orderLineService.saveOrderLine(Mockito.any(OrderLineRequestDTO.class)))
                .thenReturn(order.getId());

        Mockito.when(paymentClient.requestOrderPayment(Mockito.any(PaymentRequestDTO.class)))
                .thenReturn(null);

        Mockito.doNothing()
                .when(orderProducer)
                .sendOrderConformation(Mockito.any(OrderConfirmationDTO.class));


        var orderResponse = orderServiceImpl.createOrder(orderRequestDTO, null);


        Assertions.assertNotNull(orderResponse);
        Assertions.assertEquals(order.getId(), orderResponse);
        Assertions.assertEquals(OrderStatus.CONFIRMED, order.getStatus());



        Mockito.verify(orderRepository, Mockito.times(2))
                .save(Mockito.any(Order.class));

        Mockito.verify(orderMapper, Mockito.times(1))
                .toOder(Mockito.any(OrderRequestDTO.class));


        Mockito.verify(productClient, Mockito.times(1))
                .purchaseProducts(orders);


        Mockito.verify(orderLineService, Mockito.atLeastOnce())
                .saveOrderLine(Mockito.any(OrderLineRequestDTO.class));


        Mockito.verify(paymentClient, Mockito.times(1))
                .requestOrderPayment(Mockito.any(PaymentRequestDTO.class));


        Mockito.verify(orderProducer, Mockito.times(1))
                .sendOrderConformation(Mockito.any(OrderConfirmationDTO.class));
    }

    @Test
    public void shouldMarkOrderPaymentFailedAndNotSendConformationWhenPaymentClientFails(){

        PurchaseRequestDTO purchaseRequestDTO = PurchaseRequestDTO.builder()
                .variantId(1)
                .quantity(2)
                .build();

        List<PurchaseRequestDTO> orders = List.of(purchaseRequestDTO);

        PurchaseResponseDTO purchaseResponseDTO = PurchaseResponseDTO.builder()
                .variantId(1)
                .productId(1)
                .name("name")
                .description("description")
                .size("M")
                .color("Red")
                .price(new BigDecimal("123"))
                .quantity(12.0)
                .build();

        List<PurchaseResponseDTO> orderResponses = List.of(purchaseResponseDTO);

        OrderRequestDTO orderRequestDTO = OrderRequestDTO.builder()
                .id(1)
                .reference("reference")
                .totalAmount(new BigDecimal("1235"))
                .customerId("Id-1")
                .products(orders)
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

        Mockito.when(orderRepository.save(Mockito.any(Order.class)))
                .thenReturn(order);

        Mockito.when(customerClient.findCustomerById("Id-1"))
                .thenReturn(Optional.of(customerResponseDTO));

        Mockito.when(productClient.purchaseProducts(orders))
                .thenReturn(orderResponses);

        Mockito.when(orderLineService.saveOrderLine(Mockito.any(OrderLineRequestDTO.class)))
                .thenReturn(order.getId());

        Mockito.when(paymentClient.requestOrderPayment(Mockito.any(PaymentRequestDTO.class)))
                .thenThrow(new RuntimeException("payment-service unavailable"));

        var exception = Assertions.assertThrows(
                BusinessException.class,
                () -> orderServiceImpl.createOrder(orderRequestDTO, null)
        );

        Assertions.assertEquals("Order payment failed for order reference: reference", exception.getMsg());
        Assertions.assertEquals(OrderStatus.PAYMENT_FAILED, order.getStatus());

        Mockito.verify(orderRepository, Mockito.times(2))
                .save(Mockito.any(Order.class));

        Mockito.verify(orderProducer, Mockito.never())
                .sendOrderConformation(Mockito.any(OrderConfirmationDTO.class));
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
        Mockito.verify(productClient, Mockito.never()).purchaseProducts(Mockito.anyList());
        Mockito.verify(paymentClient, Mockito.never()).requestOrderPayment(Mockito.any(PaymentRequestDTO.class));
        Mockito.verify(orderProducer, Mockito.never()).sendOrderConformation(Mockito.any(OrderConfirmationDTO.class));
    }
}