package com.rajitha.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record ProductImageRegisterRequestDTO(
        @NotBlank(message = "objectKey is required")
        String objectKey
) {
}
