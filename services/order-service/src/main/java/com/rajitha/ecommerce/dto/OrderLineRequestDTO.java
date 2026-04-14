package com.rajitha.ecommerce.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderLineRequestDTO(
Integer id,
Integer orderId,
Integer productId,
double quantity
) {
}
