package com.rajitha.ecommerce.service;

import com.rajitha.ecommerce.dto.OrderLineRequestDTO;
import com.rajitha.ecommerce.dto.OrderLineResponseDTO;

import java.util.List;

public interface OrderLineService {

    Integer saveOrderLine(OrderLineRequestDTO orderLineRequest);

    List<OrderLineResponseDTO> findOrderLineByOrderId(Integer orderId);
}
