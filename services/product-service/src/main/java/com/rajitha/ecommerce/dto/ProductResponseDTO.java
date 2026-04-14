package com.rajitha.ecommerce.dto;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

import java.math.BigDecimal;
@Builder
public record ProductResponceDTO (
        int id,

        String name,

        String description,

        double availableQuantity,

        BigDecimal price,

        Integer categoryId,

        String categoryName,
        
){

}
