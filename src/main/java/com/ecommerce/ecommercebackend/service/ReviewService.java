package com.ecommerce.ecommercebackend.service;

import com.ecommerce.ecommercebackend.dto.request.ReviewRequest;
import com.ecommerce.ecommercebackend.dto.response.ProductReviewsResponse;
import com.ecommerce.ecommercebackend.dto.response.RatingDto;
import com.ecommerce.ecommercebackend.dto.response.ReviewResponse;
import com.ecommerce.ecommercebackend.entity.Product;
import com.ecommerce.ecommercebackend.entity.Review;
import com.ecommerce.ecommercebackend.entity.User;
import com.ecommerce.ecommercebackend.exception.BadRequestException;
import com.ecommerce.ecommercebackend.exception.ResourceNotFoundException;
import com.ecommerce.ecommercebackend.repository.OrderRepository;
import com.ecommerce.ecommercebackend.repository.ProductRepository;
import com.ecommerce.ecommercebackend.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Business logic for product reviews (table {@code ratings}).
 *
 * <p>Only buyers may review (a non-cancelled order containing the product), and
 * each (user, product) pair has at most one review — re-reviewing updates it.
 * The same data feeds the AI recommendation engine via {@link #getRatingsDataset()}.</p>
 */
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    // ── Create / update (upsert) ─────────────────────────────────────────────────

    /**
     * Creates the user's review for a product, or updates it if one exists.
     *
     * @throws ResourceNotFoundException if the product does not exist
     * @throws BadRequestException       if the user has not purchased the product
     */
    @Transactional
    public ReviewResponse createOrUpdate(User user, ReviewRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with id: " + request.getProductId()));

        if (!orderRepository.hasPurchased(user.getId(), product.getId())) {
            throw new BadRequestException(
                    "Bạn chỉ có thể đánh giá sản phẩm đã mua.");
        }

        Review review = reviewRepository
                .findByUserIdAndProductId(user.getId(), product.getId())
                .orElseGet(() -> Review.builder().user(user).product(product).build());

        review.setRating(request.getRating());
        review.setComment(request.getComment());

        return toResponse(reviewRepository.save(review));
    }

    // ── Read ────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ProductReviewsResponse getProductReviews(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found with id: " + productId);
        }
        List<ReviewResponse> items = reviewRepository
                .findByProductIdOrderByCreatedAtDesc(productId).stream()
                .map(this::toResponse)
                .toList();
        Double avg = reviewRepository.averageRatingByProductId(productId);

        return ProductReviewsResponse.builder()
                .productId(productId)
                .averageRating(avg == null ? 0.0 : Math.round(avg * 10.0) / 10.0)
                .totalReviews(items.size())
                .items(items)
                .build();
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getMyReviews(User user) {
        return reviewRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    /** Full interaction dataset for the AI recommendation engine. */
    @Transactional(readOnly = true)
    public List<RatingDto> getRatingsDataset() {
        return reviewRepository.findAll().stream()
                .map(r -> RatingDto.builder()
                        .userId(r.getUser().getId())
                        .productId(r.getProduct().getId())
                        .rating(r.getRating())
                        .build())
                .toList();
    }

    // ── Delete ──────────────────────────────────────────────────────────────────

    /** Deletes a review the user owns. */
    @Transactional
    public void deleteOwn(User user, Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Review not found with id: " + reviewId));
        if (!review.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Review not found with id: " + reviewId);
        }
        reviewRepository.delete(review);
    }

    // ── Admin operations ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ReviewResponse> getAllReviews() {
        return reviewRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    /** Admin deletes any review (e.g. inappropriate content). */
    @Transactional
    public void deleteAny(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Review not found with id: " + reviewId));
        reviewRepository.delete(review);
    }

    // ── Mapping ──────────────────────────────────────────────────────────────────

    private ReviewResponse toResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .productId(review.getProduct().getId())
                .userId(review.getUser().getId())
                .username(review.getUser().getUsername())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
