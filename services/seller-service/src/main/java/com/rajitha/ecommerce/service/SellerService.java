package com.rajitha.ecommerce.service;

import com.rajitha.ecommerce.dto.SellerRegistrationRequestDTO;
import com.rajitha.ecommerce.dto.SellerResponseDTO;
import com.rajitha.ecommerce.dto.StripeOnboardingLinkResponseDTO;
import com.rajitha.ecommerce.enums.SellerStatus;

import java.util.List;

public interface SellerService {

    SellerResponseDTO register(SellerRegistrationRequestDTO requestDTO, String keycloakUserId);

    SellerResponseDTO findByKeycloakUserId(String keycloakUserId);

    SellerResponseDTO updateStatus(Integer sellerId, SellerStatus status);

    List<SellerResponseDTO> findAll();

    StripeOnboardingLinkResponseDTO createOnboardingLink(String keycloakUserId);

    void handleStripeAccountUpdated(String stripeAccountId, boolean chargesEnabled, boolean payoutsEnabled);
}
