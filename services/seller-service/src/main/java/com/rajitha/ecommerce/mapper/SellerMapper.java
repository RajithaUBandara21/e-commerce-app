package com.rajitha.ecommerce.mapper;

import com.rajitha.ecommerce.dto.SellerRegistrationRequestDTO;
import com.rajitha.ecommerce.dto.SellerResponseDTO;
import com.rajitha.ecommerce.entity.Seller;
import com.rajitha.ecommerce.enums.SellerStatus;
import org.springframework.stereotype.Component;

@Component
public class SellerMapper {

    public Seller toSellerEntity(SellerRegistrationRequestDTO requestDTO, String keycloakUserId) {
        return Seller.builder()
                .keycloakUserId(keycloakUserId)
                .businessName(requestDTO.businessName())
                .businessEmail(requestDTO.businessEmail())
                .description(requestDTO.description())
                .status(SellerStatus.PENDING)
                .build();
    }

    public SellerResponseDTO toSellerResponseDTO(Seller seller) {
        return SellerResponseDTO.builder()
                .id(seller.getId())
                .keycloakUserId(seller.getKeycloakUserId())
                .businessName(seller.getBusinessName())
                .businessEmail(seller.getBusinessEmail())
                .description(seller.getDescription())
                .status(seller.getStatus())
                .stripeAccountId(seller.getStripeAccountId())
                .chargesEnabled(seller.isChargesEnabled())
                .payoutsEnabled(seller.isPayoutsEnabled())
                .createdDate(seller.getCreatedDate())
                .build();
    }
}
