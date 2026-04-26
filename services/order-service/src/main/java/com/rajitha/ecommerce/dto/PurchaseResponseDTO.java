package com.rajitha.ecommerce.dto;

import lombok.Builder;

import java.math.BigDecimal;
@Builder
public record PurchaseResponseDTO(
        Integer productId,
        String name,
        String description,
        BigDecimal price,
        Double quantity
) {
}
