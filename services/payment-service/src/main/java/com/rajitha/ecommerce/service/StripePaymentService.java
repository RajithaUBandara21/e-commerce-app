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

    record RefundResult(boolean success, String stripeRefundId, String failureReason) {
        public static RefundResult success(String stripeRefundId) {
            return new RefundResult(true, stripeRefundId, null);
        }

        public static RefundResult failure(String reason) {
            return new RefundResult(false, null, reason);
        }
    }

    ChargeResult charge(BigDecimal amount, String currency, String paymentMethodId, String customerEmail);

    RefundResult refund(String stripePaymentIntentId);
}
