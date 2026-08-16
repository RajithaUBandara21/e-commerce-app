package com.rajitha.ecommerce.entity;

import com.rajitha.ecommerce.enums.CouponType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static jakarta.persistence.EnumType.STRING;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@Entity
@EntityListeners(AuditingEntityListener.class)
public class Coupon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true, nullable = false)
    private String code;

    @Enumerated(STRING)
    private CouponType type;

    // PERCENT: 0-100. AMOUNT: a flat currency value.
    private BigDecimal value;

    // Null = no minimum.
    private BigDecimal minOrderAmount;

    // Null = never expires.
    private LocalDateTime expiresAt;

    @Builder.Default
    private boolean active = true;

    @CreatedDate
    @Column(updatable = false, nullable = false)
    private LocalDateTime createdDate;
}
