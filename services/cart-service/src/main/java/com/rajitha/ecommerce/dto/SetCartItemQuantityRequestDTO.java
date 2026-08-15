package com.rajitha.ecommerce.dto;

import jakarta.validation.constraints.PositiveOrZero;
import lombok.Builder;

@Builder
public record SetCartItemQuantityRequestDTO(
        @PositiveOrZero(message = "Quantity cannot be negative")
        double quantity
) {
}
