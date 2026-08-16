package com.rajitha.ecommerce.entity;

import com.rajitha.ecommerce.enums.PayoutStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static jakarta.persistence.EnumType.STRING;

// One row per seller per order — the "separate charges and transfers" ledger.
// The platform already charged the customer once (Payment, above); this is what
// the platform in turn owes each seller for their share of that one charge, minus
// commission. Settlement (the actual Stripe Transfer) is a separate step — see
// SellerPayoutServiceImpl.settlePendingPayouts.
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "seller_payout")
public class SellerPayout {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String sellerId;
    private String orderReference;

    private BigDecimal grossAmount;
    private BigDecimal commissionAmount;
    private BigDecimal netAmount;

    @Enumerated(STRING)
    @Builder.Default
    private PayoutStatus status = PayoutStatus.PENDING;

    private String stripeTransferId;
    private String failureReason;

    @CreatedDate
    @Column(updatable = false, nullable = false)
    private LocalDateTime createdDate;

    @LastModifiedDate
    @Column(insertable = false)
    private LocalDateTime lastModifiedDate;
}
