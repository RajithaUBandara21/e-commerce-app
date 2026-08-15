package com.rajitha.ecommerce.entity;

import com.rajitha.ecommerce.enums.SellerStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

import static jakarta.persistence.EnumType.STRING;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "seller")
public class Seller {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // The Keycloak subject (JWT "sub") of the user who registered as a seller.
    // Threaded through product-service/order-service as a plain string, same as
    // Order.customerId — never a cross-service JPA relation.
    @Column(unique = true, nullable = false)
    private String keycloakUserId;

    private String businessName;
    private String businessEmail;
    private String description;

    @Enumerated(STRING)
    @Builder.Default
    private SellerStatus status = SellerStatus.PENDING;

    private String stripeAccountId;

    @Builder.Default
    private boolean chargesEnabled = false;

    @Builder.Default
    private boolean payoutsEnabled = false;

    @CreatedDate
    @Column(updatable = false, nullable = false)
    private LocalDateTime createdDate;

    @LastModifiedDate
    @Column(insertable = false)
    private LocalDateTime lastModifiedDate;
}
