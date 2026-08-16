package com.rajitha.ecommerce.entity;

import com.rajitha.ecommerce.enums.PaymentMethode;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@Validated
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "payment")
public class Payment {
    @Id
    @GeneratedValue
    private Integer id;
    private BigDecimal amount;
    @Enumerated(EnumType.STRING)
    private PaymentMethode paymentMethode;
    private String orderReference;
    // Needed to ever issue a Stripe refund against this charge — was not persisted
    // before Phase 8 even though StripePaymentService.ChargeResult already carried it.
    private String stripePaymentIntentId;
    @Builder.Default
    private boolean refunded = false;
    @CreatedDate
    @Column(updatable = false, nullable = false)
    private LocalDateTime createdDate;
    @LastModifiedDate
    @Column(insertable = false)
    private LocalDateTime lastModifiedDate;
}
