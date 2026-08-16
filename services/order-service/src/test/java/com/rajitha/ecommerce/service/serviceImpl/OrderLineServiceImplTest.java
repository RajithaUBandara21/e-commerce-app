package com.rajitha.ecommerce.service.serviceImpl;

import com.rajitha.ecommerce.dto.OrderLineFulfillmentRequestDTO;
import com.rajitha.ecommerce.entity.Order;
import com.rajitha.ecommerce.entity.OrderLine;
import com.rajitha.ecommerce.enums.OrderLineStatus;
import com.rajitha.ecommerce.enums.OrderStatus;
import com.rajitha.ecommerce.exception.BusinessException;
import com.rajitha.ecommerce.exception.OrderAccessDeniedException;
import com.rajitha.ecommerce.mapper.OrderLineMapper;
import com.rajitha.ecommerce.repository.OrderLineRepository;
import com.rajitha.ecommerce.repository.OrderRepository;
import jakarta.persistence.EntityNotFoundException;
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
class OrderLineServiceImplTest {

    @InjectMocks
    private OrderLineServiceImpl orderLineService;

    @Mock
    private OrderLineRepository orderLineRepository;
    @Mock
    private OrderRepository orderRepository;

    private final OrderLineMapper orderLineMapper = new OrderLineMapper();

    @Test
    void shouldUpdateFulfillmentForOwningSeller() {
        var order = Order.builder().Id(1).status(OrderStatus.CONFIRMED).build();
        var line = OrderLine.builder().Id(10).order(order).sellerId("seller-1").status(OrderLineStatus.PENDING).build();
        var request = OrderLineFulfillmentRequestDTO.builder().status(OrderLineStatus.SHIPPED).trackingNumber("TRACK-1").build();

        Mockito.when(orderLineRepository.findById(10)).thenReturn(Optional.of(line));
        Mockito.when(orderRepository.findById(1)).thenReturn(Optional.of(order));
        Mockito.when(orderLineRepository.findOrderLinesByOrderId(1)).thenReturn(List.of(line));

        withRealMapper();
        var response = orderLineService.updateFulfillment(10, request, "seller-1", false);

        Assertions.assertEquals(OrderLineStatus.SHIPPED, response.status());
        Assertions.assertEquals("TRACK-1", response.trackingNumber());
        Assertions.assertEquals(OrderStatus.SHIPPED, order.getStatus());
        Mockito.verify(orderRepository).save(order);
    }

    @Test
    void shouldRejectFulfillmentUpdateFromNonOwningSeller() {
        var order = Order.builder().Id(1).status(OrderStatus.CONFIRMED).build();
        var line = OrderLine.builder().Id(10).order(order).sellerId("seller-1").status(OrderLineStatus.PENDING).build();
        var request = OrderLineFulfillmentRequestDTO.builder().status(OrderLineStatus.SHIPPED).build();

        Mockito.when(orderLineRepository.findById(10)).thenReturn(Optional.of(line));

        withRealMapper();
        Assertions.assertThrows(OrderAccessDeniedException.class,
                () -> orderLineService.updateFulfillment(10, request, "seller-2", false));
    }

    @Test
    void shouldAllowAdminToUpdateAnyLine() {
        var order = Order.builder().Id(1).status(OrderStatus.CONFIRMED).build();
        var line = OrderLine.builder().Id(10).order(order).sellerId("seller-1").status(OrderLineStatus.PENDING).build();
        var request = OrderLineFulfillmentRequestDTO.builder().status(OrderLineStatus.SHIPPED).build();

        Mockito.when(orderLineRepository.findById(10)).thenReturn(Optional.of(line));
        Mockito.when(orderRepository.findById(1)).thenReturn(Optional.of(order));
        Mockito.when(orderLineRepository.findOrderLinesByOrderId(1)).thenReturn(List.of(line));

        withRealMapper();
        var response = orderLineService.updateFulfillment(10, request, "some-admin", true);

        Assertions.assertEquals(OrderLineStatus.SHIPPED, response.status());
    }

    @Test
    void shouldRejectFulfillmentUpdateWhenOrderNotYetConfirmed() {
        var order = Order.builder().Id(1).status(OrderStatus.PENDING_PAYMENT).build();
        var line = OrderLine.builder().Id(10).order(order).sellerId("seller-1").status(OrderLineStatus.PENDING).build();
        var request = OrderLineFulfillmentRequestDTO.builder().status(OrderLineStatus.SHIPPED).build();

        Mockito.when(orderLineRepository.findById(10)).thenReturn(Optional.of(line));
        Mockito.when(orderRepository.findById(1)).thenReturn(Optional.of(order));

        withRealMapper();
        Assertions.assertThrows(BusinessException.class,
                () -> orderLineService.updateFulfillment(10, request, "seller-1", false));
    }

    @Test
    void shouldRejectBackwardStatusTransition() {
        var order = Order.builder().Id(1).status(OrderStatus.SHIPPED).build();
        var line = OrderLine.builder().Id(10).order(order).sellerId("seller-1").status(OrderLineStatus.DELIVERED).build();
        var request = OrderLineFulfillmentRequestDTO.builder().status(OrderLineStatus.PENDING).build();

        Mockito.when(orderLineRepository.findById(10)).thenReturn(Optional.of(line));
        Mockito.when(orderRepository.findById(1)).thenReturn(Optional.of(order));

        withRealMapper();
        Assertions.assertThrows(BusinessException.class,
                () -> orderLineService.updateFulfillment(10, request, "seller-1", false));
    }

    @Test
    void shouldMarkOrderDeliveredOnlyWhenAllLinesDelivered() {
        var order = Order.builder().Id(1).status(OrderStatus.SHIPPED).build();
        var line1 = OrderLine.builder().Id(10).order(order).sellerId("seller-1").status(OrderLineStatus.SHIPPED).build();
        var line2 = OrderLine.builder().Id(11).order(order).sellerId("seller-2").status(OrderLineStatus.DELIVERED).build();
        var request = OrderLineFulfillmentRequestDTO.builder().status(OrderLineStatus.DELIVERED).build();

        Mockito.when(orderLineRepository.findById(10)).thenReturn(Optional.of(line1));
        Mockito.when(orderRepository.findById(1)).thenReturn(Optional.of(order));
        Mockito.when(orderLineRepository.findOrderLinesByOrderId(1)).thenReturn(List.of(line1, line2));

        withRealMapper();
        orderLineService.updateFulfillment(10, request, "seller-1", false);

        Assertions.assertEquals(OrderStatus.DELIVERED, order.getStatus());
    }

    @Test
    void shouldThrowWhenOrderLineNotFound() {
        Mockito.when(orderLineRepository.findById(99)).thenReturn(Optional.empty());
        var request = OrderLineFulfillmentRequestDTO.builder().status(OrderLineStatus.SHIPPED).build();

        withRealMapper();
        Assertions.assertThrows(EntityNotFoundException.class,
                () -> orderLineService.updateFulfillment(99, request, "seller-1", false));
    }

    private void withRealMapper() {
        orderLineService = new OrderLineServiceImpl(orderLineRepository, orderRepository, orderLineMapper);
    }
}
