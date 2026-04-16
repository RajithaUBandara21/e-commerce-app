package com.rajitha.ecommerce.document;

import com.rajitha.ecommerce.dto.OrderConfirmationDTO;
import com.rajitha.ecommerce.dto.PaymentConfirmationDTO;
import com.rajitha.ecommerce.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Document
public class Notification {
    @Id
    private String id;
    private NotificationType notificationType;
    private LocalDateTime notificationDate;
    private OrderConfirmationDTO orderConfirmationDTO;
    private PaymentConfirmationDTO paymentConfirmationDTO;

}
