package com.rajitha.ecommerce.client.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

// The one deliberate synchronous product-service -> order-service call: review
// eligibility needs to know which variants a customer has actually paid for, and
// order-service is the only service that knows order status. Not routed through
// api-gateway (service-to-service), matching the CustomerClient/PaymentClient
// pattern already established in order-service/payment-service.
@FeignClient(
        name = "order-service",
        url = "${application.config.order-url:http://localhost:8070/api/v1/order-lines}"
)
public interface OrderClient {
    @GetMapping("/purchased-variants/{customerId}")
    List<Integer> findPurchasedVariantIds(@PathVariable String customerId);
}
