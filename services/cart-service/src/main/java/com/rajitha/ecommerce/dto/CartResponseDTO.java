package com.rajitha.ecommerce.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record CartResponseDTO(
        String userId,
        List<CartItemResponseDTO> items
) {
}
