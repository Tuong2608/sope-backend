package com.ecommerce.ecommercebackend.config;

import com.ecommerce.ecommercebackend.security.JwtTokenProvider;
import com.ecommerce.ecommercebackend.security.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Interceptor xác thực JWT khi client kết nối WebSocket qua STOMP.
 *
 * <p>Khi client gửi frame CONNECT, interceptor này đọc token từ header
 * {@code Authorization} (hoặc query param {@code token}), xác thực bằng
 * {@link JwtTokenProvider}, sau đó gắn {@link UsernamePasswordAuthenticationToken}
 * vào header message để Spring Security nhận diện user trên WebSocket session.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider        jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) return message;

        // Chỉ xác thực khi client gửi CONNECT
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = extractToken(accessor);

            if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
                String username = jwtTokenProvider.getUsernameFromToken(token);
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                accessor.setUser(auth);
                log.info("[WS] User '{}' kết nối WebSocket thành công", username);
            } else {
                log.warn("[WS] CONNECT bị từ chối: token không hợp lệ hoặc thiếu token");
            }
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            String destination = accessor.getDestination();
            if (destination != null && destination.startsWith("/topic/chat.")) {
                java.security.Principal principal = accessor.getUser();
                if (principal == null) {
                    throw new IllegalArgumentException("Từ chối SUBSCRIBE: Chưa xác thực (No Principal)");
                }
                
                org.springframework.security.authentication.UsernamePasswordAuthenticationToken auth =
                        (org.springframework.security.authentication.UsernamePasswordAuthenticationToken) principal;
                Object authPrincipal = auth.getPrincipal();
                
                if (authPrincipal instanceof com.ecommerce.ecommercebackend.entity.User user) {
                    if (user.getRole() != com.ecommerce.ecommercebackend.entity.Role.ROLE_ADMIN) {
                        String userIdStr = String.valueOf(user.getId());
                        if (!destination.contains("_" + userIdStr + "_") && !destination.endsWith("_" + userIdStr)) {
                            log.warn("[WS] Từ chối SUBSCRIBE: User {} không có quyền truy cập {}", user.getUsername(), destination);
                            throw new IllegalArgumentException("Từ chối SUBSCRIBE: Bạn không có quyền truy cập phòng này");
                        }
                    }
                } else {
                    throw new IllegalArgumentException("Từ chối SUBSCRIBE: Principal không hợp lệ");
                }
            }
        }
        return message;
    }

    /** Lấy token từ header Authorization hoặc native header "token". */
    private String extractToken(StompHeaderAccessor accessor) {
        // Ưu tiên header Authorization: Bearer <token>
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        // Fallback: native header "token" (dùng với SockJS)
        return accessor.getFirstNativeHeader("token");
    }
}
