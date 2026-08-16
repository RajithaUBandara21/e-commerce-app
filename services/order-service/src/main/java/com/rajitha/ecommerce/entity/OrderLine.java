package com.rajitha.ecommerce.entity;

import com.rajitha.ecommerce.enums.OrderLineStatus;
import jakarta.persistence.*;
import lombok.*;

import static jakarta.persistence.EnumType.STRING;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@Entity
public class OrderLine {
    @jakarta.persistence.Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer Id;
    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;
    private Integer variantId;
    private double quantity;
    // Filled in once stock reservation resolves (product-service is the only
    // service that knows which seller owns a variant) — null between order
    // creation and that point, not a data-integrity issue.
    private String sellerId;

    @Enumerated(STRING)
    @Builder.Default
    private OrderLineStatus status = OrderLineStatus.PENDING;

    private String trackingNumber;
}
