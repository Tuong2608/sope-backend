package com.ecommerce.ecommercebackend.dto.request;

import com.ecommerce.ecommercebackend.entity.ColorVariant;
import com.ecommerce.ecommercebackend.entity.CrawledReview;
import com.ecommerce.ecommercebackend.entity.StorageVariant;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Request payload for creating/updating a product.
 *
 * <p>Field names mirror the (updated) TGDD crawl schema so a crawled JSON object
 * can be POSTed directly. Prices arrive as formatted strings and are parsed to
 * numeric VND in the service layer; the brand arrives as a nested array and is
 * flattened there. Unknown crawl fields (e.g. {@code service_packages}) are
 * ignored so ingestion never breaks on extra keys.</p>
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductRequest {

    private String sku;

    @NotBlank(message = "Product name is required")
    @JsonProperty("product_name")
    private String name;

    private String category;

    /** Crawl brand is nested, e.g. {@code [["iPad (Apple)"]]}; flattened in service. */
    private List<List<String>> brand = new ArrayList<>();

    @JsonProperty("short_description")
    private String shortDescription;

    /** Full article text (crawl: {@code detailed_article}). */
    @JsonProperty("detailed_article")
    private String description;

    /** Current price, formatted (crawl: {@code current_price}). */
    @JsonProperty("current_price")
    private String price;

    /** Original price, formatted (crawl: {@code original_price}). */
    @JsonProperty("original_price")
    private String oldPrice;

    private String url;

    @JsonProperty("infographic_images")
    private List<String> images = new ArrayList<>();

    @JsonProperty("detailed_specs")
    private Map<String, String> specs = new LinkedHashMap<>();

    @JsonProperty("storage_variants")
    private List<StorageVariant> storageVariants = new ArrayList<>();

    @JsonProperty("color_variants")
    private List<ColorVariant> colorVariants = new ArrayList<>();

    @JsonProperty("customer_reviews")
    private List<CrawledReview> reviews = new ArrayList<>();
}
