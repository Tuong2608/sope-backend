package com.ecommerce.ecommercebackend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * A single product review returned to clients.
 */
@Data
@Builder
public class ReviewResponse {

    private Long id;
    private Long productId;
    private Long userId;
    private String username;
    private int rating;
    private String comment;
    private LocalDateTime createdAt;
}
