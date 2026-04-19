package com.rajitha.ecommerce.service.serviceImpl;
import com.rajitha.ecommerce.dto.ProductDTO;
import com.rajitha.ecommerce.dto.PurchaseResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import com.rajitha.ecommerce.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.thymeleaf.spring6.SpringTemplateEngine;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.rajitha.ecommerce.enums.EmailTemplates.ORDER_CONFORMATIONS;
import static com.rajitha.ecommerce.enums.EmailTemplates.PAYMENT_CONFORMATIONS;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Async
    @Override
    public void sendPaymentSuccessEmail(
            String destinationEmail,
            String customerName,
            BigDecimal amount,
            String orderReference) throws MessagingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage, MimeMessageHelper.MULTIPART_MODE_MIXED, StandardCharsets.UTF_8.name());
        messageHelper.setFrom("kgrubandara@gmail.com");

        final String templateName = PAYMENT_CONFORMATIONS.getTemplate();

        Map<String, Object> variables = new HashMap<>();
        variables.put("customerName", customerName);
        variables.put("amount", amount);
        variables.put("orderReference", orderReference);

        Context context = new Context();
        context.setVariables(variables);
        messageHelper.setSubject(PAYMENT_CONFORMATIONS.getSubject());

        try{
            String htmlTemplate = templateEngine.process(templateName, context);
            messageHelper.setText(htmlTemplate, true);
            messageHelper.setTo(destinationEmail);
            mailSender.send(mimeMessage);
            log.info(String.format("Email successfully sent to %s with template %s", destinationEmail , templateName));
        }catch (MessagingException e){
            log.warn("Warning - cannot send email to: " + destinationEmail );
        }

    }

    @Async
    @Override
    public void sendOrderConformationEmail(String destinationEmail, String customerName, BigDecimal amount, String orderReference, List<PurchaseResponseDTO> products) throws MessagingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage, MimeMessageHelper.MULTIPART_MODE_MIXED, StandardCharsets.UTF_8.name());
        messageHelper.setFrom("kgrubandara@gmail.com");

        final String templateName = ORDER_CONFORMATIONS.getTemplate();

        Map<String, Object> variables = new HashMap<>();
        variables.put("customerName", customerName);
        variables.put("totalAmount", amount);
        variables.put("orderReference", orderReference);
        variables.put("products", products);

        Context context = new Context();
        context.setVariables(variables);
        messageHelper.setSubject(ORDER_CONFORMATIONS.getSubject());

        try{
            String htmlTemplate = templateEngine.process(templateName, context);
            messageHelper.setText(htmlTemplate, true);
            messageHelper.setTo(destinationEmail);
            mailSender.send(mimeMessage);
            log.info(String.format("Email successfully sent to %s with template %s", destinationEmail , templateName));
        }catch (MessagingException e){
            log.warn("Warning - cannot send email to: " + destinationEmail );
        }

    }

}
