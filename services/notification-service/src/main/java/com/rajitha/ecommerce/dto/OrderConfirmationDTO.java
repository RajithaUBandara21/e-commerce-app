package com.rajitha.ecommerce.dto;
import com.rajitha.ecommerce.enums.PaymentMethod;
import java.math.BigDecimal;
import java.util.List;

public record OrderConfirmationDTO(
        String orderReference,
        BigDecimal totalAmount,
        PaymentMethod paymentMethod,
        CustomerDTO customer,
        List<ProductDTO> productsList
) {
}
