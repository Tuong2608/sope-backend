package com.ecommerce.ecommercebackend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Arrays;

/**
 * Cấu hình WebSocket Server sử dụng STOMP message broker.
 *
 * <h2>Kiến trúc topic</h2>
 * <pre>
 * ┌──────────────────────────────────────────────────────────────────┐
 * │  Application Destinations (client → server)                      │
 * │  /app/chat.send          – gửi tin nhắn trong phòng chat         │
 * │  /app/chat.typing        – thông báo đang gõ                     │
 * │  /app/notification.mark  – đánh dấu thông báo đã đọc            │
 * ├──────────────────────────────────────────────────────────────────┤
 * │  Broker Destinations (server → client, subscribe)                │
 * │  /topic/chat.{roomId}    – tin nhắn của phòng chat cụ thể        │
 * │    roomId = "buyer_{buyerId}_seller_{sellerId}"                  │
 * │  /topic/notification.{userId} – thông báo hệ thống cho 1 user    │
 * │  /user/queue/errors      – lỗi private gửi về đúng user          │
 * └──────────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * <h2>Task 5 — WebSocket Server & Trigger</h2>
 * <ul>
 *   <li>Endpoint handshake: {@code /ws} (native) + {@code /ws-sockjs} (SockJS fallback)</li>
 *   <li>In-memory broker cho {@code /topic} và {@code /user}</li>
 *   <li>Application prefix {@code /app} cho các handler method</li>
 * </ul>
 *
 * <h2>Task 6 — STOMP Rooms theo cặp Buyer-Seller</h2>
 * <ul>
 *   <li>Mỗi cặp Buyer–Seller có topic riêng biệt:
 *       {@code /topic/chat.buyer_{X}_seller_{Y}}</li>
 *   <li>JWT được xác thực qua {@link WebSocketAuthInterceptor} tại bước CONNECT,
 *       đảm bảo chỉ đúng Buyer hoặc Seller mới subscribe được phòng của họ.</li>
 * </ul>
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    @Value("${app.frontend.origins:https://sope-frontend-self.vercel.app,http://localhost:3000,http://127.0.0.1:3000}")
    private String frontendOrigins;

    // ── Task 5: Cấu hình Message Broker ──────────────────────────────────────

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // In-memory broker phục vụ các topic subscription
        registry.enableSimpleBroker("/topic", "/user");

        // Prefix cho các @MessageMapping trong controller
        registry.setApplicationDestinationPrefixes("/app");

        // Prefix cho private message (SimpMessagingTemplate.convertAndSendToUser)
        registry.setUserDestinationPrefix("/user");
    }

    // ── Task 5: Đăng ký STOMP endpoints ──────────────────────────────────────

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        String[] allowedOrigins = Arrays.stream(frontendOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toArray(String[]::new);

        // Native WebSocket endpoint (dùng cho frontend Next.js với ws://)
        registry.addEndpoint("/ws")
                .setAllowedOrigins(allowedOrigins);

        // SockJS fallback endpoint (dùng khi ws:// bị block bởi proxy/firewall)
        registry.addEndpoint("/ws-sockjs")
                .setAllowedOrigins(allowedOrigins)
                .withSockJS();
    }

    // ── Task 5: Gắn JWT interceptor vào inbound channel ──────────────────────

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // WebSocketAuthInterceptor xác thực token tại bước STOMP CONNECT
        registration.interceptors(webSocketAuthInterceptor);
    }
}
