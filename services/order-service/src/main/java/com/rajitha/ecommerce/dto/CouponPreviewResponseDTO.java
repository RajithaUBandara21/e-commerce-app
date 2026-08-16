package com.rajitha.ecommerce.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record CouponPreviewResponseDTO(BigDecimal originalAmount, BigDecimal discountedAmount) {
}
