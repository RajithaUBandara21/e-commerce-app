package com.rajitha.ecommerce.dto;

import java.math.BigDecimal;

public record PurchaseResponseDTO(
        Integer productId,
        String name,
        String description,
        BigDecimal price,
        Double quantity
) {
}
