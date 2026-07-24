package com.ecommerce.ecommercebackend.controller;

import com.ecommerce.ecommercebackend.dto.request.ChatbotRequest;
import com.ecommerce.ecommercebackend.dto.response.ProductResponse;
import com.ecommerce.ecommercebackend.service.ChatService;
import com.ecommerce.ecommercebackend.service.OrderChatService;
import com.ecommerce.ecommercebackend.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductChatbotIsolationTest {

    @Test
    void productDetailStillReturnsOkWhenChatbotReturnsServiceUnavailable() {
        ChatService chatService = mock(ChatService.class);
        OrderChatService orderChatService = mock(OrderChatService.class);
        RestTemplate chatRestTemplate = mock(RestTemplate.class);
        ChatController chatController =
                new ChatController(chatService, orderChatService, chatRestTemplate);
        ReflectionTestUtils.setField(chatController, "chatbotUrl", "http://chatbot");
        when(orderChatService.answer(null, "hello")).thenReturn(Optional.empty());
        when(chatRestTemplate.postForObject(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(
                        com.ecommerce.ecommercebackend.dto.response.ChatbotResponse.class)))
                .thenThrow(new ResourceAccessException("timeout"));
        ChatbotRequest request = new ChatbotRequest();
        request.setMessage("hello");

        assertThatThrownBy(() -> chatController.chat(request, null))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode().value()).isEqualTo(503));

        ProductService productService = mock(ProductService.class);
        ProductController productController = new ProductController(productService);
        ProductResponse product = ProductResponse.builder()
                .id(3L)
                .name("Phone")
                .price(10_000_000L)
                .build();
        when(productService.getById(3L)).thenReturn(product);

        var productResponse = productController.getById(3L);

        assertThat(productResponse.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(productResponse.getBody()).isSameAs(product);
    }
}
