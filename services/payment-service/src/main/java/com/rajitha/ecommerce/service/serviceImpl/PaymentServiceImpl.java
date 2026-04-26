package com.rajitha.ecommerce.service.serviceImpl;
import com.rajitha.ecommerce.dto.PaymentNotificationRequestDTO;
import com.rajitha.ecommerce.dto.PaymentRequestDTO;
import com.rajitha.ecommerce.mapper.PaymentMapper;
import com.rajitha.ecommerce.messaging.PaymentNotificationProducer;
import com.rajitha.ecommerce.service.PaymentService;
import com.rajitha.ecommerce.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class PaymentServiceImpl implements PaymentService {

 private final PaymentRepository paymentRepository;
 private final PaymentMapper paymentMapper;
 private final PaymentNotificationProducer paymentNotificationProducer;
    @Override
    public Integer createPayment(PaymentRequestDTO paymentRequestDTO) {


        var payment = paymentRepository.save(paymentMapper.toPayment(paymentRequestDTO));

        paymentNotificationProducer.sendNotification(
                PaymentNotificationRequestDTO.builder()
                        .orderReference(paymentRequestDTO.orderReference())
                        .amount(paymentRequestDTO.amount())
                        .paymentMethode(paymentRequestDTO.paymentMethode())
                        .customerEmail(paymentRequestDTO.customer().email())
                        .customerFirstName(paymentRequestDTO.customer().firstName())
                        .customerLastName(paymentRequestDTO.customer().lastName())
                        .build()
        );
        return payment.getId();
    }
}
