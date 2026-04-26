package com.rajitha.ecommerce.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

@Builder
public record PurchaseRequestDTO(
        @NotNull(message = "Product is mandatory")
        Integer productId,
        @Positive(message = "Quantity is mandatory")
        double quantity
) {
}
