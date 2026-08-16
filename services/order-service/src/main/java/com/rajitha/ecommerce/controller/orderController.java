package com.rajitha.ecommerce.controller;

import com.rajitha.ecommerce.dto.OrderRequestDTO;
import com.rajitha.ecommerce.dto.OrderResponseDTO;
import com.rajitha.ecommerce.service.serviceImpl.OrderServiceImpl;
import com.rajitha.ecommerce.util.RolesHeader;
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
    public ResponseEntity<Integer> createOrder(
            @RequestBody @Valid OrderRequestDTO orderRequestDTO,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
    return ResponseEntity.ok(orderService.createOrder(orderRequestDTO, idempotencyKey));
    }

    // Unfiltered — every order, regardless of customer. Gateway-gated to
    // ROLE_ADMIN; not a page a customer should ever be able to reach.
    @GetMapping
    public ResponseEntity<List<OrderResponseDTO>> findAllResponses(){
        return ResponseEntity.ok(orderService.findAllOderResponses());
    }

    // Customer-scoped — the fix for the previously-flagged unscoped-orders gap.
    // customerId always comes from the caller's own X-User-Id.
    @GetMapping("/mine")
    public ResponseEntity<List<OrderResponseDTO>> findMyOrders(@RequestHeader("X-User-Id") String customerId) {
        return ResponseEntity.ok(orderService.findMyOrders(customerId));
    }

    @GetMapping("/{order-id}")
    public ResponseEntity<OrderResponseDTO> findOrderById(@PathVariable("order-id") Integer orderId){
        return ResponseEntity.ok(orderService.getOderById(orderId));
    }

    @PostMapping("/{order-id}/refund")
    public ResponseEntity<OrderResponseDTO> refundOrder(
            @PathVariable("order-id") Integer orderId,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-User-Roles", defaultValue = "") String roles) {
        return ResponseEntity.ok(orderService.refundOrder(orderId, userId, RolesHeader.isAdmin(roles)));
    }
}
