package com.rajitha.ecommerce.dto;
import com.rajitha.ecommerce.enums.PaymentMethode;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record OrderConfirmationDTO(
        String orderReference,
        BigDecimal totalAmount,
        PaymentMethode paymentMethode,
        CustomerResponseDTO customerResponseDTO,
        List<PurchaseResponseDTO> products

) {
}
