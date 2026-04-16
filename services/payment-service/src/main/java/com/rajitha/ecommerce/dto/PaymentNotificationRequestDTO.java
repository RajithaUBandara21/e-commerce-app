package com.rajitha.ecommerce.dto;
import com.rajitha.ecommerce.enums.PaymentMethode;
import jakarta.validation.constraints.Email;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record PaymentNotificationRequestDTO(
        String orderReference,
        BigDecimal amount,
        PaymentMethode  paymentMethode,
        String customerFirstName,
        String customerLastName,
        Email customerEmail
) {
}
