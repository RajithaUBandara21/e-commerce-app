package com.rajitha.ecommerce.service.serviceIMPL;

import com.rajitha.ecommerce.client.feign.OrderClient;
import com.rajitha.ecommerce.dto.RatingSummaryDTO;
import com.rajitha.ecommerce.dto.ReviewRequestDTO;
import com.rajitha.ecommerce.dto.ReviewResponseDTO;
import com.rajitha.ecommerce.entity.ProductVariant;
import com.rajitha.ecommerce.entity.Review;
import com.rajitha.ecommerce.exeption.ReviewAccessDeniedException;
import com.rajitha.ecommerce.exeption.ReviewAlreadyExistsException;
import com.rajitha.ecommerce.exeption.ReviewNotVerifiedPurchaseException;
import com.rajitha.ecommerce.mapper.ReviewMapper;
import com.rajitha.ecommerce.repository.ProductRepository;
import com.rajitha.ecommerce.repository.ProductVariantRepository;
import com.rajitha.ecommerce.repository.ReviewRepository;
import com.rajitha.ecommerce.service.ReviewService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReviewServiceIMPL implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final OrderClient orderClient;
    private final ReviewMapper reviewMapper;

    public ReviewServiceIMPL(ReviewRepository reviewRepository, ProductRepository productRepository,
                              ProductVariantRepository productVariantRepository, OrderClient orderClient,
                              ReviewMapper reviewMapper) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.orderClient = orderClient;
        this.reviewMapper = reviewMapper;
    }

    @Override
    public ReviewResponseDTO createReview(Integer productId, String customerId, ReviewRequestDTO request) {
        var product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found" + productId));

        if (reviewRepository.existsByProduct_IdAndCustomerId(productId, customerId)) {
            throw new ReviewAlreadyExistsException("You have already reviewed product " + productId);
        }

        if (!hasVerifiedPurchase(productId, customerId)) {
            throw new ReviewNotVerifiedPurchaseException("Only customers who purchased this product can review it");
        }

        var review = Review.builder()
                .product(product)
                .customerId(customerId)
                .rating(request.rating())
                .comment(request.comment())
                .build();

        return reviewMapper.toReviewResponseDTO(reviewRepository.save(review));
    }

    private boolean hasVerifiedPurchase(Integer productId, String customerId) {
        List<Integer> purchasedVariantIds;
        try {
            purchasedVariantIds = orderClient.findPurchasedVariantIds(customerId);
        } catch (RuntimeException e) {
            // order-service unreachable — fail closed rather than let an outage
            // silently let unverified reviews through.
            throw new ReviewNotVerifiedPurchaseException("Could not verify your purchase right now, please try again later");
        }

        if (purchasedVariantIds.isEmpty()) {
            return false;
        }

        var productVariantIds = productVariantRepository.findAllByProduct_Id(productId)
                .stream()
                .map(ProductVariant::getId)
                .collect(Collectors.toSet());

        return purchasedVariantIds.stream().anyMatch(productVariantIds::contains);
    }

    @Override
    public List<ReviewResponseDTO> findByProductId(Integer productId) {
        return reviewRepository.findByProduct_IdOrderByCreatedDateDesc(productId)
                .stream()
                .map(reviewMapper::toReviewResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteReview(Integer productId, Integer reviewId, String callerId, boolean isAdmin) {
        var review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("Review not found" + reviewId));

        if (!review.getProduct().getId().equals(productId)) {
            throw new EntityNotFoundException("Review not found" + reviewId);
        }

        if (!isAdmin && !review.getCustomerId().equals(callerId)) {
            throw new ReviewAccessDeniedException("You do not own review " + reviewId);
        }

        reviewRepository.delete(review);
    }

    @Override
    public RatingSummaryDTO getRatingSummary(Integer productId) {
        var reviews = reviewRepository.findByProduct_Id(productId);
        if (reviews.isEmpty()) {
            return RatingSummaryDTO.EMPTY;
        }
        double average = reviews.stream().mapToInt(Review::getRating).average().orElse(0);
        return new RatingSummaryDTO(average, reviews.size());
    }
}
