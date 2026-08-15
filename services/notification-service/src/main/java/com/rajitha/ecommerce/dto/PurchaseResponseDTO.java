package com.rajitha.ecommerce.dto;

import java.math.BigDecimal;

public record PurchaseResponseDTO(
        Integer variantId,
        Integer productId,
        String name,
        String description,
        String size,
        String color,
        BigDecimal price,
        Double quantity
) {
}
