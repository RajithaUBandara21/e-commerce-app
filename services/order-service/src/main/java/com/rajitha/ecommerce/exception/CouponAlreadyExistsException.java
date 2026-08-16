package com.rajitha.ecommerce.exception;

public class CouponAlreadyExistsException extends RuntimeException {
    public CouponAlreadyExistsException(String message) {
        super(message);
    }
}
