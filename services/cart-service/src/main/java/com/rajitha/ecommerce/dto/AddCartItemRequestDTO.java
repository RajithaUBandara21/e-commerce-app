package com.rajitha.ecommerce.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

@Builder
public record AddCartItemRequestDTO(
        @NotNull(message = "Variant id is mandatory")
        Integer variantId,
        @Positive(message = "Quantity must be positive")
        double quantity
) {
}
