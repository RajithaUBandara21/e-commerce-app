package com.rajitha.ecommerce.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ProductRequestDTO
        (

        int id,
        @NotNull(message = " Product name is required")
        String name,
        @NotNull(message = " Product description is required")
        String description,
        @Positive(message = "Available quantity should be positive")
        double availableQuantity,
        @Positive(message = "Product price should be positive")
        BigDecimal price,
        @NotNull(message = "Product category should be required")
        Integer categoryId
)
{}
