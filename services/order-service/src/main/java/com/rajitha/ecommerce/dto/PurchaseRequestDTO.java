package com.rajitha.ecommerce.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

@Builder
public record PurchaseRequestDTO(
        @NotNull(message = "Product variant is mandatory")
        Integer variantId,
        @Positive(message = "Quantity is mandatory")
        double quantity
) {
}
