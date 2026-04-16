package com.rajitha.ecommerce.dto;
import com.rajitha.ecommerce.enums.PaymentMethode;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record PaymentNotificationRequestDTO(
        String orderReference,
        BigDecimal amount,
        PaymentMethode  paymentMethode,
        String customerFirstName,
        String customerLastName,
        String customerEmail
) {
}
