package com.ecommerce.ecommercebackend.service;

import com.ecommerce.ecommercebackend.dto.request.ProductRequest;
import com.ecommerce.ecommercebackend.dto.response.PagedResponse;
import com.ecommerce.ecommercebackend.dto.response.ProductResponse;
import com.ecommerce.ecommercebackend.entity.CrawledReview;
import com.ecommerce.ecommercebackend.entity.Product;
import com.ecommerce.ecommercebackend.entity.ProductStatus;
import com.ecommerce.ecommercebackend.exception.ResourceNotFoundException;
import com.ecommerce.ecommercebackend.repository.ProductRepository;
import com.ecommerce.ecommercebackend.repository.ProductVariantRepository;
import com.ecommerce.ecommercebackend.specification.ProductSpecifications;
import com.ecommerce.ecommercebackend.util.PriceParser;
import com.ecommerce.ecommercebackend.util.ProductImageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

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

    private final ProductRepository        productRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductVariantService    variantService;
    private final ProductImageUtil         imageUtil; // B10

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
        Product p = findOrThrow(id);
        // B03: Ẩn sản phẩm INACTIVE khi truy cập public
        if (p.getStatus() == ProductStatus.INACTIVE) {
            throw new ResourceNotFoundException("Product not found with id: " + id);
        }
        return toResponse(p);
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
            String storage,
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
        if (storage != null && !storage.isBlank()) {
            spec = spec.and(ProductSpecifications.storageContains(storage));
        }
        if (minPrice != null) {
            spec = spec.and(ProductSpecifications.priceGreaterThanOrEqual(minPrice));
        }
        if (maxPrice != null) {
            spec = spec.and(ProductSpecifications.priceLessThanOrEqual(maxPrice));
        }

        if (isRatingStarsSort(pageable)) {
            return searchSortedByRatingStars(spec, pageable);
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

    private PagedResponse<ProductResponse> searchSortedByRatingStars(
            Specification<Product> spec,
            Pageable pageable) {
        Sort.Direction direction = pageable.getSort().stream()
                .filter(order -> "ratingStars".equals(order.getProperty()))
                .map(Sort.Order::getDirection)
                .findFirst()
                .orElse(Sort.Direction.DESC);

        Comparator<Product> comparator = Comparator.comparingDouble(this::averageRatingStars);
        if (direction.isDescending()) {
            comparator = comparator.reversed();
        }
        comparator = comparator.thenComparing(Product::getId, Comparator.nullsLast(Long::compareTo));

        List<ProductResponse> products = productRepository.findAll(spec)
                .stream()
                .sorted(comparator)
                .map(this::toResponse)
                .toList();

        int start = (int) Math.min(pageable.getOffset(), products.size());
        int end = Math.min(start + pageable.getPageSize(), products.size());
        Page<ProductResponse> page = new PageImpl<>(products.subList(start, end), pageable, products.size());

        return PagedResponse.from(page);
    }

    private boolean isRatingStarsSort(Pageable pageable) {
        return pageable.getSort().stream()
                .anyMatch(order -> "ratingStars".equals(order.getProperty()));
    }

    private double averageRatingStars(Product product) {
        if (product.getReviews() == null || product.getReviews().isEmpty()) {
            return 0.0;
        }
        return product.getReviews().stream()
                .map(CrawledReview::getRatingStars)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
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
        // B03: Lấy danh sách variants đang active
        var variants = variantRepository.findByProductIdAndActiveTrue(product.getId())
                .stream().map(variantService::toResponse).toList();

        // B10: Chuẩn hóa ảnh
        String resolvedImg = imageUtil.resolveImageUrl(product.getImgUrl(), product.getCategory());
        var normalizedImages = imageUtil.normalizeImages(product.getImages(), resolvedImg);

        return ProductResponse.builder()
                .id(product.getId())
                .sku(product.getSku())
                .name(product.getName())
                .category(product.getCategory())
                .brand(resolveBrand(product))
                .shortDescription(product.getShortDescription())
                .description(product.getDescription())
                .price(product.getPrice())
                .oldPrice(product.getOldPrice())
                .url(product.getUrl())
                .mainThumbnail(product.getMainThumbnail())
                .imgUrl(resolvedImg)
                .images(normalizedImages)
                // Detach lazy Hibernate collections while the transaction is
                // still open. Returning PersistentMap/PersistentBag instances
                // makes JSON serialization fail after this method returns.
                .specs(product.getSpecs() == null
                        ? new LinkedHashMap<>()
                        : new LinkedHashMap<>(product.getSpecs()))
                .storageVariants(product.getStorageVariants() == null
                        ? new ArrayList<>()
                        : new ArrayList<>(product.getStorageVariants()))
                .colorVariants(product.getColorVariants() == null
                        ? new ArrayList<>()
                        : new ArrayList<>(product.getColorVariants()))
                .reviews(product.getReviews() == null
                        ? new ArrayList<>()
                        : new ArrayList<>(product.getReviews()))
                // B03: Inventory fields
                .status(product.getStatus())
                .availableQuantity(product.getAvailableQuantity())
                .inStock(product.isInStock())
                .lowStock(product.isLowStock())
                // B02/B03: Typed variants
                .variants(variants)
                .build();
    }

    private String resolveBrand(Product product) {
        if (product.getBrand() != null && !product.getBrand().isBlank()) {
            return product.getBrand().trim();
        }
        return inferBrandFromName(product.getName());
    }

    private String inferBrandFromName(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }

        String lowerName = name.toLowerCase();
        if (lowerName.contains("iphone")) return "iPhone (Apple)";
        if (lowerName.contains("ipad")) return "iPad (Apple)";
        if (lowerName.contains("macbook")) return "MacBook (Apple)";
        if (lowerName.contains("samsung")) return "Samsung";
        if (lowerName.contains("oppo")) return "OPPO";
        if (lowerName.contains("vivo")) return "Vivo";
        if (lowerName.contains("xiaomi")) return "Xiaomi";
        if (lowerName.contains("realme")) return "realme";
        if (lowerName.contains("honor")) return "HONOR";
        if (lowerName.contains("motorola")) return "Motorola";
        if (lowerName.contains("lenovo")) return "Lenovo";
        if (lowerName.contains("asus")) return "ASUS";
        if (lowerName.contains("acer")) return "Acer";
        if (lowerName.contains("msi")) return "MSI";
        if (lowerName.contains("dell")) return "Dell";
        if (lowerName.contains("hp ")) return "HP";

        return "";
    }
}
