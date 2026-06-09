package com.ecommerce.ecommercebackend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A chat conversation = the "context package" (Gói ngữ cảnh) of one chatter.
 *
 * <p>The chatbot (Nhân's FastAPI) identifies the chatter only by a free-form
 * {@code userId} string (a logged-in user id or a guest tag like
 * "khach_hang_test"); there is no separate session id in the contract. We
 * therefore keep <em>one</em> session per {@code userId} and append every turn
 * to it, so the full history can be replayed as LLM context.</p>
 */
@Entity
@Table(
        name = "chat_sessions",
        uniqueConstraints = @UniqueConstraint(name = "uk_chat_sessions_user", columnNames = "user_id")
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Free-form chatter identifier from the chatbot (user id or guest tag). */
    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ChatSessionStatus status = ChatSessionStatus.CHATBOT;

    @OneToMany(
            mappedBy = "session",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @OrderBy("createdAt ASC, id ASC")
    @Builder.Default
    private List<ChatMessage> messages = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void addMessage(ChatMessage message) {
        messages.add(message);
        message.setSession(this);
    }
}
