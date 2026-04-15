package com.rajitha.ecommerce.dto;
import com.rajitha.ecommerce.enums.PaymentMethode;
import lombok.Builder;
import java.math.BigDecimal;

@Builder
public record OrderResponseDTO(
        Integer id,
        String reference,
        BigDecimal amount,
        PaymentMethode paymentMethode,
        String customerId
) {

}
