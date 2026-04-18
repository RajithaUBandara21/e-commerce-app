package com.rajitha.ecommerce.dto;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.rajitha.ecommerce.enums.PaymentMethode;


import java.math.BigDecimal;

public record PaymentRequestDTO(

        BigDecimal amount,
        PaymentMethode paymentMethode,
        Integer orderId,
        String orderReference,
        @JsonProperty("customer")
        CustomerResponseDTO customer
) {
}
