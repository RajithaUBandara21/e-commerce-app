package com.rajitha.ecommerce.service;

import java.math.BigDecimal;

public interface StripeTransferService {

    record TransferResult(boolean success, String stripeTransferId, String failureReason) {
        public static TransferResult success(String stripeTransferId) {
            return new TransferResult(true, stripeTransferId, null);
        }

        public static TransferResult failure(String reason) {
            return new TransferResult(false, null, reason);
        }
    }

    TransferResult transfer(BigDecimal amount, String currency, String destinationStripeAccountId, String orderReference);
}
