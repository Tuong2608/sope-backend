package com.ecommerce.ecommercebackend.service;

import com.ecommerce.ecommercebackend.entity.Product;
import com.ecommerce.ecommercebackend.entity.ProductStatus;
import com.ecommerce.ecommercebackend.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * E07 – Service báo cáo tồn kho cho admin dashboard.
 *
 * <p>Cung cấp dữ liệu thống kê tồn kho để hiển thị trên dashboard:
 * <ul>
 *   <li>Sản phẩm sắp hết hàng (availableQty &lt;= minStockLevel)</li>
 *   <li>Sản phẩm đã hết hàng hoàn toàn (availableQty = 0)</li>
 *   <li>Sản phẩm đang ngừng bán (INACTIVE)</li>
 *   <li>Tổng quan kho hàng (tổng số SP, tổng có hàng, tổng hết hàng)</li>
 * </ul>
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryReportService {

    private final ProductRepository productRepository;

    // ── Report DTOs ───────────────────────────────────────────────────────────

    /** Thống kê tổng quan tồn kho. */
    public record InventoryOverview(
            long totalProducts,
            long activeProducts,
            long inStockProducts,
            long outOfStockProducts,
            long lowStockProducts,
            long inactiveProducts
    ) {}

    /** Thông tin một sản phẩm trong báo cáo tồn kho. */
    public record InventoryItemReport(
            Long   productId,
            String sku,
            String name,
            String category,
            String brand,
            Integer stockQuantity,
            Integer reservedQuantity,
            Integer availableQuantity,
            Integer minStockLevel,
            String  status,
            boolean lowStock
    ) {}

    // ── Overview ──────────────────────────────────────────────────────────────

    /**
     * E07 – Thống kê tổng quan tồn kho.
     */
    @Transactional(readOnly = true)
    public InventoryOverview getOverview() {
        List<Product> all = productRepository.findAll();

        long total    = all.size();
        long active   = all.stream().filter(p -> p.getStatus() == ProductStatus.ACTIVE).count();
        long inStock  = all.stream().filter(Product::isInStock).count();
        long outStock = all.stream().filter(p ->
                p.getStatus() == ProductStatus.OUT_OF_STOCK || p.getAvailableQuantity() == 0).count();
        long lowStock = all.stream().filter(Product::isLowStock).count();
        long inactive = all.stream().filter(p -> p.getStatus() == ProductStatus.INACTIVE).count();

        InventoryOverview ov = new InventoryOverview(total, active, inStock, outStock, lowStock, inactive);
        log.info("[E07] Overview: total={} active={} inStock={} outStock={} lowStock={} inactive={}",
                total, active, inStock, outStock, lowStock, inactive);
        return ov;
    }

    // ── Lists ─────────────────────────────────────────────────────────────────

    /**
     * E07 – Danh sách sản phẩm sắp hết hàng (availableQty &lt;= minStockLevel).
     */
    @Transactional(readOnly = true)
    public List<InventoryItemReport> getLowStockProducts() {
        return productRepository.findAll().stream()
                .filter(Product::isLowStock)
                .filter(p -> p.getStatus() != ProductStatus.INACTIVE)
                .sorted((a, b) -> Integer.compare(a.getAvailableQuantity(), b.getAvailableQuantity()))
                .map(this::toReport)
                .toList();
    }

    /**
     * E07 – Danh sách sản phẩm đã hết hàng hoàn toàn.
     */
    @Transactional(readOnly = true)
    public List<InventoryItemReport> getOutOfStockProducts() {
        return productRepository.findAll().stream()
                .filter(p -> p.getAvailableQuantity() == 0)
                .map(this::toReport)
                .toList();
    }

    /**
     * E07 – Danh sách sản phẩm đang ngừng bán (INACTIVE).
     */
    @Transactional(readOnly = true)
    public List<InventoryItemReport> getInactiveProducts() {
        return productRepository.findAll().stream()
                .filter(p -> p.getStatus() == ProductStatus.INACTIVE)
                .map(this::toReport)
                .toList();
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private InventoryItemReport toReport(Product p) {
        return new InventoryItemReport(
                p.getId(),
                p.getSku(),
                p.getName(),
                p.getCategory(),
                p.getBrand(),
                p.getStockQuantity(),
                p.getReservedQuantity(),
                p.getAvailableQuantity(),
                p.getMinStockLevel(),
                p.getStatus() != null ? p.getStatus().name() : "UNKNOWN",
                p.isLowStock()
        );
    }
}
