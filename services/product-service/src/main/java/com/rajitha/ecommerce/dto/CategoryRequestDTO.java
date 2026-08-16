package com.rajitha.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record CategoryRequestDTO(
        @NotBlank(message = "Category name is required")
        String name,
        String description
) {
}
