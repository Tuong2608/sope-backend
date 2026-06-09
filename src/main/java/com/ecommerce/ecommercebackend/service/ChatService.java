package com.ecommerce.ecommercebackend.service;

import com.ecommerce.ecommercebackend.dto.request.SaveChatRequest;
import com.ecommerce.ecommercebackend.dto.response.ChatMessageResponse;
import com.ecommerce.ecommercebackend.dto.response.ChatSessionResponse;
import com.ecommerce.ecommercebackend.dto.response.SaveChatResponse;
import com.ecommerce.ecommercebackend.entity.ChatMessage;
import com.ecommerce.ecommercebackend.entity.ChatSession;
import com.ecommerce.ecommercebackend.entity.MessageSender;
import com.ecommerce.ecommercebackend.exception.ResourceNotFoundException;
import com.ecommerce.ecommercebackend.repository.ChatSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Business logic for persisting chatbot conversations — the "Gói ngữ cảnh".
 *
 * <p>Each turn pushed by the chatbot is appended (as a USER message + an AI
 * reply) to the chatter's single {@link ChatSession}, building a replayable
 * conversation history. Also exposes read access for the seller dashboard and
 * for feeding context back to the LLM.</p>
 */
@Service
@RequiredArgsConstructor
public class ChatService {

    private static final String ANONYMOUS = "anonymous";

    private final ChatSessionRepository chatSessionRepository;

    // ── Ingestion (called by the chatbot service) ───────────────────────────────

    /**
     * Stores one conversation turn. Creates the session on first contact.
     */
    @Transactional
    public SaveChatResponse saveTurn(SaveChatRequest request) {
        String userId = (request.getUserId() == null || request.getUserId().isBlank())
                ? ANONYMOUS
                : request.getUserId().trim();

        ChatSession session = chatSessionRepository.findByUserId(userId)
                .orElseGet(() -> ChatSession.builder().userId(userId).build());

        session.addMessage(ChatMessage.builder()
                .sender(MessageSender.USER)
                .content(request.getUserMessage())
                .build());

        if (request.getBotReply() != null && !request.getBotReply().isBlank()) {
            session.addMessage(ChatMessage.builder()
                    .sender(MessageSender.AI)
                    .content(request.getBotReply())
                    .build());
        }

        ChatSession saved = chatSessionRepository.save(session);

        return SaveChatResponse.builder()
                .status("saved")
                .sessionId(saved.getId())
                .messageCount(saved.getMessages().size())
                .build();
    }

    // ── Read (seller dashboard / LLM context) ────────────────────────────────────

    /** Lists all sessions (most recent first) without message bodies. */
    @Transactional(readOnly = true)
    public List<ChatSessionResponse> listSessions() {
        return chatSessionRepository.findAllByOrderByUpdatedAtDesc().stream()
                .map(s -> toSessionResponse(s, false))
                .toList();
    }

    /** Returns a chatter's full conversation history — the context package. */
    @Transactional(readOnly = true)
    public ChatSessionResponse getHistory(String userId) {
        ChatSession session = chatSessionRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Chat session not found for userId: " + userId));
        return toSessionResponse(session, true);
    }

    // ── Mapping ──────────────────────────────────────────────────────────────────

    private ChatSessionResponse toSessionResponse(ChatSession session, boolean withMessages) {
        List<ChatMessageResponse> messages = withMessages
                ? session.getMessages().stream().map(this::toMessageResponse).toList()
                : null;

        return ChatSessionResponse.builder()
                .id(session.getId())
                .userId(session.getUserId())
                .status(session.getStatus())
                .messageCount(session.getMessages().size())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .messages(messages)
                .build();
    }

    private ChatMessageResponse toMessageResponse(ChatMessage message) {
        return ChatMessageResponse.builder()
                .id(message.getId())
                .sender(message.getSender())
                .content(message.getContent())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
