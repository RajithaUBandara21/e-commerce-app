package com.rajitha.ecommerce.controller;

import com.rajitha.ecommerce.dto.OrderRequestDTO;
import com.rajitha.ecommerce.service.serviceImpl.OrderServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor

public class orderController {
    private final OrderServiceImpl orderService;

    @PostMapping
    public ResponseEntity<Integer> createOrder(@RequestBody @Valid OrderRequestDTO orderRequestDTO) {
    return ResponseEntity.ok(orderService.createOrder(orderRequestDTO));
    }

}
