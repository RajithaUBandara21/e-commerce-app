package com.rajitha.ecommerce.repository;

import com.rajitha.ecommerce.entity.Seller;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SellerRepository extends JpaRepository<Seller, Integer> {
    Optional<Seller> findByKeycloakUserId(String keycloakUserId);
}
