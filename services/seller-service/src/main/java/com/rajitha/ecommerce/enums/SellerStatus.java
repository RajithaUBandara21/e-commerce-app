package com.rajitha.ecommerce.enums;

// PENDING: registered, not yet approved to sell.
// ACTIVE: admin-approved; grantSellerRole() has run so the Keycloak "seller" role is live.
// SUSPENDED: previously ACTIVE, access revoked by an admin.
public enum SellerStatus {
    PENDING,
    ACTIVE,
    SUSPENDED
}
