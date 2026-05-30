package com.ecommerce.ecommercebackend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JPA entity representing a product in the catalog.
 *
 * <p>Modelled after the TGDD crawl sample. Prices are stored as plain VND
 * integers ({@code Long}) so they can be range-filtered efficiently; the
 * formatted source strings (e.g. "7.890.000₫") are parsed before persistence.</p>
 *
 * <p>{@code specs} holds the variable key/value technical specifications in a
 * side table ({@code product_specs}); {@link LinkedHashMap} preserves the
 * original crawl ordering.</p>
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

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 100)
    private String category;

    @Column(length = 150)
    private String brand;

    /** Current selling price in VND. */
    @Column
    private Long price;

    /** Original/list price in VND before discount (nullable). */
    @Column(name = "old_price")
    private Long oldPrice;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "img_url", length = 500)
    private String imgUrl;

    @Column(length = 500)
    private String url;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "product_specs",
            joinColumns = @JoinColumn(name = "product_id")
    )
    @MapKeyColumn(name = "spec_key", length = 150)
    @Column(name = "spec_value", length = 1000)
    @Builder.Default
    private Map<String, String> specs = new LinkedHashMap<>();
}
