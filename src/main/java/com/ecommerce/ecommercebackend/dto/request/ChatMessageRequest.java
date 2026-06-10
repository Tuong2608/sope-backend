package com.ecommerce.ecommercebackend.dto.request;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Payload client gửi lên khi gửi tin nhắn trong phòng chat Buyer–Seller.
 * Destination STOMP: {@code /app/chat.send}
 */
@Data
public class ChatMessageRequest {

    /**
     * Mã Buyer (người mua) — dùng để xác định phòng chat.
     * Ví dụ: "42" (userId của buyer)
     */
    @NotNull
    private Long buyerId;

    /**
     * Mã Seller (người bán) — dùng để xác định phòng chat.
     * Ví dụ: "7" (userId của seller)
     */
    @NotNull
    private Long sellerId;

    /** Nội dung tin nhắn. */
    @NotBlank
    private String content;
}
