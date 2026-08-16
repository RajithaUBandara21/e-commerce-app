package com.rajitha.ecommerce.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record PurchaseResponseDTO(
        Integer variantId,
        Integer productId,
        String name,
        String description,
        String size,
        String color,
        BigDecimal price,
        Double quantity,
        String sellerId
) {
}
