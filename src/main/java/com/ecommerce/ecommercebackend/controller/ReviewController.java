package com.ecommerce.ecommercebackend.controller;

import com.ecommerce.ecommercebackend.dto.request.ReviewRequest;
import com.ecommerce.ecommercebackend.dto.response.ProductReviewsResponse;
import com.ecommerce.ecommercebackend.dto.response.RatingDto;
import com.ecommerce.ecommercebackend.dto.response.ReviewResponse;
import com.ecommerce.ecommercebackend.entity.User;
import com.ecommerce.ecommercebackend.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for product reviews and the AI ratings dataset.
 *
 * <ul>
 *   <li>{@code POST /api/reviews} — create/update own review (auth, buyers only).</li>
 *   <li>{@code GET /api/products/{id}/reviews} — public reviews + average rating.</li>
 *   <li>{@code GET /api/reviews/me} — caller's reviews (auth).</li>
 *   <li>{@code DELETE /api/reviews/{id}} — delete own review (auth).</li>
 *   <li>{@code GET /api/ratings} — interaction dataset for the AI engine (public).</li>
 * </ul>
 */
@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/api/reviews")
    public ResponseEntity<ReviewResponse> create(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ReviewRequest request) {
        return ResponseEntity.ok(reviewService.createOrUpdate(user, request));
    }

    @GetMapping("/api/products/{productId}/reviews")
    public ResponseEntity<ProductReviewsResponse> productReviews(@PathVariable Long productId) {
        return ResponseEntity.ok(reviewService.getProductReviews(productId));
    }

    @GetMapping("/api/reviews/me")
    public ResponseEntity<List<ReviewResponse>> myReviews(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(reviewService.getMyReviews(user));
    }

    @DeleteMapping("/api/reviews/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        reviewService.deleteOwn(user, id);
        return ResponseEntity.noContent().build();
    }

    /** Interaction dataset consumed by the AI recommendation engine. */
    @GetMapping("/api/ratings")
    public ResponseEntity<List<RatingDto>> ratingsDataset() {
        return ResponseEntity.ok(reviewService.getRatingsDataset());
    }
}
