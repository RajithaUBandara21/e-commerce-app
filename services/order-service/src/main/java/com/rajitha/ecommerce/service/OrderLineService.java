package com.rajitha.ecommerce.service;

import com.rajitha.ecommerce.dto.OrderLineRequestDTO;

public interface OrderLineService {
    Integer saveOrderLine(OrderLineRequestDTO orderLineRequest);
}
