package com.ecommerce.ecommercebackend.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * Minimal interaction record exposed to the AI recommendation engine via
 * {@code GET /api/ratings} — mirrors the {@code ratings} table columns
 * ({@code user_id}, {@code product_id}, {@code rating}).
 */
@Data
@Builder
public class RatingDto {

    private Long userId;
    private Long productId;
    private int rating;
}
