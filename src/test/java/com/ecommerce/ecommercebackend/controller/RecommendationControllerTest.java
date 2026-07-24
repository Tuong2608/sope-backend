package com.ecommerce.ecommercebackend.controller;

import com.ecommerce.ecommercebackend.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RecommendationControllerTest {

    @Test
    void timeoutFallsBackToEmptyList() {
        ProductService productService = mock(ProductService.class);
        RestTemplate restTemplate = mock(RestTemplate.class);
        RecommendationController controller =
                new RecommendationController(productService, restTemplate);
        ReflectionTestUtils.setField(controller, "chatbotUrl", "http://chatbot");
        when(restTemplate.getForObject(anyString(), any(), any(Object[].class)))
                .thenThrow(new ResourceAccessException("read timed out"));

        var response = controller.getContentBasedSimilarProducts(3L, 5);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void chatbotBadGatewayFallsBackToEmptyList() {
        ProductService productService = mock(ProductService.class);
        RestTemplate restTemplate = mock(RestTemplate.class);
        RecommendationController controller =
                new RecommendationController(productService, restTemplate);
        ReflectionTestUtils.setField(controller, "chatbotUrl", "http://chatbot");
        when(restTemplate.getForObject(anyString(), any(), any(Object[].class)))
                .thenThrow(HttpServerErrorException.create(
                        HttpStatus.BAD_GATEWAY,
                        "Bad Gateway",
                        null,
                        null,
                        null));

        var response = controller.getContentBasedSimilarProducts(3L, 5);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEmpty();
    }
}
