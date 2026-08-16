package com.rajitha.ecommerce.exception;

public class SellerAccessDeniedException extends RuntimeException {
    public SellerAccessDeniedException(String message) {
        super(message);
    }
}
