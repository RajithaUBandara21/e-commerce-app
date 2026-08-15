package com.rajitha.ecommerce.dto;

import lombok.Builder;

@Builder
public record PaymentResultEventDTO(
        String orderReference,
        boolean success,
        String reason
) {
}
