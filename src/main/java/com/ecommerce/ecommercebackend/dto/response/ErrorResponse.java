package com.ecommerce.ecommercebackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Unified error envelope returned by {@code GlobalExceptionHandler}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    private int    status;
    private String error;
    private String message;
    private LocalDateTime timestamp;
}
