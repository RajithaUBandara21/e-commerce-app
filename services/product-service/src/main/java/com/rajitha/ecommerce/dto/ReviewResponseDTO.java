package com.rajitha.ecommerce.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ReviewResponseDTO(
        Integer id,
        Integer productId,
        String customerId,
        int rating,
        String comment,
        LocalDateTime createdDate
) {
}
