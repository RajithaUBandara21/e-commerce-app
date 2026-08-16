package com.rajitha.ecommerce.service.serviceImpl;

import com.rajitha.ecommerce.service.StripePaymentService;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class StripePaymentServiceImpl implements StripePaymentService {

    @Override
    public ChargeResult charge(BigDecimal amount, String currency, String paymentMethodId, String customerEmail) {
        if (paymentMethodId == null || paymentMethodId.isBlank()) {
            return ChargeResult.failure("No Stripe payment method provided");
        }

        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amount.movePointRight(2).longValueExact())
                    .setCurrency(currency)
                    .setPaymentMethod(paymentMethodId)
                    .setConfirm(true)
                    .setOffSession(true)
                    .setReceiptEmail(customerEmail)
                    .build();

            PaymentIntent intent = PaymentIntent.create(params);

            if ("succeeded".equals(intent.getStatus())) {
                return ChargeResult.success(intent.getId());
            }
            return ChargeResult.failure("Stripe payment intent status: " + intent.getStatus());
        } catch (StripeException e) {
            log.warn("Stripe charge failed :: {}", e.getMessage());
            return ChargeResult.failure(e.getMessage());
        }
    }

    @Override
    public RefundResult refund(String stripePaymentIntentId) {
        if (stripePaymentIntentId == null || stripePaymentIntentId.isBlank()) {
            return RefundResult.failure("No Stripe payment intent recorded for this payment");
        }

        try {
            var params = RefundCreateParams.builder()
                    .setPaymentIntent(stripePaymentIntentId)
                    .build();
            Refund refund = Refund.create(params);
            return RefundResult.success(refund.getId());
        } catch (StripeException e) {
            log.warn("Stripe refund failed :: {}", e.getMessage());
            return RefundResult.failure(e.getMessage());
        }
    }
}
