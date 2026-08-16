package com.rajitha.ecommerce.dto;
import com.rajitha.ecommerce.enums.OrderLineStatus;
import lombok.Builder;

@Builder
public record OrderLineResponseDTO(
        Integer id,
        Integer orderId,
        Integer variantId,
        double quantity,
        String sellerId,
        OrderLineStatus status,
        String trackingNumber
) {
}
