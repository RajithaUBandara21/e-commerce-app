package com.rajitha.ecommerce.dto;



public record CustomerResponseDTO(
        String id,
        String firstName,
        String lastName,
        String email,
        AddressDTO address) {}
