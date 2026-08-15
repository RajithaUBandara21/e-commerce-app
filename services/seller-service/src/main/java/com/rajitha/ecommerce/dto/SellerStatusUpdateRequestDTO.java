package com.rajitha.ecommerce.dto;

import com.rajitha.ecommerce.enums.SellerStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record SellerStatusUpdateRequestDTO(
        @NotNull(message = "Status is required")
        SellerStatus status
) {
}
