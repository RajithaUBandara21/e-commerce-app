package com.rajitha.ecommerce.dto;

import jakarta.validation.constraints.Email;
import lombok.Builder;

@Builder
public record CustomerResponseDTO(
        String id,
        String firstName,
        String lastName,
        String email,
        AddressDTO address
) {
}
