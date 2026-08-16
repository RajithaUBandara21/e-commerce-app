package com.rajitha.ecommerce.repository;

import com.rajitha.ecommerce.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Integer> {
    List<Review> findByProduct_IdOrderByCreatedDateDesc(Integer productId);

    boolean existsByProduct_IdAndCustomerId(Integer productId, String customerId);

    Optional<Review> findByProduct_IdAndCustomerId(Integer productId, String customerId);

    List<Review> findByProduct_Id(Integer productId);
}
