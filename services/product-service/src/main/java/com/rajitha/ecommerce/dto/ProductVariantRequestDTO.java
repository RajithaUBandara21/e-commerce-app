package com.rajitha.ecommerce.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Builder;

@Builder
public record ProductVariantRequestDTO(
        Integer id,
        @NotNull(message = "Variant SKU is required")
        String sku,
        @NotNull(message = "Variant size is required")
        String size,
        @NotNull(message = "Variant color is required")
        String color,
        @PositiveOrZero(message = "Available quantity cannot be negative")
        double availableQuantity
) {
}
