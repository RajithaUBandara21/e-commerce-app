package com.rajitha.ecommerce.mapper;

import com.rajitha.ecommerce.dto.SellerPayoutResponseDTO;
import com.rajitha.ecommerce.entity.SellerPayout;
import org.springframework.stereotype.Component;

@Component
public class SellerPayoutMapper {

    public SellerPayoutResponseDTO toResponseDTO(SellerPayout payout) {
        return SellerPayoutResponseDTO.builder()
                .id(payout.getId())
                .sellerId(payout.getSellerId())
                .orderReference(payout.getOrderReference())
                .grossAmount(payout.getGrossAmount())
                .commissionAmount(payout.getCommissionAmount())
                .netAmount(payout.getNetAmount())
                .status(payout.getStatus())
                .stripeTransferId(payout.getStripeTransferId())
                .failureReason(payout.getFailureReason())
                .createdDate(payout.getCreatedDate())
                .build();
    }
}
