package com.rajitha.ecommerce.dto;

import com.rajitha.ecommerce.enums.OrderLineStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record OrderLineFulfillmentRequestDTO(
        @NotNull(message = "Status is required")
        OrderLineStatus status,
        String trackingNumber
) {
}
