package com.rajitha.ecommerce.service;

import com.rajitha.ecommerce.dto.PurchaseResponseDTO;
import com.rajitha.ecommerce.dto.SellerPayoutResponseDTO;

import java.util.List;

public interface SellerPayoutService {

    void recordPayoutsForOrder(String orderReference, List<PurchaseResponseDTO> products);

    List<SellerPayoutResponseDTO> findBySellerId(String sellerId);

    List<SellerPayoutResponseDTO> findAll();

    // Actually moves money (Stripe Transfer) for every PENDING payout whose seller
    // has payouts enabled. Called both by the daily schedule and the admin-triggered
    // endpoint — same method, so there's exactly one settlement code path either way.
    void settlePendingPayouts();
}
