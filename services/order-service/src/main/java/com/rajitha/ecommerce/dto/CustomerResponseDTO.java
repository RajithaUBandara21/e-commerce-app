package com.rajitha.ecommerce.dto;

import jakarta.validation.constraints.Email;

public record CustomerResponseDTO(
        String id,
        String firstname,
        String lastname,
        Email email

) {
}
