package com.rajitha.ecommerce.dto;

import lombok.Builder;

@Builder
public record PaymentRefundRequestDTO(String orderReference) {
}
