package com.rajitha.ecommerce.messaging.consumer;

import com.rajitha.ecommerce.dto.CustomerDTO;
import com.rajitha.ecommerce.dto.PaymentNotificationRequestDTO;
import com.rajitha.ecommerce.dto.PurchaseResponseDTO;
import com.rajitha.ecommerce.dto.StockReservationResultEventDTO;
import com.rajitha.ecommerce.entity.Payment;
import com.rajitha.ecommerce.enums.PaymentMethode;
import com.rajitha.ecommerce.messaging.PaymentNotificationProducer;
import com.rajitha.ecommerce.repository.PaymentRepository;
import com.rajitha.ecommerce.service.SellerPayoutService;
import com.rajitha.ecommerce.service.StripePaymentService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class StockReservationConsumerTest {

    @InjectMocks
    StockReservationConsumer stockReservationConsumer;
    @Mock StripePaymentService stripePaymentService;
    @Mock PaymentRepository paymentRepository;
    @Mock PaymentNotificationProducer paymentNotificationProducer;
    @Mock SellerPayoutService sellerPayoutService;

    @Test
    void shouldChargeAndSaveAndNotifyWhenReservationSucceedsAndChargeSucceeds(){

        CustomerDTO customer = CustomerDTO.builder().id("Id-1").firstName("first").lastName("last").email("a@b.com").build();

        StockReservationResultEventDTO event = StockReservationResultEventDTO.builder()
                .orderReference("reference")
                .success(true)
                .totalAmount(new BigDecimal("100"))
                .paymentMethode(PaymentMethode.BITCOIN)
                .stripePaymentMethodId("pm_123")
                .customer(customer)
                .products(List.of(PurchaseResponseDTO.builder().variantId(1).sellerId("seller-1")
                        .price(new BigDecimal("100")).quantity(1.0).build()))
                .build();

        Mockito.when(stripePaymentService.charge(new BigDecimal("100"), "usd", "pm_123", "a@b.com"))
                .thenReturn(StripePaymentService.ChargeResult.success("pi_123"));

        stockReservationConsumer.consumeStockReservationResult(event);

        Mockito.verify(paymentRepository, Mockito.times(1)).save(Mockito.any(Payment.class));
        Mockito.verify(sellerPayoutService, Mockito.times(1)).recordPayoutsForOrder("reference", event.products());

        var captor = ArgumentCaptor.forClass(PaymentNotificationRequestDTO.class);
        Mockito.verify(paymentNotificationProducer, Mockito.times(1)).sendNotification(captor.capture());
        Assertions.assertTrue(captor.getValue().success());
        Assertions.assertEquals("reference", captor.getValue().orderReference());
    }

    @Test
    void shouldNotSavePaymentAndNotifyFailureWhenChargeFails(){

        CustomerDTO customer = CustomerDTO.builder().id("Id-1").firstName("first").lastName("last").email("a@b.com").build();

        StockReservationResultEventDTO event = StockReservationResultEventDTO.builder()
                .orderReference("reference")
                .success(true)
                .totalAmount(new BigDecimal("100"))
                .paymentMethode(PaymentMethode.BITCOIN)
                .stripePaymentMethodId(null)
                .customer(customer)
                .build();

        Mockito.when(stripePaymentService.charge(new BigDecimal("100"), "usd", null, "a@b.com"))
                .thenReturn(StripePaymentService.ChargeResult.failure("No Stripe payment method provided"));

        stockReservationConsumer.consumeStockReservationResult(event);

        Mockito.verify(paymentRepository, Mockito.never()).save(Mockito.any(Payment.class));
        Mockito.verify(sellerPayoutService, Mockito.never()).recordPayoutsForOrder(Mockito.any(), Mockito.any());

        var captor = ArgumentCaptor.forClass(PaymentNotificationRequestDTO.class);
        Mockito.verify(paymentNotificationProducer, Mockito.times(1)).sendNotification(captor.capture());
        Assertions.assertFalse(captor.getValue().success());
        Assertions.assertEquals("No Stripe payment method provided", captor.getValue().reason());
    }

    @Test
    void shouldDoNothingWhenStockReservationFailed(){

        StockReservationResultEventDTO event = StockReservationResultEventDTO.builder()
                .orderReference("reference")
                .success(false)
                .reason("Insufficient stock")
                .build();

        stockReservationConsumer.consumeStockReservationResult(event);

        Mockito.verify(stripePaymentService, Mockito.never()).charge(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(paymentNotificationProducer, Mockito.never()).sendNotification(Mockito.any(PaymentNotificationRequestDTO.class));
    }
}
