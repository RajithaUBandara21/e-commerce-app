package com.rajitha.ecommerce.repository;


import com.rajitha.ecommerce.entity.OrderLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderLineRepository extends JpaRepository<OrderLine, Integer> {


    List<OrderLine> findOrderLinesByOrderId(Integer order_id);
}
