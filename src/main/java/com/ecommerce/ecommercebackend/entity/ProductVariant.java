package com.ecommerce.ecommercebackend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * B02 – Phiên bản sản phẩm với màu + dung lượng riêng biệt.
 *
 * <p>Mỗi {@link ProductVariant} là một SKU con của một {@link Product}.
 * Ví dụ: iPhone 15 Pro / Màu Titan Tự Nhiên / 256GB.</p>
 *
 * <p>Mỗi variant có:
 * <ul>
 *   <li>Màu sắc ({@code colorName}, {@code colorHex})</li>
 *   <li>Dung lượng/cấu hình ({@code storageName})</li>
 *   <li>Giá riêng ({@code price}) — có thể khác giá base của Product</li>
 *   <li>Ảnh riêng ({@code imageUrl})</li>
 *   <li>Số lượng tồn kho riêng ({@code stockQuantity}, {@code reservedQuantity})</li>
 * </ul>
 * </p>
 */
@Entity
@Table(
        name = "product_variants",
        indexes = {
                @Index(name = "idx_variant_product", columnList = "product_id"),
                @Index(name = "idx_variant_sku",     columnList = "sku")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Product cha mà variant này thuộc về. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /**
     * SKU của variant này — dùng để định danh duy nhất.
     * Format gợi ý: {productSku}-{colorCode}-{storageCode}
     * Ví dụ: "363417-BLACK-256GB"
     */
    @Column(length = 100, unique = true)
    private String sku;

    // ── Màu sắc ────────────────────────────────────────────────────────────────

    /** Tên màu hiển thị, ví dụ: "Titan Tự Nhiên", "Đen". */
    @Column(name = "color_name", length = 100)
    private String colorName;

    /** Mã màu hex để hiển thị trên UI, ví dụ: "#F5F0E8". */
    @Column(name = "color_hex", length = 10)
    private String colorHex;

    // ── Dung lượng / cấu hình ─────────────────────────────────────────────────

    /** Tên dung lượng, ví dụ: "128GB", "256GB", "512GB". */
    @Column(name = "storage_name", length = 50)
    private String storageName;

    // ── Giá ────────────────────────────────────────────────────────────────────

    /**
     * Giá bán của variant này (VND).
     * Nếu null → dùng giá base của Product.
     */
    @Column(name = "price")
    private Long price;

    /** Giá gốc (trước giảm giá) của variant này (VND). */
    @Column(name = "old_price")
    private Long oldPrice;

    // ── Ảnh ────────────────────────────────────────────────────────────────────

    /** Ảnh đại diện của variant này (URL). */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    // ── Tồn kho ────────────────────────────────────────────────────────────────

    /** Số lượng tồn kho của variant này. */
    @Column(name = "stock_quantity", nullable = false)
    @Builder.Default
    private Integer stockQuantity = 0;

    /** Số lượng đang được giữ tạm trong checkout. */
    @Column(name = "reserved_quantity", nullable = false)
    @Builder.Default
    private Integer reservedQuantity = 0;

    // ── Trạng thái ─────────────────────────────────────────────────────────────

    /** Variant có đang được bán hay không. */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    // ── Timestamps ─────────────────────────────────────────────────────────────

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ── Computed helpers ───────────────────────────────────────────────────────

    /** Số lượng thực sự có thể bán = stockQuantity - reservedQuantity. */
    public int getAvailableQuantity() {
        return Math.max(0, stockQuantity - reservedQuantity);
    }

    /** {@code true} nếu variant còn hàng và đang được bán. */
    public boolean isAvailable() {
        return active && getAvailableQuantity() > 0;
    }

    /**
     * Giá hiệu dụng: dùng giá của variant nếu có, fallback về giá Product.
     *
     * @param productBasePrice Giá base của Product cha
     */
    public Long getEffectivePrice(Long productBasePrice) {
        return (price != null) ? price : productBasePrice;
    }
}
