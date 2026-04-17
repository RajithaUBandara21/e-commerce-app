package com.rajitha.ecommerce.service;
import com.rajitha.ecommerce.dto.PaymentRequestDTO;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;


public interface PaymentService {
    Integer createPayment(@Valid PaymentRequestDTO paymentRequestDTO);
}

