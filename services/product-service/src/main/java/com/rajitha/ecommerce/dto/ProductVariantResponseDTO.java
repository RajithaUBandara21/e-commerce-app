package com.rajitha.ecommerce.dto;

import lombok.Builder;

@Builder
public record ProductVariantResponseDTO(
        Integer id,
        String sku,
        String size,
        String color,
        double availableQuantity
) {
}
