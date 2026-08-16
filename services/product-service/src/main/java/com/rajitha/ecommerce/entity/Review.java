package com.rajitha.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "customerId"}))
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    // Keycloak subject, same convention as Order.customerId — not a cross-service FK.
    private String customerId;

    private int rating;

    @Column(length = 2000)
    private String comment;

    @CreatedDate
    @Column(updatable = false, nullable = false)
    private LocalDateTime createdDate;
}
