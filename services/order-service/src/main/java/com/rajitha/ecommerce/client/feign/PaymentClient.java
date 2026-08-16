package com.rajitha.ecommerce.client.feign;

import com.rajitha.ecommerce.dto.PaymentRefundRequestDTO;
import com.rajitha.ecommerce.dto.PaymentRefundResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

// Deliberate, narrow exception to "no more direct HTTP calls to payment-service"
// (see CLAUDE.md): a customer/admin-initiated refund needs an immediate success/fail
// response, unlike checkout which correctly stays async (Kafka saga). This is the
// only synchronous order-service -> payment-service call, and it does exactly one
// thing.
@FeignClient(
        name = "payment-service",
        url = "${application.config.payment-url:http://localhost:8060/api/v1/payments}"
)
public interface PaymentClient {
    @PostMapping("/refund")
    PaymentRefundResponseDTO refund(@RequestBody PaymentRefundRequestDTO request);
}
