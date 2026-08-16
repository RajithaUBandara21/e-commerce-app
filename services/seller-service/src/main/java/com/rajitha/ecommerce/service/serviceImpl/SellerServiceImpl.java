package com.rajitha.ecommerce.service.serviceImpl;

import com.rajitha.ecommerce.client.keycloak.KeycloakRoleClient;
import com.rajitha.ecommerce.dto.SellerRegistrationRequestDTO;
import com.rajitha.ecommerce.dto.SellerResponseDTO;
import com.rajitha.ecommerce.dto.StripeOnboardingLinkResponseDTO;
import com.rajitha.ecommerce.enums.SellerStatus;
import com.rajitha.ecommerce.exception.SellerAlreadyExistsException;
import com.rajitha.ecommerce.exception.SellerNotFoundException;
import com.rajitha.ecommerce.mapper.SellerMapper;
import com.rajitha.ecommerce.repository.SellerRepository;
import com.rajitha.ecommerce.service.SellerService;
import com.rajitha.ecommerce.service.StripeConnectService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SellerServiceImpl implements SellerService {

    private final SellerRepository sellerRepository;
    private final SellerMapper sellerMapper;
    private final KeycloakRoleClient keycloakRoleClient;
    private final StripeConnectService stripeConnectService;

    // Field initializer doubles as the default for plain-Java unit tests that
    // construct this class directly rather than through Spring's @Value resolution.
    @Value("${application.config.frontend-origin:http://localhost:3000}")
    private String frontendOrigin = "http://localhost:3000";

    @Override
    public SellerResponseDTO register(SellerRegistrationRequestDTO requestDTO, String keycloakUserId) {
        sellerRepository.findByKeycloakUserId(keycloakUserId).ifPresent(existing -> {
            throw new SellerAlreadyExistsException("A seller profile already exists for this account");
        });
        var seller = sellerMapper.toSellerEntity(requestDTO, keycloakUserId);
        return sellerMapper.toSellerResponseDTO(sellerRepository.save(seller));
    }

    @Override
    public SellerResponseDTO findByKeycloakUserId(String keycloakUserId) {
        return sellerRepository.findByKeycloakUserId(keycloakUserId)
                .map(sellerMapper::toSellerResponseDTO)
                .orElseThrow(() -> new SellerNotFoundException("No seller profile registered for this account"));
    }

    @Override
    @Transactional
    public SellerResponseDTO updateStatus(Integer sellerId, SellerStatus status) {
        var seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new SellerNotFoundException("Seller not found: " + sellerId));

        var previousStatus = seller.getStatus();
        seller.setStatus(status);

        // Keycloak role membership tracks ACTIVE status: granted on the transition
        // into ACTIVE, revoked on the transition out of it (SUSPENDED or back to
        // PENDING) — this is what actually gates seller-only gateway routes/product
        // mutations, not this service's own status field.
        if (status == SellerStatus.ACTIVE && previousStatus != SellerStatus.ACTIVE) {
            keycloakRoleClient.grantSellerRole(seller.getKeycloakUserId());
        } else if (status != SellerStatus.ACTIVE && previousStatus == SellerStatus.ACTIVE) {
            keycloakRoleClient.revokeSellerRole(seller.getKeycloakUserId());
        }

        return sellerMapper.toSellerResponseDTO(sellerRepository.save(seller));
    }

    @Override
    public List<SellerResponseDTO> findAll() {
        return sellerRepository.findAll().stream().map(sellerMapper::toSellerResponseDTO).toList();
    }

    @Override
    @Transactional
    public StripeOnboardingLinkResponseDTO createOnboardingLink(String keycloakUserId) {
        var seller = sellerRepository.findByKeycloakUserId(keycloakUserId)
                .orElseThrow(() -> new SellerNotFoundException("No seller profile registered for this account"));

        if (seller.getStripeAccountId() == null) {
            var stripeAccountId = stripeConnectService.createExpressAccount(seller.getBusinessEmail());
            seller.setStripeAccountId(stripeAccountId);
            sellerRepository.save(seller);
        }

        var refreshUrl = frontendOrigin + "/seller/onboarding/refresh";
        var returnUrl = frontendOrigin + "/seller/onboarding/complete";
        var url = stripeConnectService.createAccountLink(seller.getStripeAccountId(), refreshUrl, returnUrl);
        return new StripeOnboardingLinkResponseDTO(url);
    }

    @Override
    @Transactional
    public void handleStripeAccountUpdated(String stripeAccountId, boolean chargesEnabled, boolean payoutsEnabled) {
        sellerRepository.findByStripeAccountId(stripeAccountId).ifPresent(seller -> {
            seller.setChargesEnabled(chargesEnabled);
            seller.setPayoutsEnabled(payoutsEnabled);
            sellerRepository.save(seller);
        });
    }
}
