package com.rajitha.ecommerce.service.serviceImpl;

import com.rajitha.ecommerce.dto.CouponRequestDTO;
import com.rajitha.ecommerce.dto.CouponResponseDTO;
import com.rajitha.ecommerce.enums.CouponType;
import com.rajitha.ecommerce.exception.CouponAlreadyExistsException;
import com.rajitha.ecommerce.exception.CouponInvalidException;
import com.rajitha.ecommerce.exception.CouponNotFoundException;
import com.rajitha.ecommerce.mapper.CouponMapper;
import com.rajitha.ecommerce.repository.CouponRepository;
import com.rajitha.ecommerce.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;
    private final CouponMapper couponMapper;

    @Override
    public Integer createCoupon(CouponRequestDTO requestDTO) {
        couponRepository.findByCode(requestDTO.code()).ifPresent(existing -> {
            throw new CouponAlreadyExistsException("A coupon with code '" + requestDTO.code() + "' already exists");
        });
        return couponRepository.save(couponMapper.toCoupon(requestDTO)).getId();
    }

    @Override
    public void updateCoupon(Integer couponId, CouponRequestDTO requestDTO) {
        var coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new CouponNotFoundException("Coupon not found: " + couponId));

        coupon.setCode(requestDTO.code());
        coupon.setType(requestDTO.type());
        coupon.setValue(requestDTO.value());
        coupon.setMinOrderAmount(requestDTO.minOrderAmount());
        coupon.setExpiresAt(requestDTO.expiresAt());
        coupon.setActive(requestDTO.active() == null || requestDTO.active());
        couponRepository.save(coupon);
    }

    @Override
    public void deleteCoupon(Integer couponId) {
        if (!couponRepository.existsById(couponId)) {
            throw new CouponNotFoundException("Coupon not found: " + couponId);
        }
        couponRepository.deleteById(couponId);
    }

    @Override
    public CouponResponseDTO findById(Integer couponId) {
        return couponRepository.findById(couponId)
                .map(couponMapper::toResponseDTO)
                .orElseThrow(() -> new CouponNotFoundException("Coupon not found: " + couponId));
    }

    @Override
    public List<CouponResponseDTO> findAll() {
        return couponRepository.findAll().stream().map(couponMapper::toResponseDTO).collect(Collectors.toList());
    }

    @Override
    public BigDecimal applyDiscount(String code, BigDecimal totalAmount) {
        var coupon = couponRepository.findByCode(code)
                .orElseThrow(() -> new CouponInvalidException("Coupon code not found: " + code));

        if (!coupon.isActive()) {
            throw new CouponInvalidException("Coupon is no longer active: " + code);
        }
        if (coupon.getExpiresAt() != null && coupon.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new CouponInvalidException("Coupon has expired: " + code);
        }
        if (coupon.getMinOrderAmount() != null && totalAmount.compareTo(coupon.getMinOrderAmount()) < 0) {
            throw new CouponInvalidException(
                    "Order total must be at least " + coupon.getMinOrderAmount() + " to use coupon " + code);
        }

        var discount = coupon.getType() == CouponType.PERCENT
                ? totalAmount.multiply(coupon.getValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                : coupon.getValue();

        var discounted = totalAmount.subtract(discount);
        return discounted.max(BigDecimal.ZERO);
    }
}
