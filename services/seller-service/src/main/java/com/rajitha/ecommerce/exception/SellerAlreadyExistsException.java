package com.rajitha.ecommerce.exception;

public class SellerAlreadyExistsException extends RuntimeException {
    public SellerAlreadyExistsException(String message) {
        super(message);
    }
}
