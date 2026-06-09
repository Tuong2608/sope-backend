package com.ecommerce.ecommercebackend.dto.response;

import com.ecommerce.ecommercebackend.entity.ChatSessionStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * A chat session. Used both as a list item for the seller dashboard
 * (with {@code messages = null}) and as the full conversation/context package
 * (with {@code messages} populated).
 */
@Data
@Builder
public class ChatSessionResponse {

    private Long id;
    private String userId;
    private ChatSessionStatus status;
    private int messageCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Full ordered history; {@code null} in list views. */
    private List<ChatMessageResponse> messages;
}
