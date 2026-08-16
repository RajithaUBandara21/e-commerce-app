package com.rajitha.ecommerce.controller;

import com.rajitha.ecommerce.dto.SellerRegistrationRequestDTO;
import com.rajitha.ecommerce.dto.SellerResponseDTO;
import com.rajitha.ecommerce.dto.SellerStatusUpdateRequestDTO;
import com.rajitha.ecommerce.dto.StripeOnboardingLinkResponseDTO;
import com.rajitha.ecommerce.service.SellerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// X-User-Id/X-User-Roles are set by api-gateway from the caller's validated JWT and
// stripped from any inbound client request first — this service trusts the gateway
// as the auth boundary rather than re-validating the JWT itself (see PLAN.md's
// gateway-as-trust-boundary decision). GET /sellers and PATCH .../status are
// additionally gateway-gated to ROLE_ADMIN, so no role check is duplicated here.
@RestController
@RequestMapping("/api/v1/sellers")
@RequiredArgsConstructor
public class SellerController {

    private final SellerService sellerService;

    @PostMapping("/register")
    public ResponseEntity<SellerResponseDTO> register(
            @RequestBody @Valid SellerRegistrationRequestDTO requestDTO,
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(sellerService.register(requestDTO, userId));
    }

    @GetMapping("/me")
    public ResponseEntity<SellerResponseDTO> me(@RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(sellerService.findByKeycloakUserId(userId));
    }

    @GetMapping
    public ResponseEntity<List<SellerResponseDTO>> findAll() {
        return ResponseEntity.ok(sellerService.findAll());
    }

    @PatchMapping("/{seller-id}/status")
    public ResponseEntity<SellerResponseDTO> updateStatus(
            @PathVariable("seller-id") Integer sellerId,
            @RequestBody @Valid SellerStatusUpdateRequestDTO requestDTO) {
        return ResponseEntity.ok(sellerService.updateStatus(sellerId, requestDTO.status()));
    }

    @PostMapping("/me/stripe/onboarding-link")
    public ResponseEntity<StripeOnboardingLinkResponseDTO> createOnboardingLink(
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(sellerService.createOnboardingLink(userId));
    }

    // Internal, service-to-service only (called via Feign/Eureka discovery, same
    // pattern as order-service's CustomerClient — not routed through api-gateway,
    // so no X-User-Id/roles headers apply here).
    @GetMapping("/lookup/{keycloak-user-id}")
    public ResponseEntity<SellerResponseDTO> lookupByKeycloakUserId(
            @PathVariable("keycloak-user-id") String keycloakUserId) {
        return ResponseEntity.ok(sellerService.findByKeycloakUserId(keycloakUserId));
    }
}
