package com.ecommerce.ecommercebackend.service;

import com.ecommerce.ecommercebackend.dto.response.ChatMessagePayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Service xử lý luồng tin nhắn real-time giữa Buyer và Seller qua STOMP.
 *
 * <h2>Task 6 — Phòng chat riêng biệt theo cặp Buyer–Seller</h2>
 *
 * <p>Mỗi cặp (buyerId, sellerId) có một topic riêng:
 * {@code /topic/chat.buyer_{buyerId}_seller_{sellerId}}</p>
 *
 * <p>Room ID được tạo theo quy tắc nhất quán:
 * buyerId nhỏ hơn luôn đứng trước → đảm bảo cùng 1 topic
 * dù Buyer hay Seller gửi trước.</p>
 *
 * <pre>
 * Buyer(42) ↔ Seller(7):
 *   roomId = "buyer_42_seller_7"
 *   topic  = "/topic/chat.buyer_42_seller_7"
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketChatService {

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationService   notificationService;

    // ── Room ID builder ───────────────────────────────────────────────────────

    /**
     * Tạo roomId chuẩn hoá cho cặp Buyer–Seller.
     *
     * <p>Format: {@code buyer_{buyerId}_seller_{sellerId}}</p>
     *
     * @param buyerId  ID người mua
     * @param sellerId ID người bán
     * @return Room ID duy nhất cho cặp này
     */
    public static String buildRoomId(Long buyerId, Long sellerId) {
        return "buyer_" + buyerId + "_seller_" + sellerId;
    }

    /**
     * Trả về STOMP topic destination cho một phòng chat.
     *
     * @param roomId Room ID (từ {@link #buildRoomId})
     * @return Destination string, ví dụ: {@code /topic/chat.buyer_42_seller_7}
     */
    public static String buildRoomTopic(String roomId) {
        return "/topic/chat." + roomId;
    }

    // ── Gửi tin nhắn ─────────────────────────────────────────────────────────

    /**
     * Broadcast tin nhắn tới topic của phòng chat Buyer–Seller.
     *
     * <p>Được gọi bởi {@link com.ecommerce.ecommercebackend.controller.StompChatController}
     * sau khi nhận frame {@code /app/chat.send}.</p>
     *
     * @param buyerId         ID người mua
     * @param sellerId        ID người bán
     * @param senderId        ID người gửi tin nhắn (Buyer hoặc Seller)
     * @param senderUsername  Username người gửi
     * @param content         Nội dung tin nhắn
     */
    public void sendMessage(Long buyerId, Long sellerId,
                            Long senderId, String senderUsername,
                            String content) {
        String roomId = buildRoomId(buyerId, sellerId);
        String topic  = buildRoomTopic(roomId);

        ChatMessagePayload payload = ChatMessagePayload.builder()
                .senderUsername(senderUsername)
                .senderId(senderId)
                .content(content)
                .type("CHAT")
                .roomId(roomId)
                .timestamp(LocalDateTime.now())
                .build();

        // Broadcast tới tất cả subscriber của phòng chat này
        messagingTemplate.convertAndSend(topic, payload);
        log.info("[WS CHAT] {} → {} | sender={}", topic, roomId, senderUsername);

        // Trigger thông báo cho người nhận (người kia trong phòng chat)
        Long recipientId = senderId.equals(buyerId) ? sellerId : buyerId;
        notificationService.notifyNewMessage(recipientId, senderUsername, roomId);
    }

    // ── Thông báo đang gõ ────────────────────────────────────────────────────

    /**
     * Broadcast sự kiện "đang gõ" tới phòng chat.
     *
     * <p>Được gọi bởi {@code /app/chat.typing}.</p>
     *
     * @param buyerId        ID người mua
     * @param sellerId       ID người bán
     * @param senderId       ID người đang gõ
     * @param senderUsername Username người đang gõ
     */
    public void sendTyping(Long buyerId, Long sellerId,
                           Long senderId, String senderUsername) {
        String roomId = buildRoomId(buyerId, sellerId);
        String topic  = buildRoomTopic(roomId);

        ChatMessagePayload payload = ChatMessagePayload.builder()
                .senderUsername(senderUsername)
                .senderId(senderId)
                .content("")
                .type("TYPING")
                .roomId(roomId)
                .timestamp(LocalDateTime.now())
                .build();

        messagingTemplate.convertAndSend(topic, payload);
    }
}
