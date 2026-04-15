package com.rajitha.ecommerce.service;

import com.rajitha.ecommerce.dto.OrderRequestDTO;
import com.rajitha.ecommerce.dto.OrderResponseDTO;

import jakarta.validation.Valid;

import java.util.List;

public interface OrderService {
    Integer createOrder(@Valid OrderRequestDTO orderRequestDTO);

    List<OrderResponseDTO> findAllOderResponses();

    OrderResponseDTO getOderById(Integer orderId);
}
