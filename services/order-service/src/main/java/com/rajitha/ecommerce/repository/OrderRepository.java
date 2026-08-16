package com.rajitha.ecommerce.repository;

import com.rajitha.ecommerce.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order,Integer> {
    Optional<Order> findByIdempotencyKey(String idempotencyKey);
    Optional<Order> findByReference(String reference);
    List<Order> findByCustomerId(String customerId);
}
