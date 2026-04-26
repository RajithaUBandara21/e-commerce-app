package com.rajitha.ecommerce.mapper;

import com.rajitha.ecommerce.dto.OrderRequestDTO;
import com.rajitha.ecommerce.dto.OrderResponseDTO;
import com.rajitha.ecommerce.dto.PurchaseRequestDTO;
import com.rajitha.ecommerce.entity.Order;
import com.rajitha.ecommerce.entity.OrderLine;
import com.rajitha.ecommerce.enums.PaymentMethode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class OrderMapperTest {
private OrderMapper orderMapper;

@BeforeEach
    void setUp() {
    orderMapper = new OrderMapper();
}

@Test
    public void shouldMapToOder() {
    PurchaseRequestDTO product = PurchaseRequestDTO.builder()
            .productId(1)
            .quantity(2)
            .build();

    OrderRequestDTO orderRequestDTO = OrderRequestDTO.builder()
            .id(1)
            .reference("reference")
            .totalAmount(new BigDecimal("156"))
            .paymentMethode(PaymentMethode.BITCOIN)
            .customerId("customerId")
            .products(List.of(product))
            .build();

    Order order = orderMapper.toOder(orderRequestDTO);

    assertNotNull(order);
    Assertions.assertEquals(order.getId(), 1);
    Assertions.assertEquals(order.getReference(), "reference");
    Assertions.assertEquals(order.getTotalAmount(), new BigDecimal("156"));
    Assertions.assertEquals(order.getPaymentMethode(), PaymentMethode.BITCOIN);
}


@Test
    public void shouldMapToOrderResponseDTO(){

    OrderLine orderLine = OrderLine.builder()
            .Id(1)
            .productId(2)
            .quantity(2)
            .build();

    Order order = Order.builder()
            .Id(1)
            .reference("reference")
            .totalAmount(new BigDecimal("156"))
            .paymentMethode(PaymentMethode.BITCOIN)
            .customerId("customerId")
            .orderLines(List.of(orderLine))
            .build();

    orderLine.setOrder(order);

    OrderResponseDTO orderResponseDTO = orderMapper.toOrderResponseDTO(order);

    Assertions.assertNotNull(orderResponseDTO);
    Assertions.assertEquals(order.getId(), orderResponseDTO.id());
    Assertions.assertEquals(order.getId(), orderResponseDTO.id());
}
}