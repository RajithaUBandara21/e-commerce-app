package com.rajitha.ecommerce.dto;

import com.rajitha.ecommerce.enums.PaymentMethode;

import java.math.BigDecimal;

public record PaymentRequestDTO(

        Integer id,
        BigDecimal amount,
        PaymentMethode  paymentMethode,
        Integer orderId,
        String orderReference,
        CustomerDTO customerDTO
) {
}
