package com.ecommerce.ecommercebackend.service;

import com.ecommerce.ecommercebackend.dto.response.PagedResponse;
import com.ecommerce.ecommercebackend.dto.response.ProductResponse;
import com.ecommerce.ecommercebackend.entity.ColorVariant;
import com.ecommerce.ecommercebackend.entity.CrawledReview;
import com.ecommerce.ecommercebackend.entity.Product;
import com.ecommerce.ecommercebackend.entity.ProductStatus;
import com.ecommerce.ecommercebackend.entity.StorageVariant;
import com.ecommerce.ecommercebackend.repository.ProductRepository;
import com.ecommerce.ecommercebackend.repository.ProductVariantRepository;
import com.ecommerce.ecommercebackend.util.ProductImageUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductVariantRepository variantRepository;

    @Mock
    private ProductVariantService variantService;

    @Mock
    private ProductImageUtil imageUtil;

    @InjectMocks
    private ProductService productService;

    @Test
    void searchDetachesElementCollectionsBeforeReturningDto() {
        LinkedHashMap<String, String> specs = new LinkedHashMap<>();
        specs.put("CPU", "A17");
        List<StorageVariant> storageVariants = new ArrayList<>(
                List.of(new StorageVariant("256GB", "/iphone-256", true)));
        List<ColorVariant> colorVariants = new ArrayList<>(
                List.of(new ColorVariant("Blue", "/iphone-blue", "blue", "#00f", true)));
        List<CrawledReview> reviews = new ArrayList<>(
                List.of(new CrawledReview("An", "2026-07-23", "Good", 5.0)));

        Product product = Product.builder()
                .id(173L)
                .name("iPhone")
                .price(20_000_000L)
                .specs(specs)
                .storageVariants(storageVariants)
                .colorVariants(colorVariants)
                .reviews(reviews)
                .stockQuantity(10)
                .reservedQuantity(0)
                .minStockLevel(2)
                .status(ProductStatus.ACTIVE)
                .build();
        Pageable pageable = PageRequest.of(0, 10);

        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(product), pageable, 1));
        when(variantRepository.findByProductIdAndActiveTrue(173L)).thenReturn(List.of());

        PagedResponse<ProductResponse> response = productService.search(
                null, null, null, null, null, null, pageable);
        ProductResponse item = response.getContent().get(0);

        specs.clear();
        storageVariants.clear();
        colorVariants.clear();
        reviews.clear();

        assertThat(item.getSpecs()).containsEntry("CPU", "A17");
        assertThat(item.getStorageVariants()).hasSize(1);
        assertThat(item.getColorVariants()).hasSize(1);
        assertThat(item.getReviews()).hasSize(1);
    }
}
