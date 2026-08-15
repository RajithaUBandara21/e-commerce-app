package com.rajitha.ecommerce.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record StockReleaseEventDTO(
        String orderReference,
        List<PurchaseRequestDTO> products
) {
}
