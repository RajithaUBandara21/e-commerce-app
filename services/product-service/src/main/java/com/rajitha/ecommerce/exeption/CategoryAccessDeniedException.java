package com.rajitha.ecommerce.exeption;

public class CategoryAccessDeniedException extends RuntimeException {
    public CategoryAccessDeniedException(String message) {
        super(message);
    }
}
