package com.rajitha.ecommerce.mapper;

import com.rajitha.ecommerce.dto.CategoryRequestDTO;
import com.rajitha.ecommerce.entity.Category;
import com.rajitha.ecommerce.entity.Product;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

class CategoryMapperTest {

    private CategoryMapper categoryMapper;

    @BeforeEach
    void setUp() {
        categoryMapper = new CategoryMapper();
    }

    @Test
    void shouldMapToCategoryEntity() {
        var requestDTO = CategoryRequestDTO.builder().name("Shirts").description("All shirts").build();

        var category = categoryMapper.toCategoryEntity(requestDTO);

        Assertions.assertEquals("Shirts", category.getName());
        Assertions.assertEquals("All shirts", category.getDescription());
    }

    @Test
    void shouldMapToCategoryResponseDTOWithProductCount() {
        var category = new Category();
        category.setId(1);
        category.setName("Shirts");
        category.setDescription("All shirts");
        category.setProducts(List.of(new Product(), new Product()));

        var responseDTO = categoryMapper.toCategoryResponseDTO(category);

        Assertions.assertEquals(1, responseDTO.id());
        Assertions.assertEquals("Shirts", responseDTO.name());
        Assertions.assertEquals(2, responseDTO.productCount());
    }

    @Test
    void shouldMapToCategoryResponseDTOWithNoProducts() {
        var category = new Category();
        category.setId(1);
        category.setName("Shirts");

        var responseDTO = categoryMapper.toCategoryResponseDTO(category);

        Assertions.assertEquals(0, responseDTO.productCount());
    }
}
