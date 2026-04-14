package com.rajitha.ecommerce.repository;

import com.rajitha.ecommerce.entity.OrderLine;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderLineRepository extends JpaRepository<OrderLine, Integer> {
}
