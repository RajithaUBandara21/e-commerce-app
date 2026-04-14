package com.rajitha.ecommerce.dto;
import lombok.Builder;

import java.math.BigDecimal;
@Builder
public record ProductResponseDTO(
        int id,

        String name,

        String description,

        double availableQuantity,

        BigDecimal price,

        Integer categoryId,

        String categoryName,

        String categoryDescription
){

}
