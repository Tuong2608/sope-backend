package com.ecommerce.ecommercebackend.controller;

import com.ecommerce.ecommercebackend.dto.request.SaveChatRequest;
import com.ecommerce.ecommercebackend.dto.response.ChatSessionResponse;
import com.ecommerce.ecommercebackend.dto.response.SaveChatResponse;
import com.ecommerce.ecommercebackend.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.ecommerce.ecommercebackend.entity.User;
import com.ecommerce.ecommercebackend.entity.Role;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * REST controller for chatbot conversation storage and retrieval.
 *
 * <ul>
 *   <li>{@code POST /api/chat/save} — ingestion from the chatbot (FastAPI).
 *       Public (no JWT); must return HTTP 200 per the agreed contract.</li>
 *   <li>{@code GET /api/chat/sessions} — list conversations (seller dashboard).</li>
 *   <li>{@code GET /api/chat/sessions/{userId}} — one chatter's full history
 *       (the "context package"). Both GETs require authentication.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @Value("${app.chatbot.secret:}")
    private String chatbotSecret;

    /**
     * Receives one conversation turn from the chatbot and stores it.
     * Returns 200 OK — the FastAPI side checks the status code.
     */
    @PostMapping("/save")
    public ResponseEntity<SaveChatResponse> save(
            @Valid @RequestBody SaveChatRequest request,
            @RequestHeader(value = "X-Chatbot-Secret", required = false) String secret) {
        if (StringUtils.hasText(chatbotSecret) && !chatbotSecret.equals(secret)) {
            throw new AccessDeniedException("Invalid chatbot secret");
        }
        return ResponseEntity.ok(chatService.saveTurn(request));
    }

    /** Lists all chat sessions for the seller dashboard (no message bodies). */
    @GetMapping("/sessions")
    public ResponseEntity<List<ChatSessionResponse>> listSessions(@AuthenticationPrincipal User user) {
        if (user.getRole() != Role.ROLE_ADMIN) {
            throw new AccessDeniedException("Only admins can list all chat sessions");
        }
        return ResponseEntity.ok(chatService.listSessions());
    }

    /** Returns a chatter's full conversation history (the context package). */
    @GetMapping("/sessions/{userId}")
    public ResponseEntity<ChatSessionResponse> getHistory(
            @PathVariable String userId,
            @AuthenticationPrincipal User user) {
        if (user.getRole() != Role.ROLE_ADMIN 
            && !userId.equals(user.getUsername()) 
            && !userId.equals(String.valueOf(user.getId()))) {
            throw new AccessDeniedException("Cannot view chat session of another user");
        }
        return ResponseEntity.ok(chatService.getHistory(userId));
    }
}
