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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

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
    // ── Read (List by IDs for Recommendation) ──────────────────────────────────

    @Transactional(readOnly = true)
    public List<ProductResponse> getProductsByIds(List<Long> ids) {
        return productRepository.findAllById(ids)
                .stream()
                .map(this::toResponse)
                .toList(); // Nếu dùng Java bản cũ hơn 16, dùng .collect(Collectors.toList())
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

        // 1. KHỞI TẠO SPEC LUÔN ĐÚNG (Tránh hoàn toàn lỗi Null)
        Specification<Product> spec = (root, query, cb) -> cb.conjunction();

        // 2. Nối các điều kiện (Code của bạn đã viết rất chuẩn phần này)
        if (keyword != null && !keyword.isBlank()) {
            spec = spec.and(ProductSpecifications.nameContains(keyword));
        }
        if (category != null && !category.isBlank()) {
            spec = spec.and(ProductSpecifications.categoryEquals(category));
        }
        if (brand != null && !brand.isBlank()) {
            spec = spec.and(ProductSpecifications.brandContains(brand));
        }
        if (minPrice != null) {
            spec = spec.and(ProductSpecifications.priceGreaterThanOrEqual(minPrice));
        }
        if (maxPrice != null) {
            spec = spec.and(ProductSpecifications.priceLessThanOrEqual(maxPrice));
        }

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

    /** Copies request fields onto the entity, parsing prices and flattening brand. */
    private void applyRequest(Product product, ProductRequest request) {
        product.setSku(request.getSku());
        product.setName(request.getName());
        product.setCategory(request.getCategory());
        product.setBrand(flattenBrand(request.getBrand()));
        product.setShortDescription(request.getShortDescription());
        product.setDescription(request.getDescription());
        product.setPrice(PriceParser.parse(request.getPrice()));
        product.setOldPrice(PriceParser.parse(request.getOldPrice()));
        product.setUrl(request.getUrl());

        List<String> images = (request.getImages() == null)
                ? new ArrayList<>()
                : new ArrayList<>(request.getImages());
        product.setImages(images);
        // Map trường mainThumbnail từ request vào entity
        product.setMainThumbnail(request.getMainThumbnail());

        product.setSpecs(request.getSpecs() == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(request.getSpecs()));
        product.setStorageVariants(request.getStorageVariants() == null
                ? new ArrayList<>()
                : new ArrayList<>(request.getStorageVariants()));
        product.setColorVariants(request.getColorVariants() == null
                ? new ArrayList<>()
                : new ArrayList<>(request.getColorVariants()));
        product.setReviews(request.getReviews() == null
                ? new ArrayList<>()
                : new ArrayList<>(request.getReviews()));
    }

    /** Flattens the crawl's nested brand array (e.g. [["iPad (Apple)"]]) to a string. */
    private String flattenBrand(List<List<String>> brand) {
        if (brand == null || brand.isEmpty()) {
            return null;
        }
        List<String> leaves = new ArrayList<>();
        for (List<String> inner : brand) {
            if (inner == null) {
                continue;
            }
            for (String value : inner) {
                if (value != null && !value.isBlank()) {
                    leaves.add(value.trim());
                }
            }
        }
        return leaves.isEmpty() ? null : String.join(", ", leaves);
    }

    private ProductResponse toResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .sku(product.getSku())
                .name(product.getName())
                .category(product.getCategory())
                .brand(product.getBrand())
                .shortDescription(product.getShortDescription())
                .description(product.getDescription())
                .price(product.getPrice())
                .oldPrice(product.getOldPrice())
                .url(product.getUrl())
                .mainThumbnail(product.getMainThumbnail()) // Đã cập nhật
                .images(product.getImages())
                .specs(product.getSpecs())
                .storageVariants(product.getStorageVariants())
                .colorVariants(product.getColorVariants())
                .reviews(product.getReviews())
                .build();
    }
}
