package com.rajitha.ecommerce.service.serviceImpl;

import com.rajitha.ecommerce.client.feign.OrderClient;
import com.rajitha.ecommerce.dto.*;
import com.rajitha.ecommerce.exception.CartEmptyException;
import com.rajitha.ecommerce.repository.CartRepository;
import com.rajitha.ecommerce.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final OrderClient orderClient;

    @Override
    public void addItem(String userId, AddCartItemRequestDTO request) {
        cartRepository.incrementItem(userId, request.variantId(), request.quantity());
    }

    @Override
    public void setItemQuantity(String userId, Integer variantId, double quantity) {
        if (quantity <= 0) {
            cartRepository.removeItem(userId, variantId);
            return;
        }
        cartRepository.setItem(userId, variantId, quantity);
    }

    @Override
    public void removeItem(String userId, Integer variantId) {
        cartRepository.removeItem(userId, variantId);
    }

    @Override
    public CartResponseDTO getCart(String userId) {
        var items = cartRepository.getItems(userId).entrySet().stream()
                .map(entry -> CartItemResponseDTO.builder()
                        .variantId(Integer.valueOf(entry.getKey().toString()))
                        .quantity(Double.parseDouble(entry.getValue().toString()))
                        .build())
                .toList();
        return CartResponseDTO.builder().userId(userId).items(items).build();
    }

    @Override
    public void clearCart(String userId) {
        cartRepository.clear(userId);
    }

    @Override
    public Integer checkout(String userId, CartCheckoutRequestDTO request, String idempotencyKey) {
        var cart = getCart(userId);
        if (cart.items().isEmpty()) {
            throw new CartEmptyException("Cannot checkout an empty cart for user " + userId);
        }

        var products = cart.items().stream()
                .map(item -> new PurchaseRequestDTO(item.variantId(), item.quantity()))
                .toList();

        var orderRequest = OrderRequestDTO.builder()
                .reference(request.reference())
                .totalAmount(request.totalAmount())
                .paymentMethode(request.paymentMethode())
                .customerId(userId)
                .products(products)
                .stripePaymentMethodId(request.stripePaymentMethodId())
                .build();

        var orderId = orderClient.createOrder(orderRequest, idempotencyKey);
        clearCart(userId);
        return orderId;
    }
}
