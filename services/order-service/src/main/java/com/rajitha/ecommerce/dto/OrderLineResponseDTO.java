package com.rajitha.ecommerce.dto;
import lombok.Builder;

@Builder
public record OrderLineResponseDTO(
        Integer id,
        double quantity
) {
}
