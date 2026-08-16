package com.rajitha.ecommerce.service.serviceImpl;

import com.rajitha.ecommerce.client.feign.CustomerClient;
import com.rajitha.ecommerce.client.feign.PaymentClient;
import com.rajitha.ecommerce.dto.*;
import com.rajitha.ecommerce.entity.Order;
import com.rajitha.ecommerce.entity.OrderLine;
import com.rajitha.ecommerce.enums.OrderLineStatus;
import com.rajitha.ecommerce.enums.OrderStatus;
import com.rajitha.ecommerce.enums.PaymentMethode;
import com.rajitha.ecommerce.exception.BusinessException;
import com.rajitha.ecommerce.exception.OrderAccessDeniedException;
import com.rajitha.ecommerce.mapper.OrderMapper;
import com.rajitha.ecommerce.messaging.OrderCreatedProducer;
import com.rajitha.ecommerce.messaging.StockReleaseProducer;
import com.rajitha.ecommerce.repository.OrderRepository;
import com.rajitha.ecommerce.service.CouponService;
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
    @Mock PaymentClient paymentClient;
    @Mock OrderRepository orderRepository;
    @Mock OrderLineService orderLineService;
    @Mock OrderCreatedProducer orderCreatedProducer;
    @Mock StockReleaseProducer stockReleaseProducer;
    @Mock CouponService couponService;

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

    @Test
    public void shouldApplyCouponDiscountBeforeSavingAndPublishing() {

        OrderRequestDTO orderRequestDTO = OrderRequestDTO.builder()
                .id(1)
                .reference("reference")
                .totalAmount(new BigDecimal("100"))
                .customerId("Id-1")
                .products(List.of(PurchaseRequestDTO.builder().variantId(1).quantity(2).build()))
                .paymentMethode(PaymentMethode.BITCOIN)
                .couponCode("SAVE10")
                .build();

        Order order = Order.builder().Id(1).reference("reference").status(OrderStatus.PENDING_PAYMENT).build();

        CustomerResponseDTO customerResponseDTO = CustomerResponseDTO.builder().id("Id-1").build();

        Mockito.when(customerClient.findCustomerById("Id-1")).thenReturn(Optional.of(customerResponseDTO));
        Mockito.when(couponService.applyDiscount("SAVE10", new BigDecimal("100"))).thenReturn(new BigDecimal("90.00"));
        Mockito.when(orderMapper.toOder(orderRequestDTO)).thenReturn(order);
        Mockito.when(orderRepository.save(Mockito.any(Order.class))).thenReturn(order);
        Mockito.when(orderLineService.saveOrderLine(Mockito.any(OrderLineRequestDTO.class))).thenReturn(order.getId());

        orderServiceImpl.createOrder(orderRequestDTO, null);

        Assertions.assertEquals(new BigDecimal("90.00"), order.getTotalAmount());
        Mockito.verify(orderCreatedProducer).sendOrderCreated(
                Mockito.argThat(event -> event.totalAmount().equals(new BigDecimal("90.00"))));
    }

    @Test
    public void shouldReturnOnlyOrdersForRequestedCustomer() {
        var order = Order.builder().Id(1).reference("reference").customerId("customer-1").build();
        Mockito.when(orderRepository.findByCustomerId("customer-1")).thenReturn(List.of(order));
        Mockito.when(orderMapper.toOrderResponseDTO(order)).thenReturn(
                OrderResponseDTO.builder().id(1).reference("reference").customerId("customer-1").build());

        var responses = orderServiceImpl.findMyOrders("customer-1");

        Assertions.assertEquals(1, responses.size());
        Assertions.assertEquals("customer-1", responses.get(0).customerId());
    }

    @Test
    public void shouldRefundOwnedConfirmedOrderAndReleaseStock() {
        var order = Order.builder().Id(1).reference("reference").customerId("customer-1").status(OrderStatus.CONFIRMED).build();

        Mockito.when(orderRepository.findById(1)).thenReturn(Optional.of(order));
        Mockito.when(paymentClient.refund(Mockito.any(PaymentRefundRequestDTO.class)))
                .thenReturn(new PaymentRefundResponseDTO(true, null));
        Mockito.when(orderLineService.findOrderLineByOrderId(1)).thenReturn(List.of(
                OrderLineResponseDTO.builder().id(100).variantId(5).quantity(2.0).build()));
        Mockito.when(orderMapper.toOrderResponseDTO(order)).thenReturn(
                OrderResponseDTO.builder().id(1).reference("reference").status(OrderStatus.REFUNDED).build());

        var response = orderServiceImpl.refundOrder(1, "customer-1", false);

        Assertions.assertEquals(OrderStatus.REFUNDED, order.getStatus());
        Assertions.assertEquals(OrderStatus.REFUNDED, response.status());
        Mockito.verify(orderRepository).save(order);
        Mockito.verify(stockReleaseProducer).sendStockRelease(Mockito.any(StockReleaseEventDTO.class));
    }

    @Test
    public void shouldRejectRefundFromNonOwningCustomer() {
        var order = Order.builder().Id(1).reference("reference").customerId("customer-1").status(OrderStatus.CONFIRMED).build();
        Mockito.when(orderRepository.findById(1)).thenReturn(Optional.of(order));

        Assertions.assertThrows(OrderAccessDeniedException.class,
                () -> orderServiceImpl.refundOrder(1, "customer-2", false));

        Mockito.verify(paymentClient, Mockito.never()).refund(Mockito.any());
    }

    @Test
    public void shouldRejectRefundOfOrderNotYetConfirmed() {
        var order = Order.builder().Id(1).reference("reference").customerId("customer-1").status(OrderStatus.PENDING_PAYMENT).build();
        Mockito.when(orderRepository.findById(1)).thenReturn(Optional.of(order));

        Assertions.assertThrows(BusinessException.class,
                () -> orderServiceImpl.refundOrder(1, "customer-1", false));

        Mockito.verify(paymentClient, Mockito.never()).refund(Mockito.any());
    }

    @Test
    public void shouldSurfaceFailureWhenPaymentServiceRefundFails() {
        var order = Order.builder().Id(1).reference("reference").customerId("customer-1").status(OrderStatus.CONFIRMED).build();
        Mockito.when(orderRepository.findById(1)).thenReturn(Optional.of(order));
        Mockito.when(paymentClient.refund(Mockito.any(PaymentRefundRequestDTO.class)))
                .thenReturn(new PaymentRefundResponseDTO(false, "already refunded"));

        Assertions.assertThrows(BusinessException.class,
                () -> orderServiceImpl.refundOrder(1, "customer-1", false));

        Assertions.assertEquals(OrderStatus.CONFIRMED, order.getStatus());
        Mockito.verify(orderRepository, Mockito.never()).save(order);
        Mockito.verify(stockReleaseProducer, Mockito.never()).sendStockRelease(Mockito.any());
    }
}
