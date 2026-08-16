package com.rajitha.ecommerce.enums;

// Per-line fulfillment state — separate from Order.status because a multi-vendor
// order can have lines from different sellers shipping independently. Order.status
// (SHIPPED/DELIVERED) is derived from these, not set directly by a seller action.
public enum OrderLineStatus {
    PENDING,
    SHIPPED,
    DELIVERED
}
