package com.rajitha.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@Entity
public class ProductImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    // MinIO object key — the public URL is derived from this (minio.public-base-url
    // + "/" + bucket + "/" + objectKey), not stored redundantly.
    private String objectKey;

    // Display order on the PDP; the first (lowest position) is the card/gallery thumbnail.
    private int position;
}
