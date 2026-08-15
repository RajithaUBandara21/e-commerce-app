package com.rajitha.ecommerce.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record SellerRegistrationRequestDTO(
        @NotBlank(message = "Business name is required")
        String businessName,
        @NotBlank(message = "Business email is required")
        @Email(message = "Business email must be a valid email address")
        String businessEmail,
        String description
) {
}
