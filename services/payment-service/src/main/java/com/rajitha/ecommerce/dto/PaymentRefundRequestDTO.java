package com.rajitha.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record PaymentRefundRequestDTO(
        @NotBlank(message = "Order reference is required")
        String orderReference
) {
}
