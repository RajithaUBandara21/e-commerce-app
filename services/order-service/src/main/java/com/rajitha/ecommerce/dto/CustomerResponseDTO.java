package com.rajitha.ecommerce.dto;

import jakarta.validation.constraints.Email;

public record CustomerResponseDTO(
        String id,
        String firstName,
        String lastName,
        String email,
        AddressDTO address
) {
}
