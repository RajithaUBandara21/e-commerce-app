package com.rajitha.ecommerce.service;

import com.rajitha.ecommerce.dto.OrderRequestDTO;
import jakarta.validation.Valid;

public interface OrderService {
    Integer createOrder(@Valid OrderRequestDTO orderRequestDTO);
}
