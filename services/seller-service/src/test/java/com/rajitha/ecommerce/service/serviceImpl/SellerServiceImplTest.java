package com.rajitha.ecommerce.service.serviceImpl;

import com.rajitha.ecommerce.client.keycloak.KeycloakRoleClient;
import com.rajitha.ecommerce.dto.SellerRegistrationRequestDTO;
import com.rajitha.ecommerce.entity.Seller;
import com.rajitha.ecommerce.enums.SellerStatus;
import com.rajitha.ecommerce.exception.SellerAlreadyExistsException;
import com.rajitha.ecommerce.exception.SellerNotFoundException;
import com.rajitha.ecommerce.mapper.SellerMapper;
import com.rajitha.ecommerce.repository.SellerRepository;
import com.rajitha.ecommerce.service.StripeConnectService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SellerServiceImplTest {

    @Mock
    private SellerRepository sellerRepository;
    @Mock
    private KeycloakRoleClient keycloakRoleClient;
    @Mock
    private StripeConnectService stripeConnectService;

    private final SellerMapper sellerMapper = new SellerMapper();

    private SellerServiceImpl sellerService;

    @Test
    void shouldRegisterNewSeller() {
        var requestDTO = SellerRegistrationRequestDTO.builder()
                .businessName("Acme Apparel")
                .businessEmail("acme@example.com")
                .description("Handmade clothing")
                .build();

        when(sellerRepository.findByKeycloakUserId("user-1")).thenReturn(Optional.empty());
        when(sellerRepository.save(any(Seller.class))).thenAnswer(invocation -> {
            Seller saved = invocation.getArgument(0);
            saved.setId(1);
            return saved;
        });

        sellerService = new SellerServiceImpl(sellerRepository, sellerMapper, keycloakRoleClient, stripeConnectService);

        var response = sellerService.register(requestDTO, "user-1");

        Assertions.assertEquals(1, response.id());
        Assertions.assertEquals("user-1", response.keycloakUserId());
        Assertions.assertEquals(SellerStatus.PENDING, response.status());
        verifyNoInteractions(keycloakRoleClient);
    }

    @Test
    void shouldRejectDuplicateRegistration() {
        var requestDTO = SellerRegistrationRequestDTO.builder()
                .businessName("Acme Apparel")
                .businessEmail("acme@example.com")
                .build();

        when(sellerRepository.findByKeycloakUserId("user-1"))
                .thenReturn(Optional.of(Seller.builder().id(1).keycloakUserId("user-1").build()));

        sellerService = new SellerServiceImpl(sellerRepository, sellerMapper, keycloakRoleClient, stripeConnectService);

        Assertions.assertThrows(SellerAlreadyExistsException.class,
                () -> sellerService.register(requestDTO, "user-1"));
    }

    @Test
    void shouldGrantKeycloakRoleOnActivation() {
        var seller = Seller.builder()
                .id(1)
                .keycloakUserId("user-1")
                .status(SellerStatus.PENDING)
                .build();

        when(sellerRepository.findById(1)).thenReturn(Optional.of(seller));
        when(sellerRepository.save(any(Seller.class))).thenAnswer(invocation -> invocation.getArgument(0));

        sellerService = new SellerServiceImpl(sellerRepository, sellerMapper, keycloakRoleClient, stripeConnectService);

        var response = sellerService.updateStatus(1, SellerStatus.ACTIVE);

        Assertions.assertEquals(SellerStatus.ACTIVE, response.status());
        verify(keycloakRoleClient).grantSellerRole("user-1");
        verify(keycloakRoleClient, never()).revokeSellerRole(any());
    }

    @Test
    void shouldRevokeKeycloakRoleOnSuspension() {
        var seller = Seller.builder()
                .id(1)
                .keycloakUserId("user-1")
                .status(SellerStatus.ACTIVE)
                .build();

        when(sellerRepository.findById(1)).thenReturn(Optional.of(seller));
        when(sellerRepository.save(any(Seller.class))).thenAnswer(invocation -> invocation.getArgument(0));

        sellerService = new SellerServiceImpl(sellerRepository, sellerMapper, keycloakRoleClient, stripeConnectService);

        var response = sellerService.updateStatus(1, SellerStatus.SUSPENDED);

        Assertions.assertEquals(SellerStatus.SUSPENDED, response.status());
        verify(keycloakRoleClient).revokeSellerRole("user-1");
        verify(keycloakRoleClient, never()).grantSellerRole(any());
    }

    @Test
    void shouldThrowWhenUpdatingStatusOfUnknownSeller() {
        when(sellerRepository.findById(99)).thenReturn(Optional.empty());
        sellerService = new SellerServiceImpl(sellerRepository, sellerMapper, keycloakRoleClient, stripeConnectService);

        Assertions.assertThrows(SellerNotFoundException.class,
                () -> sellerService.updateStatus(99, SellerStatus.ACTIVE));
    }

    @Test
    void shouldCreateStripeAccountOnFirstOnboardingLinkRequest() {
        var seller = Seller.builder().id(1).keycloakUserId("user-1").businessEmail("acme@example.com").build();

        when(sellerRepository.findByKeycloakUserId("user-1")).thenReturn(Optional.of(seller));
        when(stripeConnectService.createExpressAccount("acme@example.com")).thenReturn("acct_123");
        when(stripeConnectService.createAccountLink(eq("acct_123"), any(), any())).thenReturn("https://connect.stripe.com/setup/acct_123");

        sellerService = new SellerServiceImpl(sellerRepository, sellerMapper, keycloakRoleClient, stripeConnectService);

        var response = sellerService.createOnboardingLink("user-1");

        Assertions.assertEquals("https://connect.stripe.com/setup/acct_123", response.url());
        Assertions.assertEquals("acct_123", seller.getStripeAccountId());
        verify(sellerRepository).save(seller);
    }

    @Test
    void shouldReuseExistingStripeAccountOnSubsequentOnboardingLinkRequest() {
        var seller = Seller.builder().id(1).keycloakUserId("user-1").stripeAccountId("acct_existing").build();

        when(sellerRepository.findByKeycloakUserId("user-1")).thenReturn(Optional.of(seller));
        when(stripeConnectService.createAccountLink(eq("acct_existing"), any(), any())).thenReturn("https://connect.stripe.com/setup/acct_existing");

        sellerService = new SellerServiceImpl(sellerRepository, sellerMapper, keycloakRoleClient, stripeConnectService);

        sellerService.createOnboardingLink("user-1");

        verify(stripeConnectService, never()).createExpressAccount(any());
        verify(sellerRepository, never()).save(any(Seller.class));
    }

    @Test
    void shouldUpdateChargesAndPayoutsEnabledFromStripeWebhook() {
        var seller = Seller.builder().id(1).keycloakUserId("user-1").stripeAccountId("acct_123").build();

        when(sellerRepository.findByStripeAccountId("acct_123")).thenReturn(Optional.of(seller));

        sellerService = new SellerServiceImpl(sellerRepository, sellerMapper, keycloakRoleClient, stripeConnectService);

        sellerService.handleStripeAccountUpdated("acct_123", true, true);

        Assertions.assertTrue(seller.isChargesEnabled());
        Assertions.assertTrue(seller.isPayoutsEnabled());
        verify(sellerRepository).save(seller);
    }
}
