package com.rajitha.ecommerce.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rajitha.ecommerce.enums.PaymentMethode;
import lombok.Builder;

import java.math.BigDecimal;
@Builder
public record PaymentRequestDTO(

        BigDecimal amount,
        PaymentMethode  paymentMethode,
        Integer orderId,
        String orderReference,
        @JsonProperty("customer")
        CustomerDTO customer
) {
}
