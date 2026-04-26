package com.rajitha.ecommerce.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;


@Builder
public record CustomerRequestDTO(
        String id,
        @NotNull(message = "Customer first name required")
        String firstName,
        @NotNull(message = "Customer last name required")
        String lastName,
        @NotNull(message = "Customer email required")
        @Email(message = "Customer email is not valid email address")
        String email,

        AddressDTO address) {

}
