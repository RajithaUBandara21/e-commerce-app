package com.rajitha.ecommerce.dto;

import com.rajitha.ecommerce.enums.PaymentMethode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

public record OrderRequestDTO(
        Integer id,
        String reference,
        @Positive(message = "Order amount should be positive ")
        BigDecimal totalAmount,
        @NotNull(message = "Payment method should be precised")
        PaymentMethode paymentMethode,
        @NotNull(message = "Customer should be precised")
        @NotEmpty(message = "Customer should be precised")
        @NotBlank(message = "Customer should be precised")
        String customerId,
        @NotEmpty(message = "you should at least purchase one product")
        List<PurchaseRequestDTO> products
) {
    }
