package com.ecommerce.ecommercebackend.config;

import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Khai báo bean {@link RestTemplate} dùng chung cho các controller cần gọi
 * ra service ngoài (ví dụ {@code RecommendationController} gọi sang
 * FastAPI). Có timeout để tránh treo request nếu service Python không phản
 * hồi.
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    @Primary
    public RestTemplate restTemplate(
            RestTemplateBuilder builder,
            @Value("${app.http.connect-timeout:3s}") Duration connectTimeout,
            @Value("${app.http.read-timeout:30s}") Duration readTimeout) {
        return builder
                .connectTimeout(connectTimeout)
                .readTimeout(readTimeout)
                .build();
    }

    @Bean("chatbotChatRestTemplate")
    public RestTemplate chatbotChatRestTemplate(
            RestTemplateBuilder builder,
            @Value("${app.chatbot.chat.connect-timeout:5s}") Duration connectTimeout,
            @Value("${app.chatbot.chat.read-timeout:75s}") Duration readTimeout) {
        return builder
                .connectTimeout(connectTimeout)
                .readTimeout(readTimeout)
                .build();
    }

    @Bean("chatbotRecommendationRestTemplate")
    public RestTemplate chatbotRecommendationRestTemplate(
            RestTemplateBuilder builder,
            @Value("${app.chatbot.recommendation.connect-timeout:5s}") Duration connectTimeout,
            @Value("${app.chatbot.recommendation.read-timeout:15s}") Duration readTimeout) {
        return builder
                .connectTimeout(connectTimeout)
                .readTimeout(readTimeout)
                .build();
    }
}
