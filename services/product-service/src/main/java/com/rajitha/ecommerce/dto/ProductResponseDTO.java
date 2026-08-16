package com.rajitha.ecommerce.dto;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;
@Builder
public record ProductResponseDTO(
        int id,

        String name,

        String description,

        BigDecimal price,

        String sellerId,

        Integer categoryId,

        String categoryName,

        String categoryDescription,

        List<ProductVariantResponseDTO> variants
){

}
