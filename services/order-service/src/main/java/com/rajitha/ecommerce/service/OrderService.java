package com.rajitha.ecommerce.serviceImpl;

import com.rajitha.ecommerce.dto.OrderRequestDTO;
import jakarta.validation.Valid;

public interface OrderServiceImpl {
    Integer createOrder(@Valid OrderRequestDTO orderRequestDTO);
}
