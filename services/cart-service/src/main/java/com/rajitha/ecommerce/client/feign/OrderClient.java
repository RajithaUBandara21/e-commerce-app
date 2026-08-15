package com.rajitha.ecommerce.client.feign;

import com.rajitha.ecommerce.dto.OrderRequestDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(
        name = "order-service",
        url = "${application.config.order-url}"
)
public interface OrderClient {
    @PostMapping
    Integer createOrder(@RequestBody OrderRequestDTO orderRequestDTO,
                         @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey);
}
