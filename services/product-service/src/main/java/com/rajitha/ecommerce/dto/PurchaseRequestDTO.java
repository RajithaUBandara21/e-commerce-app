       package com.rajitha.ecommerce.dto;

       import jakarta.validation.constraints.NotNull;
       import jakarta.validation.constraints.Positive;

       public record PurchaseRequestDTO (
              @NotNull(message = "Product id is mandatory")
              Integer productId,
              @NotNull(message = "Quantity id is mandatory")
              @Positive(message = "Quantity must be positive")
              double quantity
       ){
       }
