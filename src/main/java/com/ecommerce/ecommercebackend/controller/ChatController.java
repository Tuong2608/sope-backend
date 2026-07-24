package com.ecommerce.ecommercebackend.controller;

import com.ecommerce.ecommercebackend.dto.request.SaveChatRequest;
import com.ecommerce.ecommercebackend.dto.request.ChatbotRequest;
import com.ecommerce.ecommercebackend.dto.response.ChatbotResponse;
import com.ecommerce.ecommercebackend.dto.response.ChatSessionResponse;
import com.ecommerce.ecommercebackend.dto.response.SaveChatResponse;
import com.ecommerce.ecommercebackend.service.ChatService;
import com.ecommerce.ecommercebackend.service.OrderChatService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.ecommerce.ecommercebackend.entity.User;
import com.ecommerce.ecommercebackend.entity.Role;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

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
public class ChatController {

    private final ChatService chatService;
    private final OrderChatService orderChatService;
    private final RestTemplate chatRestTemplate;

    public ChatController(
            ChatService chatService,
            OrderChatService orderChatService,
            @Qualifier("chatbotChatRestTemplate") RestTemplate chatRestTemplate) {
        this.chatService = chatService;
        this.orderChatService = orderChatService;
        this.chatRestTemplate = chatRestTemplate;
    }

    @Value("${app.chatbot.url:http://localhost:8000}")
    private String chatbotUrl;

    @Value("${app.chatbot.secret:}")
    private String chatbotSecret;

    /** Proxies browser chat requests to FastAPI so the frontend never calls Gemini directly. */
    @PostMapping
    public ResponseEntity<ChatbotResponse> chat(
            @Valid @RequestBody ChatbotRequest request,
            @AuthenticationPrincipal User user) {
        var personalOrderAnswer = orderChatService.answer(user, request.getMessage());
        if (personalOrderAnswer.isPresent()) {
            ChatbotResponse response = new ChatbotResponse();
            response.setReply(personalOrderAnswer.get());
            return ResponseEntity.ok(response);
        }

        String userId = user == null ? "anonymous" : String.valueOf(user.getId());
        try {
            ChatbotResponse response = chatRestTemplate.postForObject(
                    chatbotUrl.replaceAll("/+$", "") + "/api/chat",
                    new FastApiChatRequest(userId, request.getMessage()),
                    ChatbotResponse.class);
            if (response == null || !StringUtils.hasText(response.getReply())) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Chatbot không trả về phản hồi hợp lệ");
            }
            return ResponseEntity.ok(response);
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Dịch vụ chatbot hiện không khả dụng");
        }
    }

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

    private record FastApiChatRequest(String user_id, String message) {}
}
