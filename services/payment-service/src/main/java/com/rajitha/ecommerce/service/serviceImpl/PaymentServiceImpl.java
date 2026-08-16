package com.rajitha.ecommerce.service.serviceImpl;
import com.rajitha.ecommerce.dto.PaymentNotificationRequestDTO;
import com.rajitha.ecommerce.dto.PaymentRefundResponseDTO;
import com.rajitha.ecommerce.dto.PaymentRequestDTO;
import com.rajitha.ecommerce.mapper.PaymentMapper;
import com.rajitha.ecommerce.messaging.PaymentNotificationProducer;
import com.rajitha.ecommerce.service.PaymentService;
import com.rajitha.ecommerce.service.StripePaymentService;
import com.rajitha.ecommerce.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@RequiredArgsConstructor
@Service
@Slf4j
public class PaymentServiceImpl implements PaymentService {

 private final PaymentRepository paymentRepository;
 private final PaymentMapper paymentMapper;
 private final PaymentNotificationProducer paymentNotificationProducer;
 private final StripePaymentService stripePaymentService;
    @Override
    public Integer createPayment(PaymentRequestDTO paymentRequestDTO) {


        var payment = paymentRepository.save(paymentMapper.toPayment(paymentRequestDTO));

        paymentNotificationProducer.sendNotification(
                PaymentNotificationRequestDTO.builder()
                        .orderReference(paymentRequestDTO.orderReference())
                        .amount(paymentRequestDTO.amount())
                        .paymentMethode(paymentRequestDTO.paymentMethode())
                        .success(true)
                        .customerEmail(paymentRequestDTO.customer().email())
                        .customerFirstName(paymentRequestDTO.customer().firstName())
                        .customerLastName(paymentRequestDTO.customer().lastName())
                        .build()
        );
        return payment.getId();
    }

    @Override
    public PaymentRefundResponseDTO refundByOrderReference(String orderReference) {
        var payment = paymentRepository.findByOrderReference(orderReference)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No payment found for order reference " + orderReference));

        if (payment.isRefunded()) {
            return new PaymentRefundResponseDTO(true, "Already refunded");
        }

        var result = stripePaymentService.refund(payment.getStripePaymentIntentId());
        if (result.success()) {
            payment.setRefunded(true);
            paymentRepository.save(payment);
            log.info("Refunded payment for order {} :: stripeRefundId={}", orderReference, result.stripeRefundId());
            return new PaymentRefundResponseDTO(true, null);
        }

        log.warn("Refund failed for order {} :: {}", orderReference, result.failureReason());
        return new PaymentRefundResponseDTO(false, result.failureReason());
    }
}
