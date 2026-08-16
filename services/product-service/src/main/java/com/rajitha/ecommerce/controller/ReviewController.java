package com.rajitha.ecommerce.controller;

import com.rajitha.ecommerce.dto.ReviewRequestDTO;
import com.rajitha.ecommerce.dto.ReviewResponseDTO;
import com.rajitha.ecommerce.service.ReviewService;
import com.rajitha.ecommerce.util.RolesHeader;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// GETs stay public (matches the rest of /api/v1/products/**); POST/DELETE require
// an authenticated caller but no special role — any customer can review, gated on
// verified purchase inside the service layer, not on a role.
@RestController
@RequestMapping("/api/v1/products/{product-id}/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping
    public ResponseEntity<List<ReviewResponseDTO>> findByProductId(@PathVariable("product-id") Integer productId) {
        return ResponseEntity.ok(reviewService.findByProductId(productId));
    }

    @PostMapping
    public ResponseEntity<ReviewResponseDTO> createReview(
            @PathVariable("product-id") Integer productId,
            @RequestBody @Valid ReviewRequestDTO requestDTO,
            @RequestHeader("X-User-Id") String customerId) {
        return ResponseEntity.ok(reviewService.createReview(productId, customerId, requestDTO));
    }

    @DeleteMapping("/{review-id}")
    public ResponseEntity<Void> deleteReview(
            @PathVariable("product-id") Integer productId,
            @PathVariable("review-id") Integer reviewId,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-User-Roles", defaultValue = "") String roles) {
        reviewService.deleteReview(productId, reviewId, userId, RolesHeader.isAdmin(roles));
        return ResponseEntity.noContent().build();
    }
}
