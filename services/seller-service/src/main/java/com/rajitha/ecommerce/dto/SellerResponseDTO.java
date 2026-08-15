package com.rajitha.ecommerce.dto;

import com.rajitha.ecommerce.enums.SellerStatus;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record SellerResponseDTO(
        Integer id,
        String keycloakUserId,
        String businessName,
        String businessEmail,
        String description,
        SellerStatus status,
        String stripeAccountId,
        boolean chargesEnabled,
        boolean payoutsEnabled,
        LocalDateTime createdDate
) {
}
