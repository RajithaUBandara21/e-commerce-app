package com.rajitha.ecommerce.messaging.consumer;

import com.rajitha.ecommerce.document.Notification;
import com.rajitha.ecommerce.dto.OrderConfirmationDTO;
import com.rajitha.ecommerce.dto.PaymentConfirmationDTO;
import com.rajitha.ecommerce.repository.NotificationRepository;
import com.rajitha.ecommerce.service.EmailService;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static com.rajitha.ecommerce.enums.NotificationType.ORDER_CONFORMATION;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {
    private final NotificationRepository notificationRepository;
    private final EmailService emailService;

    //private final EmailService emailService;
@KafkaListener(topics = "payment-topic")
    public void consumePaymentSuccessNotification(PaymentConfirmationDTO paymentConfirmationDTO) throws MessagingException {
    log.info("Received Payment Confirmation(consuming the message form payment-topic ):: <{}>", paymentConfirmationDTO);
    notificationRepository.save(Notification.builder()
                    .notificationType(ORDER_CONFORMATION)
                    .notificationDate(LocalDateTime.now())
                    .paymentConfirmationDTO(paymentConfirmationDTO)
                    .build());

    //Send Email
    var customerName = paymentConfirmationDTO.customerFirstName() + " " + paymentConfirmationDTO.customerLastName();
    emailService.sendPaymentSuccessEmail(
            paymentConfirmationDTO.customerEmail(),
            customerName,
            paymentConfirmationDTO.amount(),
            paymentConfirmationDTO.orderReference()
    );
                    }


    @KafkaListener(topics = "order-topic")
    public void consumeOrderConformationNotification(OrderConfirmationDTO orderConfirmationDTO) throws MessagingException {
    log.info("Received payment conformation(consuming message from order-topic ):: <{}>", orderConfirmationDTO);
    notificationRepository.save(Notification.builder()
                    .notificationType(ORDER_CONFORMATION)
                    .notificationDate(LocalDateTime.now())
                    .orderConfirmationDTO(orderConfirmationDTO)
                    .build());

    //Send Email
        var customerName = orderConfirmationDTO.customer().firstName() + " " + orderConfirmationDTO.customer().lastName();
        emailService.sendPaymentSuccessEmail(
                orderConfirmationDTO.customer().email(),
                customerName,
                orderConfirmationDTO.totalAmount(),
                orderConfirmationDTO.orderReference()

        );


}
}
