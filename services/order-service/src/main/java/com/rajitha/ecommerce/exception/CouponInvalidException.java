package com.rajitha.ecommerce.exception;

// Not-found/expired/inactive/below-minimum — anything that makes a coupon
// unusable right now. Kept as one exception type since the caller (checkout)
// only ever needs to show "this code isn't valid: <reason>".
public class CouponInvalidException extends RuntimeException {
    public CouponInvalidException(String message) {
        super(message);
    }
}
