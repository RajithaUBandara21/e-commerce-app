package com.rajitha.ecommerce.mapper;

import com.rajitha.ecommerce.dto.PaymentRequestDTO;
import com.rajitha.ecommerce.entity.Payment;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper { 


    public Payment toPayment(PaymentRequestDTO paymentRequestDTO) {
        return Payment.builder()
                .id(paymentRequestDTO.id())
                .paymentMethode(paymentRequestDTO.paymentMethode())
                .amount(paymentRequestDTO.amount())
                .orderId(paymentRequestDTO.orderId())
                .build();
    }
}
