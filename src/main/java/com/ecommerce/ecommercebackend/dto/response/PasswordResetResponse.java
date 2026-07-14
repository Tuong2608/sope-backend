package com.ecommerce.ecommercebackend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PasswordResetResponse {

    private String message;
}
