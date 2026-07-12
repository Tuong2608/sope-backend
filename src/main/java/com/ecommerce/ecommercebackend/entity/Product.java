package com.ecommerce.ecommercebackend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JPA entity representing a product in the catalog.
 *
 * <p>Modelled after the (updated) TGDD crawl schema. Prices are stored as plain
 * VND integers ({@code Long}) so they can be range-filtered efficiently; the
 * formatted source strings ("16.390.000₫" or "16490000.0") are parsed before
 * persistence.</p>
 *
 * <p>Variable-length collections (images, specs, storage/colour variants and the
 * crawled reviews) live in side tables via {@code @ElementCollection}. The
 * {@code name}/{@code price}/{@code imgUrl} accessors are kept stable so the cart
 * and order modules remain unaffected by the crawl schema change.</p>
 */
@Entity
@Table(name = "products")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "main_thumbnail", length = 500)
    @JsonProperty("main_thumbnail")
    private String mainThumbnail;
    /** Source SKU from the crawl (e.g. "363417"). */
    @Column(length = 50)
    @JsonProperty("sku")
    private String sku;

    /** Display name (crawl: {@code product_name}). */
    @Column(nullable = false, length = 255)
    @JsonProperty("product_name")
    private String name;

    /** Category slug from the crawl (e.g. "tablet", "laptop"). */
    @Column(length = 100)
    @JsonProperty("category")
    private String category;

    /** Brand, flattened from the crawl's nested array (e.g. "iPad (Apple)"). */
    @Column(length = 200)
    @JsonProperty("brand")
    private String brand;

    /** Short marketing blurb (crawl: {@code short_description}). */
    @Column(name = "short_description", columnDefinition = "TEXT")
    @JsonProperty("short_description")
    private String shortDescription;

    /** Full article/description (crawl: {@code detailed_article}). */
    @Column(columnDefinition = "LONGTEXT")
    @JsonProperty("detailed_article")
    private String description;

    /** Current selling price in VND (crawl: {@code current_price}). */
    @Column
    @JsonProperty("current_price")
    private Long price;

    /** Original/list price in VND (crawl: {@code original_price}). */
    @Column(name = "old_price")
    @JsonProperty("original_price")
    private Long oldPrice;

    @Column(length = 500)
    private String url;

    /** Primary thumbnail — first of {@link #images}; kept for cart/order display. */
    @Column(name = "img_url", length = 500)
    private String imgUrl;

    /** All product images (crawl: {@code infographic_images}). */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "product_images",
            joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "image_url", length = 500)
    @Builder.Default
    @JsonProperty("infographic_images")
    private List<String> images = new ArrayList<>();

    /** Technical specs (crawl: {@code detailed_specs}). */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "product_specs",
            joinColumns = @JoinColumn(name = "product_id"))
    @MapKeyColumn(name = "spec_key", length = 255)
    @Column(name = "spec_value", length = 1000)
    @Builder.Default
    @JsonProperty("detailed_specs")
    private Map<String, String> specs = new LinkedHashMap<>();

    /** Storage/configuration options (crawl: {@code storage_variants}). */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "product_storage_variants",
            joinColumns = @JoinColumn(name = "product_id"))
    @Builder.Default
    @JsonProperty("storage_variants")
    private List<StorageVariant> storageVariants = new ArrayList<>();

    /** Colour options (crawl: {@code color_variants}). */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "product_color_variants",
            joinColumns = @JoinColumn(name = "product_id"))
    @Builder.Default
    @JsonProperty("color_variants")
    private List<ColorVariant> colorVariants = new ArrayList<>();

    /** Snapshot of source-site reviews (crawl: {@code customer_reviews}). */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "product_reviews",
            joinColumns = @JoinColumn(name = "product_id"))
    @Builder.Default
    @JsonProperty("customer_reviews")
    private List<CrawledReview> reviews = new ArrayList<>();

    // ── B01: Inventory fields ─────────────────────────────────────────────────

    /**
     * Số lượng sản phẩm còn trong kho (chưa tính hàng đang giữ).
     * Công thức thực tế: available = stockQuantity - reservedQuantity
     */
    @Column(name = "stock_quantity", nullable = false)
    @Builder.Default
    private Integer stockQuantity = 0;

    /**
     * Số lượng đang được giữ tạm thời trong quá trình checkout.
     * Được cộng lên khi bắt đầu đặt hàng, trừ đi khi thanh toán thành công
     * hoặc khi reservation hết hạn.
     */
    @Column(name = "reserved_quantity", nullable = false)
    @Builder.Default
    private Integer reservedQuantity = 0;

    /**
     * Mức tồn kho tối thiểu — dùng để cảnh báo hàng sắp hết trên dashboard.
     * Khi stockQuantity <= minStockLevel thì hiển thị cảnh báo.
     */
    @Column(name = "min_stock_level", nullable = false)
    @Builder.Default
    private Integer minStockLevel = 5;

    /**
     * Trạng thái kinh doanh: ACTIVE (đang bán), INACTIVE (ngừng bán),
     * OUT_OF_STOCK (hết hàng tạm thời).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ProductStatus status = ProductStatus.ACTIVE;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ── Computed helper ───────────────────────────────────────────────────────

    /**
     * Số lượng thực sự có thể bán = stockQuantity - reservedQuantity.
     * Không âm — trả về 0 nếu reserved vượt stock.
     */
    public int getAvailableQuantity() {
        return Math.max(0, stockQuantity - reservedQuantity);
    }

    /** Trả về {@code true} nếu còn ít nhất 1 sản phẩm có thể bán. */
    public boolean isInStock() {
        return getAvailableQuantity() > 0;
    }

    /** Trả về {@code true} nếu số lượng còn lại <= mức tối thiểu. */
    public boolean isLowStock() {
        return getAvailableQuantity() <= minStockLevel;
    }
}
