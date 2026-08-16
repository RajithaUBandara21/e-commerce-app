package com.rajitha.ecommerce.mapper;

import com.rajitha.ecommerce.dto.ReviewResponseDTO;
import com.rajitha.ecommerce.entity.Review;
import org.springframework.stereotype.Component;

@Component
public class ReviewMapper {
    public ReviewResponseDTO toReviewResponseDTO(Review review) {
        return ReviewResponseDTO.builder()
                .id(review.getId())
                .productId(review.getProduct().getId())
                .customerId(review.getCustomerId())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdDate(review.getCreatedDate())
                .build();
    }
}
