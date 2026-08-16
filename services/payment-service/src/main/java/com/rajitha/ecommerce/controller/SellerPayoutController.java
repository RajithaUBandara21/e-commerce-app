package com.rajitha.ecommerce.controller;

import com.rajitha.ecommerce.dto.SellerPayoutResponseDTO;
import com.rajitha.ecommerce.service.SellerPayoutService;
import com.rajitha.ecommerce.util.RolesHeader;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

// A seller only ever sees their own payouts — the caller's X-User-Id (gateway-
// forwarded from their JWT) drives the query directly, there's no ?sellerId= param
// a seller could use to view someone else's ledger. Admins see every payout.
@RestController
@RequestMapping("/api/v1/payouts")
@RequiredArgsConstructor
public class SellerPayoutController {

    private final SellerPayoutService sellerPayoutService;

    @GetMapping
    public ResponseEntity<List<SellerPayoutResponseDTO>> findPayouts(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-User-Roles", defaultValue = "") String roles) {
        if (RolesHeader.isAdmin(roles)) {
            return ResponseEntity.ok(sellerPayoutService.findAll());
        }
        return ResponseEntity.ok(sellerPayoutService.findBySellerId(userId));
    }

    @PostMapping("/settle")
    public ResponseEntity<Void> settlePendingPayouts(
            @RequestHeader(value = "X-User-Roles", defaultValue = "") String roles) {
        if (!RolesHeader.isAdmin(roles)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admins can trigger payout settlement");
        }
        sellerPayoutService.settlePendingPayouts();
        return ResponseEntity.accepted().build();
    }
}
