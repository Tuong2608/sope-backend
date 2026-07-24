package com.ecommerce.ecommercebackend.controller;

import com.ecommerce.ecommercebackend.dto.response.ChatbotProductResponse;
import com.ecommerce.ecommercebackend.dto.response.PagedResponse;
import com.ecommerce.ecommercebackend.entity.ProductStatus;
import com.ecommerce.ecommercebackend.service.ProductService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InternalChatbotProductControllerTest {

    @Test
    void returnsPaginatedLightweightProjectionWithDefaultPageSize() {
        ProductService service = mock(ProductService.class);
        InternalChatbotProductController controller =
                new InternalChatbotProductController(service, "service-key");
        ChatbotProductResponse product = new ChatbotProductResponse(
                1L, "SKU-1", "Phone", "phone", "SOPE",
                "Flagship phone", 10_000_000L, 12_000_000L,
                8, 2, ProductStatus.ACTIVE);
        product.setSpecificationSummary("Chip xử lý: SOPE X1; RAM: 8 GB");
        PagedResponse<ChatbotProductResponse> expected = PagedResponse.<ChatbotProductResponse>builder()
                .content(List.of(product))
                .page(0)
                .size(15)
                .totalElements(1)
                .totalPages(1)
                .last(true)
                .build();
        when(service.getChatbotProducts(org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(expected);

        var response = controller.products("service-key", 0, 15);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(service).getChatbotProducts(pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(15);
        assertThat(response.getContent()).containsExactly(product);
        assertThat(product.getAvailableQuantity()).isEqualTo(6);
        assertThat(product.isInStock()).isTrue();
        assertThat(product.getShortDescription()).isEqualTo("Flagship phone");
        assertThat(product.getSpecificationSummary()).contains("SOPE X1");
    }

    @Test
    void dtoDoesNotExposeHeavyProductFields() {
        Set<String> fieldNames = Arrays.stream(ChatbotProductResponse.class.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toSet());

        assertThat(fieldNames).doesNotContain(
                "variants", "reviews", "images", "description", "specs",
                "storageVariants", "colorVariants");
    }

    @Test
    void rejectsMissingOrIncorrectServiceKey() {
        InternalChatbotProductController controller =
                new InternalChatbotProductController(mock(ProductService.class), "service-key");

        assertThatThrownBy(() -> controller.products(null, 0, 15))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> controller.products("wrong-key", 0, 15))
                .isInstanceOf(AccessDeniedException.class);
    }
}
