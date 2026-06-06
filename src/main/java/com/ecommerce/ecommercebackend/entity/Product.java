package com.ecommerce.ecommercebackend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    /** Source SKU from the crawl (e.g. "363417"). */
    @Column(length = 50)
    private String sku;

    /** Display name (crawl: {@code product_name}). */
    @Column(nullable = false, length = 255)
    private String name;

    /** Category slug from the crawl (e.g. "tablet", "laptop"). */
    @Column(length = 100)
    private String category;

    /** Brand, flattened from the crawl's nested array (e.g. "iPad (Apple)"). */
    @Column(length = 200)
    private String brand;

    /** Short marketing blurb (crawl: {@code short_description}). */
    @Column(name = "short_description", columnDefinition = "TEXT")
    private String shortDescription;

    /** Full article/description (crawl: {@code detailed_article}). */
    @Column(columnDefinition = "LONGTEXT")
    private String description;

    /** Current selling price in VND (crawl: {@code current_price}). */
    @Column
    private Long price;

    /** Original/list price in VND (crawl: {@code original_price}). */
    @Column(name = "old_price")
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
    private List<String> images = new ArrayList<>();

    /** Technical specs (crawl: {@code detailed_specs}). */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "product_specs",
            joinColumns = @JoinColumn(name = "product_id"))
    @MapKeyColumn(name = "spec_key", length = 255)
    @Column(name = "spec_value", length = 1000)
    @Builder.Default
    private Map<String, String> specs = new LinkedHashMap<>();

    /** Storage/configuration options (crawl: {@code storage_variants}). */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "product_storage_variants",
            joinColumns = @JoinColumn(name = "product_id"))
    @Builder.Default
    private List<StorageVariant> storageVariants = new ArrayList<>();

    /** Colour options (crawl: {@code color_variants}). */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "product_color_variants",
            joinColumns = @JoinColumn(name = "product_id"))
    @Builder.Default
    private List<ColorVariant> colorVariants = new ArrayList<>();

    /** Snapshot of source-site reviews (crawl: {@code customer_reviews}). */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "product_reviews",
            joinColumns = @JoinColumn(name = "product_id"))
    @Builder.Default
    private List<CrawledReview> reviews = new ArrayList<>();
}
