package com.rajitha.ecommerce.client.feign;

import com.rajitha.ecommerce.dto.SellerLookupResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// Service-to-service only — hits seller-service directly, not through api-gateway
// (same pattern as order-service's CustomerClient). seller-service's
// GET /api/v1/sellers/lookup/{keycloak-user-id} is deliberately unauthenticated for
// exactly this kind of internal call.
@FeignClient(
        name = "seller-service",
        url = "${application.config.seller-url:http://localhost:8095/api/v1/sellers}"
)
public interface SellerClient {
    @GetMapping("/lookup/{keycloak-user-id}")
    SellerLookupResponseDTO findByKeycloakUserId(@PathVariable("keycloak-user-id") String keycloakUserId);
}
