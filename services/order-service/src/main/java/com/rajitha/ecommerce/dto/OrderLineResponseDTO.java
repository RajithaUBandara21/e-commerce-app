package com.rajitha.ecommerce.dto;
import lombok.Builder;

@Builder
public record OrderLineResponseDTO(
        Integer id,
        Integer variantId,
        double quantity
) {
}
