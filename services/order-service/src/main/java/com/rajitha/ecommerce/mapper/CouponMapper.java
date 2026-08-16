package com.rajitha.ecommerce.mapper;

import com.rajitha.ecommerce.dto.CouponRequestDTO;
import com.rajitha.ecommerce.dto.CouponResponseDTO;
import com.rajitha.ecommerce.entity.Coupon;
import org.springframework.stereotype.Component;

@Component
public class CouponMapper {

    public Coupon toCoupon(CouponRequestDTO requestDTO) {
        return Coupon.builder()
                .code(requestDTO.code())
                .type(requestDTO.type())
                .value(requestDTO.value())
                .minOrderAmount(requestDTO.minOrderAmount())
                .expiresAt(requestDTO.expiresAt())
                .active(requestDTO.active() == null || requestDTO.active())
                .build();
    }

    public CouponResponseDTO toResponseDTO(Coupon coupon) {
        return CouponResponseDTO.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .type(coupon.getType())
                .value(coupon.getValue())
                .minOrderAmount(coupon.getMinOrderAmount())
                .expiresAt(coupon.getExpiresAt())
                .active(coupon.isActive())
                .createdDate(coupon.getCreatedDate())
                .build();
    }
}
