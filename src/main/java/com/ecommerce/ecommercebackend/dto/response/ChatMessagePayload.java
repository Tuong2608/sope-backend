package com.ecommerce.ecommercebackend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Payload được broadcast tới topic của phòng chat sau khi tin nhắn được xử lý.
 * Topic đích: {@code /topic/chat.buyer_{buyerId}_seller_{sellerId}}
 */
@Data
@Builder
public class ChatMessagePayload {

    /** ID bản ghi tin nhắn trong DB (nếu có lưu). */
    private Long id;

    /** Username của người gửi. */
    private String senderUsername;

    /** ID của người gửi. */
    private Long senderId;

    /** Nội dung tin nhắn. */
    private String content;

    /** Loại: "CHAT" hoặc "TYPING" hoặc "SYSTEM". */
    private String type;

    /** Room ID theo format: buyer_{buyerId}_seller_{sellerId} */
    private String roomId;

    private LocalDateTime timestamp;
}
