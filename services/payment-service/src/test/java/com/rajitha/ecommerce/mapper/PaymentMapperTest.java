package com.rajitha.ecommerce.mapper;

import com.rajitha.ecommerce.dto.AddressDTO;
import com.rajitha.ecommerce.dto.CustomerDTO;
import com.rajitha.ecommerce.dto.PaymentRequestDTO;
import com.rajitha.ecommerce.entity.Payment;
import com.rajitha.ecommerce.enums.PaymentMethode;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import scala.reflect.internal.settings.AbsSettings;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;


class PaymentMapperTest {

    private PaymentMapper paymentMapper ;

    @BeforeEach
    void setUp() {
        paymentMapper = new PaymentMapper();
            }

            @Test
    public void shouldMapToPayment(){

                PaymentRequestDTO paymentRequestDTO = PaymentRequestDTO.builder()
                        .amount(new BigDecimal("12"))
                        .paymentMethode(PaymentMethode.BITCOIN)
                        .orderId(12)
                        .orderReference("reference")
                        .customer(CustomerDTO.builder()
                                .id("id-1")
                                .firstName("first name")
                                .lastName("last name")
                                .email("rajithaubandara@gmail.com")
                                .address(AddressDTO.builder()
                                        .street("street")
                                        .houseNumber("123")
                                        .zipCode("123456+")
                                        .build()
                                )
                                .build())
                        .build();



                Payment payment = paymentMapper.toPayment(paymentRequestDTO);

                Assertions.assertNotNull(payment);
                Assertions.assertEquals(payment.getPaymentMethode(), PaymentMethode.BITCOIN);
                Assertions.assertEquals(payment.getOrderId(), 12);


            }

}