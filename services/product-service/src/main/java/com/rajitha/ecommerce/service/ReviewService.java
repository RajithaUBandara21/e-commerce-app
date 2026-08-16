package com.rajitha.ecommerce.service;

import com.rajitha.ecommerce.dto.RatingSummaryDTO;
import com.rajitha.ecommerce.dto.ReviewRequestDTO;
import com.rajitha.ecommerce.dto.ReviewResponseDTO;

import java.util.List;

public interface ReviewService {
    ReviewResponseDTO createReview(Integer productId, String customerId, ReviewRequestDTO request);

    List<ReviewResponseDTO> findByProductId(Integer productId);

    void deleteReview(Integer productId, Integer reviewId, String callerId, boolean isAdmin);

    RatingSummaryDTO getRatingSummary(Integer productId);
}
