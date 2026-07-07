package com.ecommerce.ecommercebackend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request payload for POST /api/auth/google.
 */
@Data
public class GoogleLoginRequest {

    @NotBlank(message = "Google credential is required")
    private String credential;
}
