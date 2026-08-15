package com.rajitha.ecommerce.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record StockReservationResultEventDTO(
        String orderReference,
        boolean success,
        String reason,
        BigDecimal totalAmount,
        String paymentMethode,
        String stripePaymentMethodId,
        JsonNode customer,
        List<ProductPurchaseResponseDTO> products
) {
}
