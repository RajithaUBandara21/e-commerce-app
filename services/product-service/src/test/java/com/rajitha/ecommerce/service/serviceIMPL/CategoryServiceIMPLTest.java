package com.rajitha.ecommerce.service.serviceIMPL;

import com.rajitha.ecommerce.dto.CategoryRequestDTO;
import com.rajitha.ecommerce.entity.Category;
import com.rajitha.ecommerce.entity.Product;
import com.rajitha.ecommerce.exeption.CategoryNotEmptyException;
import com.rajitha.ecommerce.exeption.CategoryNotFoundException;
import com.rajitha.ecommerce.mapper.CategoryMapper;
import com.rajitha.ecommerce.repository.CategoryRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class CategoryServiceIMPLTest {

    @InjectMocks
    private CategoryServiceIMPL categoryService;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryMapper categoryMapper;

    @Test
    void shouldCreateCategory() {
        var requestDTO = CategoryRequestDTO.builder().name("Shirts").description("All shirts").build();
        var category = Category.builder().name("Shirts").description("All shirts").build();
        var saved = Category.builder().id(1).name("Shirts").description("All shirts").build();

        Mockito.when(categoryMapper.toCategoryEntity(requestDTO)).thenReturn(category);
        Mockito.when(categoryRepository.save(category)).thenReturn(saved);

        var id = categoryService.createCategory(requestDTO);

        assertEquals(1, id);
    }

    @Test
    void shouldRejectDeletingNonEmptyCategory() {
        var category = Category.builder().id(1).name("Shirts").products(List.of(new Product())).build();
        Mockito.when(categoryRepository.findById(1)).thenReturn(Optional.of(category));

        Assertions.assertThrows(CategoryNotEmptyException.class, () -> categoryService.deleteCategory(1));

        Mockito.verify(categoryRepository, Mockito.never()).delete(Mockito.any(Category.class));
    }

    @Test
    void shouldDeleteEmptyCategory() {
        var category = Category.builder().id(1).name("Shirts").products(List.of()).build();
        Mockito.when(categoryRepository.findById(1)).thenReturn(Optional.of(category));

        categoryService.deleteCategory(1);

        Mockito.verify(categoryRepository).delete(category);
    }

    @Test
    void shouldThrowWhenDeletingUnknownCategory() {
        Mockito.when(categoryRepository.findById(99)).thenReturn(Optional.empty());

        Assertions.assertThrows(CategoryNotFoundException.class, () -> categoryService.deleteCategory(99));
    }

    @Test
    void shouldThrowWhenUpdatingUnknownCategory() {
        Mockito.when(categoryRepository.findById(99)).thenReturn(Optional.empty());
        var requestDTO = CategoryRequestDTO.builder().name("New").build();

        Assertions.assertThrows(CategoryNotFoundException.class, () -> categoryService.updateCategory(99, requestDTO));
    }
}
