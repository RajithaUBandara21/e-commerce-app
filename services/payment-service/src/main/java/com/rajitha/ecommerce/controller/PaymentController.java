package com.rajitha.ecommerce.controller;

import com.rajitha.ecommerce.dto.PaymentRefundRequestDTO;
import com.rajitha.ecommerce.dto.PaymentRefundResponseDTO;
import com.rajitha.ecommerce.dto.PaymentRequestDTO;
import com.rajitha.ecommerce.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Called by order-service (service-to-service, not through api-gateway) when a
// customer/admin refund is initiated — see order-service's OrderService.refundOrder
// and PLAN.md's note on why this one path is a deliberate synchronous exception to
// the checkout saga's async Kafka choreography.
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

    @PostMapping("/refund")
    public ResponseEntity<PaymentRefundResponseDTO> refund(@RequestBody @Valid PaymentRefundRequestDTO requestDTO) {
        return ResponseEntity.ok(paymentService.refundByOrderReference(requestDTO.orderReference()));
    }

    @GetMapping
    public String paymentStatus(){
        return " hi im ok";
    }

}
