package com.rajitha.ecommerce.service.serviceImpl;

import com.rajitha.ecommerce.entity.Coupon;
import com.rajitha.ecommerce.enums.CouponType;
import com.rajitha.ecommerce.exception.CouponInvalidException;
import com.rajitha.ecommerce.mapper.CouponMapper;
import com.rajitha.ecommerce.repository.CouponRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class CouponServiceImplTest {

    @InjectMocks
    private CouponServiceImpl couponService;

    @Mock
    private CouponRepository couponRepository;
    @Mock
    private CouponMapper couponMapper;

    @Test
    void shouldApplyPercentDiscount() {
        var coupon = Coupon.builder().code("SAVE10").type(CouponType.PERCENT).value(new BigDecimal("10")).active(true).build();
        Mockito.when(couponRepository.findByCode("SAVE10")).thenReturn(Optional.of(coupon));

        var result = couponService.applyDiscount("SAVE10", new BigDecimal("100.00"));

        Assertions.assertEquals(new BigDecimal("90.00"), result);
    }

    @Test
    void shouldApplyFlatAmountDiscount() {
        var coupon = Coupon.builder().code("FLAT5").type(CouponType.AMOUNT).value(new BigDecimal("5.00")).active(true).build();
        Mockito.when(couponRepository.findByCode("FLAT5")).thenReturn(Optional.of(coupon));

        var result = couponService.applyDiscount("FLAT5", new BigDecimal("20.00"));

        Assertions.assertEquals(new BigDecimal("15.00"), result);
    }

    @Test
    void shouldNeverDiscountBelowZero() {
        var coupon = Coupon.builder().code("FLAT50").type(CouponType.AMOUNT).value(new BigDecimal("50.00")).active(true).build();
        Mockito.when(couponRepository.findByCode("FLAT50")).thenReturn(Optional.of(coupon));

        var result = couponService.applyDiscount("FLAT50", new BigDecimal("20.00"));

        Assertions.assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void shouldRejectUnknownCode() {
        Mockito.when(couponRepository.findByCode("MISSING")).thenReturn(Optional.empty());

        Assertions.assertThrows(CouponInvalidException.class,
                () -> couponService.applyDiscount("MISSING", new BigDecimal("10")));
    }

    @Test
    void shouldRejectInactiveCoupon() {
        var coupon = Coupon.builder().code("OLD").type(CouponType.PERCENT).value(new BigDecimal("10")).active(false).build();
        Mockito.when(couponRepository.findByCode("OLD")).thenReturn(Optional.of(coupon));

        Assertions.assertThrows(CouponInvalidException.class,
                () -> couponService.applyDiscount("OLD", new BigDecimal("10")));
    }

    @Test
    void shouldRejectExpiredCoupon() {
        var coupon = Coupon.builder().code("EXPIRED").type(CouponType.PERCENT).value(new BigDecimal("10"))
                .active(true).expiresAt(LocalDateTime.now().minusDays(1)).build();
        Mockito.when(couponRepository.findByCode("EXPIRED")).thenReturn(Optional.of(coupon));

        Assertions.assertThrows(CouponInvalidException.class,
                () -> couponService.applyDiscount("EXPIRED", new BigDecimal("10")));
    }

    @Test
    void shouldRejectWhenBelowMinimumOrderAmount() {
        var coupon = Coupon.builder().code("BIGORDER").type(CouponType.PERCENT).value(new BigDecimal("10"))
                .active(true).minOrderAmount(new BigDecimal("100")).build();
        Mockito.when(couponRepository.findByCode("BIGORDER")).thenReturn(Optional.of(coupon));

        Assertions.assertThrows(CouponInvalidException.class,
                () -> couponService.applyDiscount("BIGORDER", new BigDecimal("50")));
    }

    @Test
    void shouldAllowWhenMinimumOrderAmountMet() {
        var coupon = Coupon.builder().code("BIGORDER").type(CouponType.PERCENT).value(new BigDecimal("10"))
                .active(true).minOrderAmount(new BigDecimal("100")).build();
        Mockito.when(couponRepository.findByCode("BIGORDER")).thenReturn(Optional.of(coupon));

        var result = couponService.applyDiscount("BIGORDER", new BigDecimal("100"));

        Assertions.assertEquals(new BigDecimal("90.00"), result);
    }
}
