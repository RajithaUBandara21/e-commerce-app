package com.rajitha.ecommerce.mapper;
import com.rajitha.ecommerce.dto.OrderRequestDTO;
import com.rajitha.ecommerce.dto.OrderResponseDTO;
import com.rajitha.ecommerce.entity.Order;
import com.rajitha.ecommerce.enums.OrderStatus;
import org.springframework.stereotype.Component;

@Component
public class OrderMapper {

    public Order toOder(OrderRequestDTO orderRequestDTO) {
       return Order.builder()
        .Id(orderRequestDTO.id())
        .reference(orderRequestDTO.reference())
        .totalAmount(orderRequestDTO.totalAmount())
        .paymentMethode(orderRequestDTO.paymentMethode())
        .status(OrderStatus.PENDING_PAYMENT)
        .customerId(orderRequestDTO.customerId())
//        .orderLines(orderRequestDTO.)
//        .createdDate(orderRequestDTO.)
//        .lastModifiedDate(orderRequestDTO.)
               .build();
    }

    public OrderResponseDTO toOrderResponseDTO(Order order) {
        return OrderResponseDTO.builder()
                .id(order.getId())
                .reference(order.getReference())
                .amount(order.getTotalAmount())
                .paymentMethode(order.getPaymentMethode())
                .status(order.getStatus())
                .customerId(order.getCustomerId())
                .build();
    }
}
