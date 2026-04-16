package com.rajitha.ecommerce.enums;

import com.rajitha.ecommerce.service.EmailService;
import lombok.Getter;

public enum EmailTemplates {
    PAYMENT_CONFORMATIONS("payment-conformation.html","payment successfully processed"),
    ORDER_CONFORMATIONS("order-conformation.html","order successfully processed");

    @Getter
    private final String template;
    @Getter
    private final String subject;

     EmailTemplates(String template , String subject){
         this.template = template;
         this.subject = subject;
     }
}
