package com.ecommerce.ecommercebackend.config;

import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofSeconds(3))
                .readTimeout(Duration.ofSeconds(5))
                .build();
    }
}