package com.ecommerce.ecommercebackend.controller;

import com.ecommerce.ecommercebackend.dto.request.SaveChatRequest;
import com.ecommerce.ecommercebackend.dto.response.ChatSessionResponse;
import com.ecommerce.ecommercebackend.dto.response.SaveChatResponse;
import com.ecommerce.ecommercebackend.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    /**
     * Receives one conversation turn from the chatbot and stores it.
     * Returns 200 OK — the FastAPI side checks the status code.
     */
    @PostMapping("/save")
    public ResponseEntity<SaveChatResponse> save(@Valid @RequestBody SaveChatRequest request) {
        return ResponseEntity.ok(chatService.saveTurn(request));
    }

    /** Lists all chat sessions for the seller dashboard (no message bodies). */
    @GetMapping("/sessions")
    public ResponseEntity<List<ChatSessionResponse>> listSessions() {
        return ResponseEntity.ok(chatService.listSessions());
    }

    /** Returns a chatter's full conversation history (the context package). */
    @GetMapping("/sessions/{userId}")
    public ResponseEntity<ChatSessionResponse> getHistory(@PathVariable String userId) {
        return ResponseEntity.ok(chatService.getHistory(userId));
    }
}
