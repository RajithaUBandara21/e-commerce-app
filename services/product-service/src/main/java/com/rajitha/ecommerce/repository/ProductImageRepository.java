package com.rajitha.ecommerce.repository;

import com.rajitha.ecommerce.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductImageRepository extends JpaRepository<ProductImage, Integer> {
    List<ProductImage> findByProduct_IdOrderByPosition(Integer productId);

    int countByProduct_Id(Integer productId);
}
