package com.rajitha.ecommerce.controller;

import com.rajitha.ecommerce.dto.AddCartItemRequestDTO;
import com.rajitha.ecommerce.dto.CartCheckoutRequestDTO;
import com.rajitha.ecommerce.dto.CartResponseDTO;
import com.rajitha.ecommerce.dto.SetCartItemQuantityRequestDTO;
import com.rajitha.ecommerce.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping("/{userId}")
    public ResponseEntity<CartResponseDTO> getCart(@PathVariable String userId) {
        return ResponseEntity.ok(cartService.getCart(userId));
    }

    @PostMapping("/{userId}/items")
    public ResponseEntity<Void> addItem(@PathVariable String userId, @RequestBody @Valid AddCartItemRequestDTO request) {
        cartService.addItem(userId, request);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{userId}/items/{variantId}")
    public ResponseEntity<Void> setItemQuantity(@PathVariable String userId, @PathVariable Integer variantId,
                                                 @RequestBody @Valid SetCartItemQuantityRequestDTO request) {
        cartService.setItemQuantity(userId, variantId, request.quantity());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{userId}/items/{variantId}")
    public ResponseEntity<Void> removeItem(@PathVariable String userId, @PathVariable Integer variantId) {
        cartService.removeItem(userId, variantId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> clearCart(@PathVariable String userId) {
        cartService.clearCart(userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{userId}/checkout")
    public ResponseEntity<Integer> checkout(@PathVariable String userId,
                                             @RequestBody @Valid CartCheckoutRequestDTO request,
                                             @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return ResponseEntity.ok(cartService.checkout(userId, request, idempotencyKey));
    }
}
