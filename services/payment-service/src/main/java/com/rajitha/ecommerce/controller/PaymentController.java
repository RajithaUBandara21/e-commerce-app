package com.rajitha.ecommerce.controller;

import com.rajitha.ecommerce.dto.PaymentRequestDTO;
import com.rajitha.ecommerce.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payment")
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<Integer> createPayment(@RequestBody @Valid PaymentRequestDTO paymentRequestDTO) {
    return ResponseEntity.ok(paymentService.createPayment(paymentRequestDTO));
    }

}
