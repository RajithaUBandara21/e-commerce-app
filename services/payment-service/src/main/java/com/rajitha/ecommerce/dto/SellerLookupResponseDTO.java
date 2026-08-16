package com.rajitha.ecommerce.dto;

// Payment-service's own copy of the fields it actually needs from seller-service's
// SellerResponseDTO — just enough to decide whether/where a payout can be settled.
public record SellerLookupResponseDTO(
        Integer id,
        String keycloakUserId,
        String status,
        String stripeAccountId,
        boolean chargesEnabled,
        boolean payoutsEnabled
) {
}
