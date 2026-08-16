package com.rajitha.ecommerce.dto;

import com.rajitha.ecommerce.enums.CouponType;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record CouponResponseDTO(
        Integer id,
        String code,
        CouponType type,
        BigDecimal value,
        BigDecimal minOrderAmount,
        LocalDateTime expiresAt,
        boolean active,
        LocalDateTime createdDate
) {
}
