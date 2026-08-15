package com.rajitha.ecommerce.dto;

import java.math.BigDecimal;

public record ProductPurchaseResponseDTO(
        Integer variantId,
        Integer productId,
        String name,
        String description,
        String size,
        String color,
        BigDecimal price,
        Double quantity
){
}
