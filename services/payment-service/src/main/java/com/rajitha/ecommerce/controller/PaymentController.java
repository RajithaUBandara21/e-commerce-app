package com.rajitha.ecommerce.controller;

import com.rajitha.ecommerce.dto.PaymentRequestDTO;
import com.rajitha.ecommerce.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<Integer> createPayment(@RequestBody @Valid PaymentRequestDTO paymentRequestDTO) {
        System.out.println(paymentRequestDTO+"payment Service ##############");
    return ResponseEntity.ok(paymentService.createPayment(paymentRequestDTO));
    }

    @GetMapping
    public String paymentStatus(){
        return " hi im ok";
    }

}
