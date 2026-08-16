package com.rajitha.ecommerce.exeption;

public class ReviewNotVerifiedPurchaseException extends RuntimeException {
    public ReviewNotVerifiedPurchaseException(String message) {
        super(message);
    }
}
