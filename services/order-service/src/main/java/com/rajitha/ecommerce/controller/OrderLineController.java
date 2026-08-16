package com.rajitha.ecommerce.controller;

import com.rajitha.ecommerce.dto.OrderLineFulfillmentRequestDTO;
import com.rajitha.ecommerce.dto.OrderLineResponseDTO;
import com.rajitha.ecommerce.service.OrderLineService;
import com.rajitha.ecommerce.util.RolesHeader;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/order-lines")
@RequiredArgsConstructor
public class OrderLineController {
    private final OrderLineService orderLineService;

    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<OrderLineResponseDTO>>orderLineFindByOrderId(@PathVariable Integer orderId){
        return ResponseEntity.ok(orderLineService.findOrderLineByOrderId(orderId));
    }

    // A seller's own order lines across every order — the seller dashboard's
    // "orders to fulfill" view. sellerId always comes from the caller's own
    // X-User-Id; there's no way to query someone else's lines through this.
    @GetMapping("/mine")
    public ResponseEntity<List<OrderLineResponseDTO>> myOrderLines(@RequestHeader("X-User-Id") String sellerId) {
        return ResponseEntity.ok(orderLineService.findBySellerId(sellerId));
    }

    // Internal, service-to-service only (product-service's review feature calls
    // this to check verified-purchase eligibility) — not routed through the
    // gateway, so there's no X-User-Id to trust here; the caller supplies the
    // customerId directly, same as seller-service's /lookup/{keycloak-user-id}.
    @GetMapping("/purchased-variants/{customer-id}")
    public ResponseEntity<List<Integer>> purchasedVariants(@PathVariable("customer-id") String customerId) {
        return ResponseEntity.ok(orderLineService.findPurchasedVariantIds(customerId));
    }

    @PatchMapping("/{order-line-id}/fulfillment")
    public ResponseEntity<OrderLineResponseDTO> updateFulfillment(
            @PathVariable("order-line-id") Integer orderLineId,
            @RequestBody @Valid OrderLineFulfillmentRequestDTO requestDTO,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-User-Roles", defaultValue = "") String roles) {
        return ResponseEntity.ok(orderLineService.updateFulfillment(orderLineId, requestDTO, userId, RolesHeader.isAdmin(roles)));
    }
}
