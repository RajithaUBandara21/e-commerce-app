package com.rajitha.ecommerce.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PurchaseRequestDTO(
        @NotNull(message = "Product is mandetory")
        Integer productId,
        @Positive(message = "Quantity is mandetory")
        double quantity
) {
}
