package com.rajitha.ecommerce.dto;

import com.rajitha.ecommerce.enums.PaymentMethode;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record StockReservationResultEventDTO(
        String orderReference,
        boolean success,
        String reason,
        BigDecimal totalAmount,
        PaymentMethode paymentMethode,
        String stripePaymentMethodId,
        CustomerDTO customer
) {
}
