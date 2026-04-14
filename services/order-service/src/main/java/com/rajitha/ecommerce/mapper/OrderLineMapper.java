package com.rajitha.ecommerce.mapper;

import com.rajitha.ecommerce.dto.OrderLineRequestDTO;
import com.rajitha.ecommerce.entity.Order;
import com.rajitha.ecommerce.entity.OrderLine;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import org.springframework.stereotype.Component;

@Component
public class OrderLineMapper {

    public OrderLine toOrderLine(OrderLineRequestDTO orderLineRequest) {
        return OrderLine.builder().

          Id(orderLineRequest.id()).
          order(Order.builder().Id(orderLineRequest.orderId()).build()).
          productId(orderLineRequest.productId()).
          quantity(orderLineRequest.quantity()).
                build();
    }
}
