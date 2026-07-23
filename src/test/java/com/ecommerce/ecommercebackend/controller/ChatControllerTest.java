package com.ecommerce.ecommercebackend.controller;

import com.ecommerce.ecommercebackend.dto.request.ChatbotRequest;
import com.ecommerce.ecommercebackend.dto.response.ChatbotResponse;
import com.ecommerce.ecommercebackend.entity.Role;
import com.ecommerce.ecommercebackend.entity.User;
import com.ecommerce.ecommercebackend.service.ChatService;
import com.ecommerce.ecommercebackend.service.OrderChatService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock
    private ChatService chatService;

    @Mock
    private OrderChatService orderChatService;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private ChatController chatController;

    @Test
    void personalOrderAnswerNeverLeavesSpringBackend() {
        User user = User.builder()
                .id(7L)
                .username("customer")
                .email("customer@example.com")
                .password("secret")
                .role(Role.ROLE_USER)
                .build();
        ChatbotRequest request = new ChatbotRequest();
        request.setMessage("Đơn gần nhất của tôi đang ở đâu?");
        when(orderChatService.answer(user, request.getMessage()))
                .thenReturn(Optional.of("Đơn của bạn đang giao."));

        ResponseEntity<ChatbotResponse> response = chatController.chat(request, user);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getReply()).isEqualTo("Đơn của bạn đang giao.");
        verifyNoInteractions(restTemplate);
    }
}
