package com.ecommerce.ecommercebackend.service;

import com.ecommerce.ecommercebackend.entity.Product;
import com.ecommerce.ecommercebackend.entity.ProductStatus;
import com.ecommerce.ecommercebackend.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * A03 – Service lọc và nhập dữ liệu laptop hợp lệ vào database.
 *
 * <p>Được gọi bởi {@link com.ecommerce.ecommercebackend.config.LaptopDataSeeder}
 * khi khởi động hoặc qua admin API.</p>
 *
 * <h2>Tiêu chí lọc hợp lệ:</h2>
 * <ul>
 *   <li>Category phải là "laptop"</li>
 *   <li>Có tên sản phẩm (name không rỗng)</li>
 *   <li>Có giá hợp lệ (price > 0)</li>
 *   <li>Có brand không rỗng</li>
 *   <li>SKU chưa tồn tại trong DB (tránh trùng lặp)</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LaptopImportService {

    private final ProductRepository        productRepository;
    private final ProductDataValidationService validationService;

    // ── Import result ─────────────────────────────────────────────────────────

    public record ImportResult(
            int totalReceived,
            int imported,
            int skipped,
            List<String> skippedReasons
    ) {}

    // ── Import from raw data ──────────────────────────────────────────────────

    /**
     * Nhập danh sách laptop từ raw data (map của các trường).
     * Lọc các bản ghi không hợp lệ trước khi lưu.
     *
     * @param rawLaptops Danh sách map mỗi laptop (key = field name, value = giá trị)
     * @return Kết quả import
     */
    @Transactional
    public ImportResult importLaptops(List<Map<String, Object>> rawLaptops) {
        log.info("[A03] Bắt đầu import {} laptop records...", rawLaptops.size());

        // Lấy tập SKU đã có trong DB để tránh trùng lặp
        Set<String> existingSkus = new HashSet<>(
                productRepository.findAll().stream()
                        .map(Product::getSku)
                        .filter(Objects::nonNull)
                        .toList()
        );

        List<Product>  toSave        = new ArrayList<>();
        List<String>   skippedReasons = new ArrayList<>();
        int            skipped        = 0;

        for (Map<String, Object> raw : rawLaptops) {
            ValidationResult vr = validateRawLaptop(raw, existingSkus);

            if (!vr.valid()) {
                skippedReasons.add("SKU=" + raw.get("sku") + " | " + vr.reason());
                skipped++;
                continue;
            }

            Product product = buildProduct(raw);
            toSave.add(product);
            // Cập nhật existing SKUs để tránh trùng trong batch hiện tại
            if (product.getSku() != null) {
                existingSkus.add(product.getSku());
            }
        }

        if (!toSave.isEmpty()) {
            productRepository.saveAll(toSave);
        }

        log.info("[A03] Import hoàn tất: {} imported, {} skipped", toSave.size(), skipped);
        if (!skippedReasons.isEmpty()) {
            skippedReasons.forEach(r -> log.debug("[A03] Bỏ qua: {}", r));
        }

        return new ImportResult(rawLaptops.size(), toSave.size(), skipped, skippedReasons);
    }

    // ── Validation ────────────────────────────────────────────────────────────

    private record ValidationResult(boolean valid, String reason) {}

    private ValidationResult validateRawLaptop(Map<String, Object> raw, Set<String> existingSkus) {
        String sku      = getString(raw, "sku");
        String name     = getString(raw, "product_name");
        String category = getString(raw, "category");
        String brand    = getString(raw, "brand");
        Long   price    = getLong(raw, "current_price");

        if (name == null || name.isBlank()) {
            return new ValidationResult(false, "Thiếu tên sản phẩm");
        }
        if (!"laptop".equalsIgnoreCase(category)) {
            return new ValidationResult(false, "Category không phải laptop: " + category);
        }
        if (price == null || price <= 0) {
            return new ValidationResult(false, "Thiếu giá hoặc giá không hợp lệ: " + price);
        }
        if (brand == null || brand.isBlank()) {
            return new ValidationResult(false, "Thiếu brand");
        }
        if (sku != null && existingSkus.contains(sku)) {
            return new ValidationResult(false, "SKU đã tồn tại: " + sku);
        }
        return new ValidationResult(true, null);
    }

    // ── Builder ───────────────────────────────────────────────────────────────

    private Product buildProduct(Map<String, Object> raw) {
        String imgUrl = getString(raw, "img_url");
        // Ảnh dự phòng nếu thiếu (B10)
        if (imgUrl == null || imgUrl.isBlank()) {
            imgUrl = "/images/placeholder-laptop.png";
        }

        return Product.builder()
                .sku(getString(raw, "sku"))
                .name(getString(raw, "product_name"))
                .category("laptop")
                .brand(getString(raw, "brand"))
                .shortDescription(getString(raw, "short_description"))
                .description(getString(raw, "detailed_article"))
                .price(getLong(raw, "current_price"))
                .oldPrice(getLong(raw, "original_price"))
                .imgUrl(imgUrl)
                .url(getString(raw, "url"))
                .status(ProductStatus.ACTIVE)
                .stockQuantity(100) // Mặc định 100 khi import lần đầu
                .minStockLevel(5)
                .build();
    }

    // ── Type helpers ──────────────────────────────────────────────────────────

    private String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString().trim() : null;
    }

    private Long getLong(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val == null) return null;
        try {
            // Xử lý chuỗi có dấu chấm/phẩy như "16.490.000" hoặc "16490000.0"
            String str = val.toString().replaceAll("[^0-9.]", "");
            return (long) Double.parseDouble(str);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
