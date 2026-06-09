package com.ecommerce.ecommercebackend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request payload pushed by the chatbot service (Nhân's FastAPI) after each turn.
 *
 * <p>Field names match the contract already implemented on the FastAPI side
 * ({@code POST /api/chat/save} with {@code userId / userMessage / botReply}).
 * One request carries a single conversation turn (the customer's question plus
 * the AI's reply).</p>
 */
@Data
public class SaveChatRequest {

    /** Chatter identifier (logged-in user id or a guest tag). May be blank. */
    private String userId;

    @NotBlank(message = "userMessage is required")
    private String userMessage;

    /** The AI reply. Stored as-is (kept lenient so a message is never lost). */
    private String botReply;
}
