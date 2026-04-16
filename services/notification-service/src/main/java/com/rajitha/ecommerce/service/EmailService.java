package com.rajitha.ecommerce.service;
import com.rajitha.ecommerce.dto.ProductDTO;
import jakarta.mail.MessagingException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public interface EmailService {
    @Async
    void sendPaymentSuccessEmail(String destinationEmail, String customerName, BigDecimal amount, String orderReference) throws MessagingException;

    @Async
    void sendOrderConformationEmail(String destinationEmail, String customerName, BigDecimal amount, String orderReference, List<ProductDTO> products) throws MessagingException;
}
