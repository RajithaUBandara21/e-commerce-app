package com.rajitha.ecommerce.controller;

import com.rajitha.ecommerce.dto.OrderRequestDTO;
import com.rajitha.ecommerce.dto.OrderResponseDTO;
import com.rajitha.ecommerce.service.serviceImpl.OrderServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor

public class orderController {
    private final OrderServiceImpl orderService;

    @PostMapping
    public ResponseEntity<Integer> createOrder(@RequestBody @Valid OrderRequestDTO orderRequestDTO) {
    return ResponseEntity.ok(orderService.createOrder(orderRequestDTO));
    }

    @GetMapping
    public ResponseEntity<List<OrderResponseDTO>> findAllResponses(){
        return ResponseEntity.ok(orderService.findAllOderResponses());
    }

    @GetMapping("/{order-id}")
    public ResponseEntity<OrderResponseDTO> findOrderById(@PathVariable("order-id") Integer orderId){
        return ResponseEntity.ok(orderService.getOderById(orderId));
    }
}
