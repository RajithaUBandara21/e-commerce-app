package com.rajitha.ecommerce.service.serviceIMPL;

import com.rajitha.ecommerce.dto.CategoryRequestDTO;
import com.rajitha.ecommerce.dto.CategoryResponseDTO;
import com.rajitha.ecommerce.exeption.CategoryNotEmptyException;
import com.rajitha.ecommerce.exeption.CategoryNotFoundException;
import com.rajitha.ecommerce.mapper.CategoryMapper;
import com.rajitha.ecommerce.repository.CategoryRepository;
import com.rajitha.ecommerce.service.CategoryService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryServiceIMPL implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    public CategoryServiceIMPL(CategoryRepository categoryRepository, CategoryMapper categoryMapper) {
        this.categoryRepository = categoryRepository;
        this.categoryMapper = categoryMapper;
    }

    @Override
    public Integer createCategory(CategoryRequestDTO requestDTO) {
        var category = categoryMapper.toCategoryEntity(requestDTO);
        return categoryRepository.save(category).getId();
    }

    @Override
    public void updateCategory(Integer categoryId, CategoryRequestDTO requestDTO) {
        var category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found: " + categoryId));
        category.setName(requestDTO.name());
        category.setDescription(requestDTO.description());
        categoryRepository.save(category);
    }

    @Override
    public void deleteCategory(Integer categoryId) {
        var category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found: " + categoryId));
        // Category.products cascades REMOVE at the JPA level, which would silently
        // delete every product in a non-empty category — a destructive surprise for
        // an admin action. Reject instead; the admin must move/delete products first.
        if (category.getProducts() != null && !category.getProducts().isEmpty()) {
            throw new CategoryNotEmptyException(
                    "Category " + categoryId + " still has " + category.getProducts().size() + " product(s) — move or delete them first");
        }
        categoryRepository.delete(category);
    }

    @Override
    public CategoryResponseDTO findCategoryById(Integer categoryId) {
        return categoryRepository.findById(categoryId)
                .map(categoryMapper::toCategoryResponseDTO)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found: " + categoryId));
    }

    @Override
    public List<CategoryResponseDTO> findAllCategories() {
        return categoryRepository.findAll().stream().map(categoryMapper::toCategoryResponseDTO).collect(Collectors.toList());
    }
}
