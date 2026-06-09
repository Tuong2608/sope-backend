package com.ecommerce.ecommercebackend.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * Lightweight acknowledgement returned to the chatbot after a turn is stored.
 * (The FastAPI side only checks the HTTP status, but a small body aids debugging.)
 */
@Data
@Builder
public class SaveChatResponse {

    private String status;
    private Long sessionId;
    /** Total messages stored in the session after this save. */
    private int messageCount;
}
