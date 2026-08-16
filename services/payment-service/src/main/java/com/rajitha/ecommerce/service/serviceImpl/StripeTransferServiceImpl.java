package com.rajitha.ecommerce.service.serviceImpl;

import com.rajitha.ecommerce.service.StripeTransferService;
import com.stripe.exception.StripeException;
import com.stripe.model.Transfer;
import com.stripe.param.TransferCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

// The "transfers" half of Stripe Connect's "separate charges and transfers" model —
// the platform already charged the customer in full (StripePaymentServiceImpl); this
// moves a seller's net share of that charge out to their connected Express account.
@Service
@Slf4j
public class StripeTransferServiceImpl implements StripeTransferService {

    @Override
    public TransferResult transfer(BigDecimal amount, String currency, String destinationStripeAccountId, String orderReference) {
        if (destinationStripeAccountId == null || destinationStripeAccountId.isBlank()) {
            return TransferResult.failure("Seller has no Stripe Connect account");
        }

        try {
            var params = TransferCreateParams.builder()
                    .setAmount(amount.movePointRight(2).longValueExact())
                    .setCurrency(currency)
                    .setDestination(destinationStripeAccountId)
                    .setTransferGroup(orderReference)
                    .build();

            Transfer transfer = Transfer.create(params);
            return TransferResult.success(transfer.getId());
        } catch (StripeException e) {
            log.warn("Stripe transfer failed for order {} :: {}", orderReference, e.getMessage());
            return TransferResult.failure(e.getMessage());
        }
    }
}
