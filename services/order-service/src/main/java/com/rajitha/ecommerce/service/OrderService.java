package com.rajitha.ecommerce.service;

import com.rajitha.ecommerce.dto.OrderRequestDTO;
import com.rajitha.ecommerce.dto.OrderResponseDTO;

import jakarta.validation.Valid;

import java.util.List;

public interface OrderService {
    Integer createOrder(@Valid OrderRequestDTO orderRequestDTO, String idempotencyKey);

    List<OrderResponseDTO> findAllOderResponses();

    OrderResponseDTO getOderById(Integer orderId);

    List<OrderResponseDTO> findMyOrders(String customerId);

    OrderResponseDTO refundOrder(Integer orderId, String callerId, boolean isAdmin);
}
