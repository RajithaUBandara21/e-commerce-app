package com.rajitha.ecommerce.service;

import com.rajitha.ecommerce.dto.CouponRequestDTO;
import com.rajitha.ecommerce.dto.CouponResponseDTO;

import java.math.BigDecimal;
import java.util.List;

public interface CouponService {

    Integer createCoupon(CouponRequestDTO requestDTO);

    void updateCoupon(Integer couponId, CouponRequestDTO requestDTO);

    void deleteCoupon(Integer couponId);

    CouponResponseDTO findById(Integer couponId);

    List<CouponResponseDTO> findAll();

    // Validates the code (exists, active, not expired, minimum met) and returns
    // totalAmount with the discount applied — never below zero. Throws
    // CouponInvalidException with a user-facing reason on any failure.
    BigDecimal applyDiscount(String code, BigDecimal totalAmount);
}
