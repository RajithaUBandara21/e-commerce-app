package com.rajitha.ecommerce.controller;

import com.rajitha.ecommerce.dto.CategoryRequestDTO;
import com.rajitha.ecommerce.dto.CategoryResponseDTO;
import com.rajitha.ecommerce.exeption.CategoryAccessDeniedException;
import com.rajitha.ecommerce.service.CategoryService;
import com.rajitha.ecommerce.util.RolesHeader;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Mutations are admin-only (gateway also gates these routes to ROLE_ADMIN — see
// SecurityConfiguration — this is defense in depth, not the only check). Reads stay
// public, same as products.
@RestController
@RequestMapping("/api/v1/categories")
@AllArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    public ResponseEntity<Integer> createCategory(
            @RequestBody @Valid CategoryRequestDTO requestDTO,
            @RequestHeader(value = "X-User-Roles", defaultValue = "") String roles) {
        requireAdmin(roles);
        return ResponseEntity.ok(categoryService.createCategory(requestDTO));
    }

    @PutMapping("/{category-id}")
    public ResponseEntity<Void> updateCategory(
            @PathVariable("category-id") Integer categoryId,
            @RequestBody @Valid CategoryRequestDTO requestDTO,
            @RequestHeader(value = "X-User-Roles", defaultValue = "") String roles) {
        requireAdmin(roles);
        categoryService.updateCategory(categoryId, requestDTO);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{category-id}")
    public ResponseEntity<Void> deleteCategory(
            @PathVariable("category-id") Integer categoryId,
            @RequestHeader(value = "X-User-Roles", defaultValue = "") String roles) {
        requireAdmin(roles);
        categoryService.deleteCategory(categoryId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{category-id}")
    public ResponseEntity<CategoryResponseDTO> findById(@PathVariable("category-id") Integer categoryId) {
        return ResponseEntity.ok(categoryService.findCategoryById(categoryId));
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponseDTO>> findAll() {
        return ResponseEntity.ok(categoryService.findAllCategories());
    }

    private void requireAdmin(String roles) {
        if (!RolesHeader.isAdmin(roles)) {
            throw new CategoryAccessDeniedException("Only admins can manage categories");
        }
    }
}
