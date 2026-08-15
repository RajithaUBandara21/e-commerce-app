package com.rajitha.ecommerce.messaging.consumer;

import com.rajitha.ecommerce.dto.PaymentNotificationRequestDTO;
import com.rajitha.ecommerce.dto.StockReservationResultEventDTO;
import com.rajitha.ecommerce.entity.Payment;
import com.rajitha.ecommerce.messaging.PaymentNotificationProducer;
import com.rajitha.ecommerce.repository.PaymentRepository;
import com.rajitha.ecommerce.service.StripePaymentService;
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
                    .build());
        }

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
}
