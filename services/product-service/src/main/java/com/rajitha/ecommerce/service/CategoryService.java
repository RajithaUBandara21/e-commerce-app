package com.rajitha.ecommerce.service;

import com.rajitha.ecommerce.dto.CategoryRequestDTO;
import com.rajitha.ecommerce.dto.CategoryResponseDTO;
import jakarta.validation.Valid;

import java.util.List;

public interface CategoryService {

    Integer createCategory(@Valid CategoryRequestDTO requestDTO);

    void updateCategory(Integer categoryId, @Valid CategoryRequestDTO requestDTO);

    void deleteCategory(Integer categoryId);

    CategoryResponseDTO findCategoryById(Integer categoryId);

    List<CategoryResponseDTO> findAllCategories();
}
