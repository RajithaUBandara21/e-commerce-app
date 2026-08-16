package com.rajitha.ecommerce.messaging.consumer;

import com.rajitha.ecommerce.dto.PaymentNotificationRequestDTO;
import com.rajitha.ecommerce.dto.StockReservationResultEventDTO;
import com.rajitha.ecommerce.entity.Payment;
import com.rajitha.ecommerce.messaging.PaymentNotificationProducer;
import com.rajitha.ecommerce.repository.PaymentRepository;
import com.rajitha.ecommerce.service.SellerPayoutService;
import com.rajitha.ecommerce.service.StripePaymentService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockReservationConsumer {

    private final StripePaymentService stripePaymentService;
    private final PaymentRepository paymentRepository;
    private final PaymentNotificationProducer paymentNotificationProducer;
    private final SellerPayoutService sellerPayoutService;
    private final MeterRegistry meterRegistry;

    @KafkaListener(topics = "stock-topic", groupId = "paymentServiceStockGroup")
    public void consumeStockReservationResult(StockReservationResultEventDTO event) {
        log.info("Received stock reservation result :: <{}>", event);

        if (!event.success()) {
            return;
        }

        var chargeResult = stripePaymentService.charge(
                event.totalAmount(),
                "usd",
                event.stripePaymentMethodId(),
                event.customer() == null ? null : event.customer().email()
        );

        if (chargeResult.success()) {
            paymentRepository.save(Payment.builder()
                    .amount(event.totalAmount())
                    .paymentMethode(event.paymentMethode())
                    .orderReference(event.orderReference())
                    .stripePaymentIntentId(chargeResult.stripePaymentIntentId())
                    .build());
            sellerPayoutService.recordPayoutsForOrder(event.orderReference(), event.products());
        }

        chargeCounter(chargeResult.success() ? "success" : "failure").increment();

        paymentNotificationProducer.sendNotification(
                PaymentNotificationRequestDTO.builder()
                        .orderReference(event.orderReference())
                        .amount(event.totalAmount())
                        .paymentMethode(event.paymentMethode())
                        .success(chargeResult.success())
                        .reason(chargeResult.failureReason())
                        .customerEmail(event.customer() == null ? null : event.customer().email())
                        .customerFirstName(event.customer() == null ? null : event.customer().firstName())
                        .customerLastName(event.customer() == null ? null : event.customer().lastName())
                        .build()
        );
    }

    // Saga decision point: how often a reserved-stock order actually gets
    // charged successfully vs. fails at Stripe — distinct from stock reservation
    // failing, which never reaches this consumer at all (see the early return
    // above).
    private Counter chargeCounter(String result) {
        return Counter.builder("payment_charge_total")
                .description("Stripe charge attempts from the checkout saga, by result")
                .tag("result", result)
                .register(meterRegistry);
    }
}
