package com.ecommerce.ecommercebackend.config;

import com.ecommerce.ecommercebackend.entity.Role;
import com.ecommerce.ecommercebackend.entity.User;
import com.ecommerce.ecommercebackend.security.CustomUserDetailsService;
import com.ecommerce.ecommercebackend.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class WebSocketAuthInterceptorTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private MessageChannel messageChannel;

    @Test
    void adminCanSubscribeToAdminOrderTopic() {
        WebSocketAuthInterceptor interceptor =
                new WebSocketAuthInterceptor(jwtTokenProvider, userDetailsService);
        Message<byte[]> message = subscribeMessage("/topic/admin.orders", user(2L, Role.ROLE_ADMIN));

        assertThat(interceptor.preSend(message, messageChannel)).isSameAs(message);
    }

    @Test
    void regularUserCannotSubscribeToAdminOrderTopic() {
        WebSocketAuthInterceptor interceptor =
                new WebSocketAuthInterceptor(jwtTokenProvider, userDetailsService);
        Message<byte[]> message = subscribeMessage("/topic/admin.orders", user(2L, Role.ROLE_USER));

        assertThatThrownBy(() -> interceptor.preSend(message, messageChannel))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void userCanOnlySubscribeToOwnNotificationTopic() {
        WebSocketAuthInterceptor interceptor =
                new WebSocketAuthInterceptor(jwtTokenProvider, userDetailsService);
        User user = user(2L, Role.ROLE_USER);
        Message<byte[]> ownMessage = subscribeMessage("/topic/notification.2", user);
        Message<byte[]> otherMessage = subscribeMessage("/topic/notification.3", user);

        assertThat(interceptor.preSend(ownMessage, messageChannel)).isSameAs(ownMessage);
        assertThatThrownBy(() -> interceptor.preSend(otherMessage, messageChannel))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private Message<byte[]> subscribeMessage(String destination, User user) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(destination);
        accessor.setUser(new UsernamePasswordAuthenticationToken(
                user,
                null,
                user.getAuthorities()));
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private User user(Long id, Role role) {
        return User.builder()
                .id(id)
                .username("user-" + id)
                .email("user-" + id + "@example.com")
                .password("secret")
                .role(role)
                .build();
    }
}
