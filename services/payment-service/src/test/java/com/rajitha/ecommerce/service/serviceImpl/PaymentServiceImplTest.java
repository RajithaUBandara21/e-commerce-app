package com.rajitha.ecommerce.service.serviceImpl;
import com.rajitha.ecommerce.dto.CustomerDTO;
import com.rajitha.ecommerce.dto.PaymentRequestDTO;
import com.rajitha.ecommerce.entity.Payment;
import com.rajitha.ecommerce.enums.PaymentMethode;
import com.rajitha.ecommerce.mapper.PaymentMapper;
import com.rajitha.ecommerce.messaging.PaymentNotificationProducer;
import com.rajitha.ecommerce.repository.PaymentRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import java.math.BigDecimal;

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

    @BeforeEach
    void setUp() {

        paymentRepository = Mockito.mock(PaymentRepository.class);
        paymentMapper = Mockito.mock(PaymentMapper.class);
        paymentNotificationProducer = Mockito.mock(PaymentNotificationProducer.class);
        paymentServiceImpl = new PaymentServiceImpl(paymentRepository, paymentMapper, paymentNotificationProducer);
    }


    @Test
            public void shouldCreatePayment() {
            PaymentRequestDTO paymentRequestDTO  = PaymentRequestDTO.builder()
                .amount(new BigDecimal("265"))
                .paymentMethode(PaymentMethode.BITCOIN)
                .orderId(1)
                .orderReference(null)
                .customer(CustomerDTO.builder()
                        .id("1-id")
                        .firstName("firstName")
                        .build())
                .build();

        Payment payment = Payment.builder()
                .orderId(123)

                .build();

            Mockito.when(paymentMapper.toPayment(paymentRequestDTO)).thenReturn(payment);
            Mockito.when(paymentRepository.save(payment)).thenReturn(payment);

        Integer Id = paymentServiceImpl.createPayment(paymentRequestDTO);

        Assertions.assertEquals(payment.getId(), Id);
        Mockito.verify(paymentRepository, Mockito.times(1)).save(payment);
        Mockito.verify(paymentMapper).toPayment(paymentRequestDTO);

    }


}