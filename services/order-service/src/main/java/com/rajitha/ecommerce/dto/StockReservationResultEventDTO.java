package com.rajitha.ecommerce.dto;

import com.rajitha.ecommerce.enums.PaymentMethode;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record StockReservationResultEventDTO(
        String orderReference,
        boolean success,
        String reason,
        BigDecimal totalAmount,
        PaymentMethode paymentMethode,
        String stripePaymentMethodId,
        CustomerResponseDTO customer,
        List<PurchaseResponseDTO> products
) {
}
