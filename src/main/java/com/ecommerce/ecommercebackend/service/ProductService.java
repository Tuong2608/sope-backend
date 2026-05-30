package com.ecommerce.ecommercebackend.service;

import com.ecommerce.ecommercebackend.dto.request.ProductRequest;
import com.ecommerce.ecommercebackend.dto.response.PagedResponse;
import com.ecommerce.ecommercebackend.dto.response.ProductResponse;
import com.ecommerce.ecommercebackend.entity.Product;
import com.ecommerce.ecommercebackend.exception.ResourceNotFoundException;
import com.ecommerce.ecommercebackend.repository.ProductRepository;
import com.ecommerce.ecommercebackend.specification.ProductSpecifications;
import com.ecommerce.ecommercebackend.util.PriceParser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;

/**
 * Application service encapsulating product catalog business logic:
 * CRUD plus keyword search, attribute filtering and pagination.
 *
 * <p>Controllers stay thin (HTTP concerns only); all domain logic and
 * entity/DTO mapping lives here.</p>
 */
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    // ── Create ────────────────────────────────────────────────────────────────

    @Transactional
    public ProductResponse create(ProductRequest request) {
        Product product = new Product();
        applyRequest(product, request);
        return toResponse(productRepository.save(product));
    }

    // ── Read (single) ───────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    // ── Read (search / filter / paginate) ───────────────────────────────────────

    /**
     * Returns a page of products matching the optional keyword and filters.
     * Any {@code null}/blank argument is ignored (matches everything).
     */
    @Transactional(readOnly = true)
    public PagedResponse<ProductResponse> search(
            String keyword,
            String category,
            String brand,
            Long minPrice,
            Long maxPrice,
            Pageable pageable) {

        Specification<Product> spec = Specification
                .allOf(ProductSpecifications.nameContains(keyword))
                .and(ProductSpecifications.categoryEquals(category))
                .and(ProductSpecifications.brandContains(brand))
                .and(ProductSpecifications.priceGreaterThanOrEqual(minPrice))
                .and(ProductSpecifications.priceLessThanOrEqual(maxPrice));

        Page<ProductResponse> page = productRepository.findAll(spec, pageable)
                .map(this::toResponse);

        return PagedResponse.from(page);
    }

    // ── Update ──────────────────────────────────────────────────────────────────

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = findOrThrow(id);
        applyRequest(product, request);
        return toResponse(productRepository.save(product));
    }

    // ── Delete ──────────────────────────────────────────────────────────────────

    @Transactional
    public void delete(Long id) {
        Product product = findOrThrow(id);
        productRepository.delete(product);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    private Product findOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with id: " + id));
    }

    /** Copies request fields onto the entity, parsing formatted prices. */
    private void applyRequest(Product product, ProductRequest request) {
        product.setName(request.getName());
        product.setCategory(request.getCategory());
        product.setBrand(request.getBrand());
        product.setPrice(PriceParser.parse(request.getPrice()));
        product.setOldPrice(PriceParser.parse(request.getOldPrice()));
        product.setDescription(request.getDescription());
        product.setImgUrl(request.getImgUrl());
        product.setUrl(request.getUrl());
        product.setSpecs(request.getSpecs() == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(request.getSpecs()));
    }

    private ProductResponse toResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .category(product.getCategory())
                .brand(product.getBrand())
                .price(product.getPrice())
                .oldPrice(product.getOldPrice())
                .description(product.getDescription())
                .imgUrl(product.getImgUrl())
                .url(product.getUrl())
                .specs(product.getSpecs())
                .build();
    }
}
