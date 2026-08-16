package com.rajitha.ecommerce.dto;

import com.rajitha.ecommerce.enums.CouponType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record CouponRequestDTO(
        @NotBlank(message = "Coupon code is required")
        String code,
        @NotNull(message = "Coupon type is required")
        CouponType type,
        @NotNull(message = "Coupon value is required")
        @Positive(message = "Coupon value must be positive")
        BigDecimal value,
        BigDecimal minOrderAmount,
        LocalDateTime expiresAt,
        Boolean active
) {
}
