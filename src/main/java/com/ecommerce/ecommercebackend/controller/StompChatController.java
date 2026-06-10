package com.ecommerce.ecommercebackend.controller;

import com.ecommerce.ecommercebackend.dto.request.ChatMessageRequest;
import com.ecommerce.ecommercebackend.dto.response.ChatMessagePayload;
import com.ecommerce.ecommercebackend.entity.User;
import com.ecommerce.ecommercebackend.service.WebSocketChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * STOMP message controller xử lý tin nhắn WebSocket real-time.
 *
 * <h2>Task 6 — Phòng chat riêng theo cặp Buyer–Seller</h2>
 *
 * <p>Client subscribe topic: {@code /topic/chat.buyer_{buyerId}_seller_{sellerId}}</p>
 * <p>Client gửi tin: {@code /app/chat.send} với body {@link ChatMessageRequest}</p>
 *
 * <h3>Luồng xử lý:</h3>
 * <pre>
 * Client (Next.js)
 *   → STOMP CONNECT ws://localhost:8080/ws   (với JWT trong header)
 *   → SUBSCRIBE /topic/chat.buyer_42_seller_7
 *   → SEND /app/chat.send  { buyerId:42, sellerId:7, content:"hello" }
 *     → StompChatController.handleMessage()
 *       → WebSocketChatService.sendMessage()
 *         → SimpMessagingTemplate → /topic/chat.buyer_42_seller_7
 *           → Cả Buyer và Seller nhận được message
 * </pre>
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/api/ws")
public class StompChatController {

    private final WebSocketChatService webSocketChatService;

    // ── Task 6: Xử lý gửi tin nhắn ───────────────────────────────────────────

    /**
     * Nhận tin nhắn từ client và broadcast tới phòng chat của cặp Buyer–Seller.
     *
     * <p>Destination: {@code /app/chat.send}</p>
     *
     * <p>Chỉ Buyer hoặc Seller trong cặp mới được gửi tin nhắn —
     * xác thực qua JWT đã được gắn tại bước STOMP CONNECT.</p>
     *
     * @param request   Payload từ client
     * @param principal User đang kết nối (từ JWT đã xác thực)
     */
    @MessageMapping("/chat.send")
    public void handleMessage(@Payload ChatMessageRequest request,
                              Principal principal) {
        if (principal == null) {
            log.warn("[WS CHAT] Từ chối tin nhắn: chưa xác thực");
            return;
        }

        User user = extractUser(principal);
        Long senderId       = user.getId();
        String senderUsername = user.getUsername();

        log.info("[WS CHAT] {} gửi tin nhắn tới phòng buyer_{}_seller_{}",
                senderUsername, request.getBuyerId(), request.getSellerId());

        webSocketChatService.sendMessage(
                request.getBuyerId(),
                request.getSellerId(),
                senderId,
                senderUsername,
                request.getContent()
        );
    }

    // ── Xử lý đang gõ ────────────────────────────────────────────────────────

    /**
     * Broadcast sự kiện "đang gõ" trong phòng chat.
     *
     * <p>Destination: {@code /app/chat.typing}</p>
     */
    @MessageMapping("/chat.typing")
    public void handleTyping(@Payload ChatMessageRequest request,
                             Principal principal) {
        if (principal == null) return;

        User user = extractUser(principal);
        webSocketChatService.sendTyping(
                request.getBuyerId(),
                request.getSellerId(),
                user.getId(),
                user.getUsername()
        );
    }

    // ── REST: Lấy thông tin room ──────────────────────────────────────────────

    /**
     * REST endpoint tiện ích — trả về thông tin về một phòng chat.
     * Client có thể gọi để biết topic cần subscribe trước khi kết nối WebSocket.
     *
     * <p>GET /api/ws/room/{buyerId}/{sellerId}</p>
     */
    @GetMapping("/room/{buyerId}/{sellerId}")
    @ResponseBody
    public Map<String, String> getRoomInfo(
            @PathVariable Long buyerId,
            @PathVariable Long sellerId) {

        String roomId = WebSocketChatService.buildRoomId(buyerId, sellerId);
        String topic  = WebSocketChatService.buildRoomTopic(roomId);

        return Map.of(
                "roomId",         roomId,
                "subscribeTopic", topic,
                "sendDestination", "/app/chat.send",
                "typingDestination", "/app/chat.typing"
        );
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private User extractUser(Principal principal) {
        if (principal instanceof Authentication auth
                && auth.getPrincipal() instanceof User user) {
            return user;
        }
        throw new IllegalStateException("Cannot extract User from principal: " + principal);
    }
}
