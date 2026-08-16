package com.rajitha.ecommerce.repository;


import com.rajitha.ecommerce.entity.OrderLine;
import com.rajitha.ecommerce.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderLineRepository extends JpaRepository<OrderLine, Integer> {


    List<OrderLine> findOrderLinesByOrderId(Integer order_id);

    List<OrderLine> findBySellerId(String sellerId);

    List<OrderLine> findByOrder_CustomerIdAndOrder_StatusIn(String customerId, List<OrderStatus> statuses);
}
