package com.rajitha.ecommerce.dto;

import lombok.Builder;

@Builder
public record CartItemResponseDTO(
        Integer variantId,
        double quantity
) {
}
