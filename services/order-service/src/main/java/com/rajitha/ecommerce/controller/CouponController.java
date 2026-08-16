package com.rajitha.ecommerce.controller;

import com.rajitha.ecommerce.dto.CouponPreviewResponseDTO;
import com.rajitha.ecommerce.dto.CouponRequestDTO;
import com.rajitha.ecommerce.dto.CouponResponseDTO;
import com.rajitha.ecommerce.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

// Mutations are admin-only (also gateway-gated — see api-gateway's
// SecurityConfiguration — this is defense in depth). /preview is reachable by any
// authenticated user: it's how the checkout page shows the discount before the
// order is actually created.
@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @PostMapping
    public ResponseEntity<Integer> createCoupon(@RequestBody @Valid CouponRequestDTO requestDTO) {
        return ResponseEntity.ok(couponService.createCoupon(requestDTO));
    }

    @PutMapping("/{coupon-id}")
    public ResponseEntity<Void> updateCoupon(
            @PathVariable("coupon-id") Integer couponId,
            @RequestBody @Valid CouponRequestDTO requestDTO) {
        couponService.updateCoupon(couponId, requestDTO);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{coupon-id}")
    public ResponseEntity<Void> deleteCoupon(@PathVariable("coupon-id") Integer couponId) {
        couponService.deleteCoupon(couponId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{coupon-id}")
    public ResponseEntity<CouponResponseDTO> findById(@PathVariable("coupon-id") Integer couponId) {
        return ResponseEntity.ok(couponService.findById(couponId));
    }

    @GetMapping
    public ResponseEntity<List<CouponResponseDTO>> findAll() {
        return ResponseEntity.ok(couponService.findAll());
    }

    @GetMapping("/preview")
    public ResponseEntity<CouponPreviewResponseDTO> preview(
            @RequestParam String code,
            @RequestParam BigDecimal amount) {
        var discounted = couponService.applyDiscount(code, amount);
        return ResponseEntity.ok(new CouponPreviewResponseDTO(amount, discounted));
    }
}
