package com.ecommerce.ecommercebackend.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request payload for creating/updating a product review.
 */
@Data
public class ReviewRequest {

    @NotNull(message = "productId is required")
    private Long productId;

    @Min(value = 1, message = "rating must be between 1 and 5")
    @Max(value = 5, message = "rating must be between 1 and 5")
    private int rating;

    private String comment;
}
