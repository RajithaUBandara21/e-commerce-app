package com.rajitha.ecommerce.dto;

import com.rajitha.ecommerce.enums.PaymentMethod;

import java.math.BigDecimal;

public record PaymentConfirmationDTO(
       String orderReference,
       BigDecimal amount,
       PaymentMethod paymentMethod,
       String customerFirstName,
       String customerLastName,
       String customerEmail
) {
}
