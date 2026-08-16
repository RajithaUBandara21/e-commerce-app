package com.rajitha.ecommerce.dto;

import com.rajitha.ecommerce.enums.PayoutStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record SellerPayoutResponseDTO(
        Integer id,
        String sellerId,
        String orderReference,
        BigDecimal grossAmount,
        BigDecimal commissionAmount,
        BigDecimal netAmount,
        PayoutStatus status,
        String stripeTransferId,
        String failureReason,
        LocalDateTime createdDate
) {
}
