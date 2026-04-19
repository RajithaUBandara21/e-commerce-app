package com.rajitha.ecommerce.dto;

import com.rajitha.ecommerce.enums.PaymentMethode;

import java.math.BigDecimal;

public record PaymentConfirmationDTO(
       String orderReference,
       BigDecimal amount,
       PaymentMethode paymentMethode,
       String customerFirstName,
       String customerLastName,
       String customerEmail
) {
}
