package com.rajitha.ecommerce.dto;

import com.rajitha.ecommerce.enums.PaymentMethode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record CartCheckoutRequestDTO(
        @NotBlank(message = "Order reference is mandatory")
        String reference,
        @Positive(message = "Total amount should be positive")
        BigDecimal totalAmount,
        @NotNull(message = "Payment method should be precised")
        PaymentMethode paymentMethode,
        String stripePaymentMethodId
) {
}
