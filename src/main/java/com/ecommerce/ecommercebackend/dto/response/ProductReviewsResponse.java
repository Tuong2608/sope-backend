package com.ecommerce.ecommercebackend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * A product's reviews together with their roll-up rating.
 */
@Data
@Builder
public class ProductReviewsResponse {

    private Long productId;
    /** Mean star rating (0 when there are no reviews). */
    private double averageRating;
    private int totalReviews;
    private List<ReviewResponse> items;
}
