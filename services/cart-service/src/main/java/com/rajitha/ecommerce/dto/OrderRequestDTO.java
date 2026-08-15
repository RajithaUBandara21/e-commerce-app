package com.rajitha.ecommerce.dto;

import com.rajitha.ecommerce.enums.PaymentMethode;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record OrderRequestDTO(
        Integer id,
        String reference,
        BigDecimal totalAmount,
        PaymentMethode paymentMethode,
        String customerId,
        List<PurchaseRequestDTO> products,
        String stripePaymentMethodId
) {
}
