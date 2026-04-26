package com.rajitha.ecommerce.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import org.springframework.validation.annotation.Validated;
@Builder
@Validated
public record CustomerDTO(
        String id,
        @NotNull(message = "First name is required")
        String firstName,
        @NotNull(message = "Last name is required")
        String lastName,
        @NotNull(message = "Email is required")
        @Email(message = "The customer not correctly formated in Email")
        String email,
        AddressDTO address
) {
}
