package com.rajitha.ecommerce.mapper;

import com.rajitha.ecommerce.dto.OrderLineRequestDTO;
import com.rajitha.ecommerce.dto.OrderLineResponseDTO;
import com.rajitha.ecommerce.entity.Order;
import com.rajitha.ecommerce.entity.OrderLine;
import org.springframework.stereotype.Component;

@Component
public class OrderLineMapper {

    public OrderLine toOrderLine(OrderLineRequestDTO orderLineRequest) {
        return OrderLine.builder().
          Id(orderLineRequest.id()).
          order(Order.builder().Id(orderLineRequest.orderId()).build()).
          variantId(orderLineRequest.variantId()).
          quantity(orderLineRequest.quantity()).
                build();
    }

    public OrderLineResponseDTO toOrderLineResponseDTO(OrderLine orderLine) {
            return OrderLineResponseDTO.builder()
                    .id(orderLine.getId())
                    .variantId(orderLine.getVariantId())
                    .quantity(orderLine.getQuantity())
                    .build();
    }
}
