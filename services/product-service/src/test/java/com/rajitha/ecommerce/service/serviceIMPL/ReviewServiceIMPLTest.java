package com.rajitha.ecommerce.service.serviceIMPL;

import com.rajitha.ecommerce.client.feign.OrderClient;
import com.rajitha.ecommerce.dto.ReviewRequestDTO;
import com.rajitha.ecommerce.entity.Product;
import com.rajitha.ecommerce.entity.ProductVariant;
import com.rajitha.ecommerce.entity.Review;
import com.rajitha.ecommerce.exeption.ReviewAccessDeniedException;
import com.rajitha.ecommerce.exeption.ReviewAlreadyExistsException;
import com.rajitha.ecommerce.exeption.ReviewNotVerifiedPurchaseException;
import com.rajitha.ecommerce.mapper.ReviewMapper;
import com.rajitha.ecommerce.repository.ProductRepository;
import com.rajitha.ecommerce.repository.ProductVariantRepository;
import com.rajitha.ecommerce.repository.ReviewRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class ReviewServiceIMPLTest {

    @InjectMocks
    private ReviewServiceIMPL reviewService;

    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private ProductVariantRepository productVariantRepository;
    @Mock
    private OrderClient orderClient;
    @Mock
    private ReviewMapper reviewMapper;

    @Test
    void shouldCreateReviewForVerifiedPurchase() {
        var product = Product.builder().id(1).build();
        var variant = ProductVariant.builder().id(10).product(product).build();

        Mockito.when(productRepository.findById(1)).thenReturn(Optional.of(product));
        Mockito.when(reviewRepository.existsByProduct_IdAndCustomerId(1, "customer-1")).thenReturn(false);
        Mockito.when(orderClient.findPurchasedVariantIds("customer-1")).thenReturn(List.of(10));
        Mockito.when(productVariantRepository.findAllByProduct_Id(1)).thenReturn(List.of(variant));
        Mockito.when(reviewRepository.save(Mockito.any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        var request = new ReviewRequestDTO(5, "Great fit");
        reviewService.createReview(1, "customer-1", request);

        Mockito.verify(reviewRepository).save(Mockito.any(Review.class));
    }

    @Test
    void shouldRejectReviewWithoutVerifiedPurchase() {
        var product = Product.builder().id(1).build();
        var variant = ProductVariant.builder().id(10).product(product).build();

        Mockito.when(productRepository.findById(1)).thenReturn(Optional.of(product));
        Mockito.when(reviewRepository.existsByProduct_IdAndCustomerId(1, "customer-1")).thenReturn(false);
        Mockito.when(orderClient.findPurchasedVariantIds("customer-1")).thenReturn(List.of(999));
        Mockito.when(productVariantRepository.findAllByProduct_Id(1)).thenReturn(List.of(variant));

        var request = new ReviewRequestDTO(5, "Never bought this");

        Assertions.assertThrows(ReviewNotVerifiedPurchaseException.class,
                () -> reviewService.createReview(1, "customer-1", request));
        Mockito.verify(reviewRepository, Mockito.never()).save(Mockito.any(Review.class));
    }

    @Test
    void shouldRejectDuplicateReview() {
        var product = Product.builder().id(1).build();
        Mockito.when(productRepository.findById(1)).thenReturn(Optional.of(product));
        Mockito.when(reviewRepository.existsByProduct_IdAndCustomerId(1, "customer-1")).thenReturn(true);

        var request = new ReviewRequestDTO(5, "Second attempt");

        Assertions.assertThrows(ReviewAlreadyExistsException.class,
                () -> reviewService.createReview(1, "customer-1", request));
        Mockito.verify(orderClient, Mockito.never()).findPurchasedVariantIds(Mockito.anyString());
    }

    @Test
    void shouldFailClosedWhenOrderServiceUnreachable() {
        var product = Product.builder().id(1).build();
        Mockito.when(productRepository.findById(1)).thenReturn(Optional.of(product));
        Mockito.when(reviewRepository.existsByProduct_IdAndCustomerId(1, "customer-1")).thenReturn(false);
        Mockito.when(orderClient.findPurchasedVariantIds("customer-1")).thenThrow(new RuntimeException("connection refused"));

        var request = new ReviewRequestDTO(5, "Nice");

        Assertions.assertThrows(ReviewNotVerifiedPurchaseException.class,
                () -> reviewService.createReview(1, "customer-1", request));
    }

    @Test
    void shouldAllowOwnerToDeleteReview() {
        var product = Product.builder().id(1).build();
        var review = Review.builder().id(5).product(product).customerId("customer-1").build();
        Mockito.when(reviewRepository.findById(5)).thenReturn(Optional.of(review));

        reviewService.deleteReview(1, 5, "customer-1", false);

        Mockito.verify(reviewRepository).delete(review);
    }

    @Test
    void shouldRejectNonOwnerNonAdminDelete() {
        var product = Product.builder().id(1).build();
        var review = Review.builder().id(5).product(product).customerId("customer-1").build();
        Mockito.when(reviewRepository.findById(5)).thenReturn(Optional.of(review));

        Assertions.assertThrows(ReviewAccessDeniedException.class,
                () -> reviewService.deleteReview(1, 5, "customer-2", false));
        Mockito.verify(reviewRepository, Mockito.never()).delete(Mockito.any(Review.class));
    }

    @Test
    void shouldAllowAdminToDeleteAnyReview() {
        var product = Product.builder().id(1).build();
        var review = Review.builder().id(5).product(product).customerId("customer-1").build();
        Mockito.when(reviewRepository.findById(5)).thenReturn(Optional.of(review));

        reviewService.deleteReview(1, 5, "admin-user", true);

        Mockito.verify(reviewRepository).delete(review);
    }

    @Test
    void shouldRejectDeleteWhenReviewBelongsToDifferentProduct() {
        var product = Product.builder().id(1).build();
        var review = Review.builder().id(5).product(product).customerId("customer-1").build();
        Mockito.when(reviewRepository.findById(5)).thenReturn(Optional.of(review));

        Assertions.assertThrows(EntityNotFoundException.class,
                () -> reviewService.deleteReview(2, 5, "customer-1", false));
    }

    @Test
    void shouldComputeRatingSummary() {
        var product = Product.builder().id(1).build();
        var reviewA = Review.builder().id(1).product(product).rating(4).build();
        var reviewB = Review.builder().id(2).product(product).rating(2).build();
        Mockito.when(reviewRepository.findByProduct_Id(1)).thenReturn(List.of(reviewA, reviewB));

        var summary = reviewService.getRatingSummary(1);

        Assertions.assertEquals(3.0, summary.averageRating());
        Assertions.assertEquals(2, summary.reviewCount());
    }

    @Test
    void shouldReturnEmptySummaryWhenNoReviews() {
        Mockito.when(reviewRepository.findByProduct_Id(1)).thenReturn(List.of());

        var summary = reviewService.getRatingSummary(1);

        Assertions.assertNull(summary.averageRating());
        Assertions.assertEquals(0, summary.reviewCount());
    }
}
