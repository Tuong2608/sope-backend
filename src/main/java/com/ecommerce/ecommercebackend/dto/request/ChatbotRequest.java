package com.ecommerce.ecommercebackend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatbotRequest {

    @NotBlank(message = "message is required")
    private String message;
}
