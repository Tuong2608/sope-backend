package com.ecommerce.ecommercebackend.service;

import com.ecommerce.ecommercebackend.entity.Product;
import com.ecommerce.ecommercebackend.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A04 – Service kiểm tra và phát hiện lỗi dữ liệu sản phẩm trong DB.
 *
 * <p>Các loại lỗi được phát hiện:
 * <ol>
 *   <li>SKU trùng lặp</li>
 *   <li>Thiếu giá (price = null hoặc = 0)</li>
 *   <li>Thiếu ảnh (imgUrl = null hoặc rỗng)</li>
 *   <li>Thiếu brand (brand = null hoặc rỗng)</li>
 *   <li>Sai loại sản phẩm (category không hợp lệ)</li>
 * </ol>
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductDataValidationService {

    private final ProductRepository productRepository;

    /** Danh sách category hợp lệ trong hệ thống. */
    private static final Set<String> VALID_CATEGORIES = Set.of(
            "dien-thoai", "may-tinh-bang", "laptop"
    );

    // ── Report object ─────────────────────────────────────────────────────────

    /**
     * Kết quả kiểm tra toàn bộ dữ liệu sản phẩm.
     */
    public record ValidationReport(
            int totalProducts,
            List<String> duplicateSkus,
            List<Long>   missingPriceIds,
            List<Long>   missingImageIds,
            List<Long>   missingBrandIds,
            List<Long>   invalidCategoryIds,
            boolean      hasErrors
    ) {
        /** Tổng số lỗi phát hiện được. */
        public int errorCount() {
            return duplicateSkus.size()
                    + missingPriceIds.size()
                    + missingImageIds.size()
                    + missingBrandIds.size()
                    + invalidCategoryIds.size();
        }

        /** Tóm tắt ngắn gọn để log. */
        public String summary() {
            return String.format(
                    "[A04 Validation] Total=%d | Errors=%d (dupSKU=%d, noPrice=%d, noImage=%d, noBrand=%d, badCategory=%d)",
                    totalProducts, errorCount(),
                    duplicateSkus.size(), missingPriceIds.size(),
                    missingImageIds.size(), missingBrandIds.size(),
                    invalidCategoryIds.size()
            );
        }
    }

    // ── Main validation ───────────────────────────────────────────────────────

    /**
     * Chạy toàn bộ kiểm tra và trả về báo cáo lỗi.
     *
     * @return {@link ValidationReport} chứa danh sách sản phẩm bị lỗi
     */
    @Transactional(readOnly = true)
    public ValidationReport validate() {
        List<Product> all = productRepository.findAll();

        List<String> duplicateSkus     = findDuplicateSkus(all);
        List<Long>   missingPriceIds   = findMissingPrice(all);
        List<Long>   missingImageIds   = findMissingImage(all);
        List<Long>   missingBrandIds   = findMissingBrand(all);
        List<Long>   invalidCategoryIds = findInvalidCategory(all);

        boolean hasErrors = !duplicateSkus.isEmpty()
                || !missingPriceIds.isEmpty()
                || !missingImageIds.isEmpty()
                || !missingBrandIds.isEmpty()
                || !invalidCategoryIds.isEmpty();

        ValidationReport report = new ValidationReport(
                all.size(),
                duplicateSkus, missingPriceIds, missingImageIds,
                missingBrandIds, invalidCategoryIds,
                hasErrors
        );

        log.info(report.summary());
        if (hasErrors) {
            log.warn("[A04] Phát hiện {} lỗi dữ liệu sản phẩm — xem ValidationReport để biết chi tiết", report.errorCount());
        }
        return report;
    }

    // ── Individual checks ─────────────────────────────────────────────────────

    /**
     * Phát hiện SKU bị trùng lặp.
     *
     * @return Danh sách SKU xuất hiện nhiều hơn 1 lần
     */
    public List<String> findDuplicateSkus(List<Product> products) {
        Map<String, Long> skuCount = products.stream()
                .filter(p -> p.getSku() != null && !p.getSku().isBlank())
                .collect(Collectors.groupingBy(Product::getSku, Collectors.counting()));

        List<String> duplicates = skuCount.entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .map(Map.Entry::getKey)
                .sorted()
                .toList();

        if (!duplicates.isEmpty()) {
            log.warn("[A04] SKU trùng lặp: {}", duplicates);
        }
        return duplicates;
    }

    /**
     * Phát hiện sản phẩm thiếu giá.
     *
     * @return Danh sách ID sản phẩm có price = null hoặc <= 0
     */
    public List<Long> findMissingPrice(List<Product> products) {
        return products.stream()
                .filter(p -> p.getPrice() == null || p.getPrice() <= 0)
                .map(Product::getId)
                .sorted()
                .toList();
    }

    /**
     * Phát hiện sản phẩm thiếu ảnh đại diện.
     *
     * @return Danh sách ID sản phẩm có imgUrl = null hoặc rỗng
     */
    public List<Long> findMissingImage(List<Product> products) {
        return products.stream()
                .filter(p -> p.getImgUrl() == null || p.getImgUrl().isBlank())
                .map(Product::getId)
                .sorted()
                .toList();
    }

    /**
     * Phát hiện sản phẩm thiếu thông tin hãng sản xuất.
     *
     * @return Danh sách ID sản phẩm có brand = null hoặc rỗng
     */
    public List<Long> findMissingBrand(List<Product> products) {
        return products.stream()
                .filter(p -> p.getBrand() == null || p.getBrand().isBlank())
                .map(Product::getId)
                .sorted()
                .toList();
    }

    /**
     * Phát hiện sản phẩm có category không hợp lệ.
     *
     * @return Danh sách ID sản phẩm có category không nằm trong {@link #VALID_CATEGORIES}
     */
    public List<Long> findInvalidCategory(List<Product> products) {
        return products.stream()
                .filter(p -> p.getCategory() == null
                        || !VALID_CATEGORIES.contains(p.getCategory().toLowerCase()))
                .map(Product::getId)
                .sorted()
                .toList();
    }
}
