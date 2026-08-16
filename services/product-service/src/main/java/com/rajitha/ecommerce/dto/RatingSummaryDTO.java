package com.rajitha.ecommerce.dto;

public record RatingSummaryDTO(Double averageRating, int reviewCount) {
    public static final RatingSummaryDTO EMPTY = new RatingSummaryDTO(null, 0);
}
