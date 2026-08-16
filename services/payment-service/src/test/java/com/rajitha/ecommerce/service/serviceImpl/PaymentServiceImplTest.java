package com.rajitha.ecommerce.service.serviceImpl;
import com.rajitha.ecommerce.dto.CustomerDTO;
import com.rajitha.ecommerce.dto.PaymentRequestDTO;
import com.rajitha.ecommerce.entity.Payment;
import com.rajitha.ecommerce.enums.PaymentMethode;
import com.rajitha.ecommerce.mapper.PaymentMapper;
import com.rajitha.ecommerce.messaging.PaymentNotificationProducer;
import com.rajitha.ecommerce.repository.PaymentRepository;
import com.rajitha.ecommerce.service.StripePaymentService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.Optional;

//@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {
//    @InjectMocks
    private PaymentServiceImpl paymentServiceImpl;
//    @Mock
    private PaymentRepository paymentRepository;
//    @Mock
    private PaymentMapper paymentMapper;
//    @Mock
    private PaymentNotificationProducer paymentNotificationProducer;
    private StripePaymentService stripePaymentService;

    @BeforeEach
    void setUp() {

        paymentRepository = Mockito.mock(PaymentRepository.class);
        paymentMapper = Mockito.mock(PaymentMapper.class);
        paymentNotificationProducer = Mockito.mock(PaymentNotificationProducer.class);
        stripePaymentService = Mockito.mock(StripePaymentService.class);
        paymentServiceImpl = new PaymentServiceImpl(paymentRepository, paymentMapper, paymentNotificationProducer, stripePaymentService);
    }


    @Test
            public void shouldCreatePayment() {
            PaymentRequestDTO paymentRequestDTO  = PaymentRequestDTO.builder()
                .amount(new BigDecimal("265"))
                .paymentMethode(PaymentMethode.BITCOIN)
                .orderReference("reference")
                .customer(CustomerDTO.builder()
                        .id("1-id")
                        .firstName("firstName")
                        .build())
                .build();

        Payment payment = Payment.builder()
                .orderReference("reference")

                .build();

            Mockito.when(paymentMapper.toPayment(paymentRequestDTO)).thenReturn(payment);
            Mockito.when(paymentRepository.save(payment)).thenReturn(payment);

        Integer Id = paymentServiceImpl.createPayment(paymentRequestDTO);

        Assertions.assertEquals(payment.getId(), Id);
        Mockito.verify(paymentRepository, Mockito.times(1)).save(payment);
        Mockito.verify(paymentMapper).toPayment(paymentRequestDTO);

    }

    @Test
    public void shouldRefundPaymentWhenStripeRefundSucceeds() {
        Payment payment = Payment.builder()
                .orderReference("reference")
                .stripePaymentIntentId("pi_123")
                .refunded(false)
                .build();

        Mockito.when(paymentRepository.findByOrderReference("reference")).thenReturn(Optional.of(payment));
        Mockito.when(stripePaymentService.refund("pi_123"))
                .thenReturn(StripePaymentService.RefundResult.success("re_123"));

        var response = paymentServiceImpl.refundByOrderReference("reference");

        Assertions.assertTrue(response.success());
        Assertions.assertTrue(payment.isRefunded());
        Mockito.verify(paymentRepository).save(payment);
    }

    @Test
    public void shouldReturnFailureWhenStripeRefundFails() {
        Payment payment = Payment.builder()
                .orderReference("reference")
                .stripePaymentIntentId("pi_123")
                .refunded(false)
                .build();

        Mockito.when(paymentRepository.findByOrderReference("reference")).thenReturn(Optional.of(payment));
        Mockito.when(stripePaymentService.refund("pi_123"))
                .thenReturn(StripePaymentService.RefundResult.failure("charge already refunded"));

        var response = paymentServiceImpl.refundByOrderReference("reference");

        Assertions.assertFalse(response.success());
        Assertions.assertEquals("charge already refunded", response.reason());
        Assertions.assertFalse(payment.isRefunded());
        Mockito.verify(paymentRepository, Mockito.never()).save(payment);
    }

    @Test
    public void shouldShortCircuitWhenAlreadyRefunded() {
        Payment payment = Payment.builder()
                .orderReference("reference")
                .stripePaymentIntentId("pi_123")
                .refunded(true)
                .build();

        Mockito.when(paymentRepository.findByOrderReference("reference")).thenReturn(Optional.of(payment));

        var response = paymentServiceImpl.refundByOrderReference("reference");

        Assertions.assertTrue(response.success());
        Mockito.verifyNoInteractions(stripePaymentService);
    }

    @Test
    public void shouldThrowWhenNoPaymentFoundForOrderReference() {
        Mockito.when(paymentRepository.findByOrderReference("missing")).thenReturn(Optional.empty());

        Assertions.assertThrows(ResponseStatusException.class,
                () -> paymentServiceImpl.refundByOrderReference("missing"));
    }
}