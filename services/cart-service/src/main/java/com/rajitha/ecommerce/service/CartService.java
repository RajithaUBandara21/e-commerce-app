package com.rajitha.ecommerce.service;

import com.rajitha.ecommerce.dto.AddCartItemRequestDTO;
import com.rajitha.ecommerce.dto.CartCheckoutRequestDTO;
import com.rajitha.ecommerce.dto.CartResponseDTO;

public interface CartService {

    void addItem(String userId, AddCartItemRequestDTO request);

    void setItemQuantity(String userId, Integer variantId, double quantity);

    void removeItem(String userId, Integer variantId);

    CartResponseDTO getCart(String userId);

    void clearCart(String userId);

    Integer checkout(String userId, CartCheckoutRequestDTO request, String idempotencyKey);
}
