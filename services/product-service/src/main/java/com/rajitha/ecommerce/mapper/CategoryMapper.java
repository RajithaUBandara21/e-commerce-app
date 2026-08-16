package com.rajitha.ecommerce.mapper;

import com.rajitha.ecommerce.dto.CategoryRequestDTO;
import com.rajitha.ecommerce.dto.CategoryResponseDTO;
import com.rajitha.ecommerce.entity.Category;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    public Category toCategoryEntity(CategoryRequestDTO requestDTO) {
        return Category.builder()
                .name(requestDTO.name())
                .description(requestDTO.description())
                .build();
    }

    public CategoryResponseDTO toCategoryResponseDTO(Category category) {
        var productCount = category.getProducts() == null ? 0 : category.getProducts().size();
        return CategoryResponseDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .productCount(productCount)
                .build();
    }
}
