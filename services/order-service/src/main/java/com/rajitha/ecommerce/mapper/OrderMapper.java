package com.rajitha.ecommerce.mapper;

import com.rajitha.ecommerce.dto.OrderRequestDTO;
import com.rajitha.ecommerce.entity.Order;
import com.rajitha.ecommerce.entity.OrderLine;
import com.rajitha.ecommerce.enums.PaymentMethode;
import jakarta.persistence.Column;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static jakarta.persistence.EnumType.STRING;

@Component
public class OrderMapper {

    public Order toOder(OrderRequestDTO orderRequestDTO) {
       return Order.builder()
        .Id(orderRequestDTO.id())
        .reference(orderRequestDTO.reference())
        .totalAmount(orderRequestDTO.totalAmount())
        .paymentMethode(orderRequestDTO.paymentMethode())
        .customerId(orderRequestDTO.customerId())
//        .orderLines(orderRequestDTO.)
//        .createdDate(orderRequestDTO.)
//        .lastModifiedDate(orderRequestDTO.)
               .build();
    }
}
