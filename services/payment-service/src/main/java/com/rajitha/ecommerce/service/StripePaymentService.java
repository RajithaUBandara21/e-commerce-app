package com.rajitha.ecommerce.service;

import java.math.BigDecimal;

public interface StripePaymentService {

    record ChargeResult(boolean success, String stripePaymentIntentId, String failureReason) {
        public static ChargeResult success(String stripePaymentIntentId) {
            return new ChargeResult(true, stripePaymentIntentId, null);
        }

        public static ChargeResult failure(String reason) {
            return new ChargeResult(false, null, reason);
        }
    }

    ChargeResult charge(BigDecimal amount, String currency, String paymentMethodId, String customerEmail);
}
