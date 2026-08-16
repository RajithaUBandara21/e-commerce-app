package com.rajitha.ecommerce.exeption;

public class ReviewAccessDeniedException extends RuntimeException {
    public ReviewAccessDeniedException(String message) {
        super(message);
    }
}
