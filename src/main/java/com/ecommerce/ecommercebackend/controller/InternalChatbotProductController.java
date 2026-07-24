package com.ecommerce.ecommercebackend.controller;

import com.ecommerce.ecommercebackend.dto.response.ChatbotProductResponse;
import com.ecommerce.ecommercebackend.dto.response.PagedResponse;
import com.ecommerce.ecommercebackend.service.ProductService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequestMapping("/api/internal/chatbot/products")
public class InternalChatbotProductController {

    private final ProductService productService;
    private final String serviceKey;

    public InternalChatbotProductController(
            ProductService productService,
            @Value("${app.chatbot.secret:}") String serviceKey) {
        this.productService = productService;
        this.serviceKey = serviceKey;
    }

    @GetMapping
    public PagedResponse<ChatbotProductResponse> products(
            @RequestHeader(value = "X-Service-Key", required = false) String suppliedKey,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        requireValidServiceKey(suppliedKey);
        var pageable = PageRequest.of(
                Math.max(page, 0),
                size < 1 ? 15 : Math.min(size, 30),
                Sort.by(Sort.Direction.ASC, "id"));
        return productService.getChatbotProducts(pageable);
    }

    private void requireValidServiceKey(String suppliedKey) {
        if (!StringUtils.hasText(serviceKey)
                || !StringUtils.hasText(suppliedKey)
                || !MessageDigest.isEqual(
                        serviceKey.getBytes(StandardCharsets.UTF_8),
                        suppliedKey.getBytes(StandardCharsets.UTF_8))) {
            throw new AccessDeniedException("Invalid chatbot service key");
        }
    }
}
