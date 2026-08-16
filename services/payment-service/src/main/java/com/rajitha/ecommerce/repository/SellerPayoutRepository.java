package com.rajitha.ecommerce.repository;

import com.rajitha.ecommerce.entity.SellerPayout;
import com.rajitha.ecommerce.enums.PayoutStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SellerPayoutRepository extends JpaRepository<SellerPayout, Integer> {
    List<SellerPayout> findBySellerId(String sellerId);
    List<SellerPayout> findByStatus(PayoutStatus status);
}
