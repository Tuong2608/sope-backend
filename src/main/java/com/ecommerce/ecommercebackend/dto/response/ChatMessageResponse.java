package com.ecommerce.ecommercebackend.dto.response;

import com.ecommerce.ecommercebackend.entity.MessageSender;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * A single message in a conversation history (the "context package").
 */
@Data
@Builder
public class ChatMessageResponse {

    private Long id;
    private MessageSender sender;
    private String content;
    private LocalDateTime createdAt;
}
