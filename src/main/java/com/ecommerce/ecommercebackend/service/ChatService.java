package com.ecommerce.ecommercebackend.service;

import com.ecommerce.ecommercebackend.dto.request.SaveChatRequest;
import com.ecommerce.ecommercebackend.dto.response.ChatMessageResponse;
import com.ecommerce.ecommercebackend.dto.response.ChatSessionResponse;
import com.ecommerce.ecommercebackend.dto.response.SaveChatResponse;
import com.ecommerce.ecommercebackend.entity.AuthProvider;
import com.ecommerce.ecommercebackend.entity.ChatMessage;
import com.ecommerce.ecommercebackend.entity.ChatSession;
import com.ecommerce.ecommercebackend.entity.MessageSender;
import com.ecommerce.ecommercebackend.entity.Role;
import com.ecommerce.ecommercebackend.entity.User;
import com.ecommerce.ecommercebackend.exception.ResourceNotFoundException;
import com.ecommerce.ecommercebackend.repository.ChatSessionRepository;
import com.ecommerce.ecommercebackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatService {

    private static final String ANONYMOUS = "anonymous";
    private static final String CHAT_EMAIL_DOMAIN = "@chat.sope.local";

    private final ChatSessionRepository chatSessionRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public SaveChatResponse saveTurn(SaveChatRequest request) {
        String userKey = normalizeUserKey(request.getUserId());
        User user = resolveChatUser(userKey);

        ChatSession session = chatSessionRepository.findByUserUsername(user.getUsername())
                .orElseGet(() -> ChatSession.builder().user(user).build());

        session.addMessage(ChatMessage.builder()
                .sender(MessageSender.USER)
                .content(request.getUserMessage())
                .build());

        if (StringUtils.hasText(request.getBotReply())) {
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

    @Transactional(readOnly = true)
    public List<ChatSessionResponse> listSessions() {
        return chatSessionRepository.findAllByOrderByUpdatedAtDesc().stream()
                .map(session -> toSessionResponse(session, false))
                .toList();
    }

    @Transactional(readOnly = true)
    public ChatSessionResponse getHistory(String userId) {
        String userKey = normalizeUserKey(userId);
        ChatSession session = findSessionByUserKey(userKey)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Chat session not found for userId: " + userId));
        return toSessionResponse(session, true);
    }

    private Optional<ChatSession> findSessionByUserKey(String userKey) {
        Optional<ChatSession> byUsername = chatSessionRepository.findByUserUsername(userKey);
        if (byUsername.isPresent()) {
            return byUsername;
        }

        try {
            return chatSessionRepository.findByUserId(Long.parseLong(userKey));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    private User resolveChatUser(String userKey) {
        Optional<User> existing = userRepository.findByUsernameOrEmail(userKey, userKey);
        if (existing.isPresent()) {
            return existing.get();
        }

        try {
            Optional<User> byId = userRepository.findById(Long.parseLong(userKey));
            if (byId.isPresent()) {
                return byId.get();
            }
        } catch (NumberFormatException ignored) {
            // Continue with guest-user creation below.
        }

        return createChatOnlyUser(userKey);
    }

    private User createChatOnlyUser(String userKey) {
        String username = buildUniqueChatUsername(userKey);
        String email = username + CHAT_EMAIL_DOMAIN;

        User user = User.builder()
                .username(username)
                .password(generateChatPasswordHash())
                .email(email)
                .role(Role.ROLE_USER)
                .provider(AuthProvider.LOCAL)
                .enabled(false)
                .emailVerified(false)
                .build();

        return userRepository.save(user);
    }

    private String buildUniqueChatUsername(String userKey) {
        String clean = userKey.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._-]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_+|_+$", "");

        String base = ANONYMOUS.equals(userKey) ? ANONYMOUS : "chat_" + clean;
        if (!StringUtils.hasText(base) || base.length() < 3) {
            base = ANONYMOUS;
        }
        if (base.length() > 45) {
            base = base.substring(0, 45);
        }

        String candidate = base;
        int suffix = 1;
        while (userRepository.existsByUsername(candidate) || userRepository.existsByEmail(candidate + CHAT_EMAIL_DOMAIN)) {
            candidate = base + suffix;
            suffix++;
        }
        return candidate;
    }

    private String generateChatPasswordHash() {
        return passwordEncoder.encode(UUID.randomUUID().toString());
    }

    private ChatSessionResponse toSessionResponse(ChatSession session, boolean withMessages) {
        List<ChatMessageResponse> messages = withMessages
                ? session.getMessages().stream().map(this::toMessageResponse).toList()
                : null;

        User user = session.getUser();

        return ChatSessionResponse.builder()
                .id(session.getId())
                .userId(user == null ? ANONYMOUS : user.getUsername())
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

    private String normalizeUserKey(String userId) {
        return StringUtils.hasText(userId) ? userId.trim() : ANONYMOUS;
    }
}
