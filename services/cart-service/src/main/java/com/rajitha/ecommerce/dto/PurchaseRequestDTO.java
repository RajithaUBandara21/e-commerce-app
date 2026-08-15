package com.rajitha.ecommerce.dto;

public record PurchaseRequestDTO(
        Integer variantId,
        double quantity
) {
}
