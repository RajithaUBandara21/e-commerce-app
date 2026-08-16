package com.rajitha.ecommerce.dto;

import lombok.Builder;

@Builder
public record CategoryResponseDTO(
        int id,
        String name,
        String description,
        int productCount
) {
}
