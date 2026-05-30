package com.ecommerce.ecommercebackend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Request payload for creating/updating a product.
 *
 * <p>Field names mirror the TGDD crawl sample (snake_case via
 * {@link JsonProperty}) so a crawled JSON object can be POSTed directly.
 * Prices arrive as formatted strings (e.g. "7.890.000₫") and are parsed to
 * numeric VND in the service layer.</p>
 */
@Data
public class ProductRequest {

    @NotBlank(message = "Product name is required")
    private String name;

    private String category;

    private String brand;

    /** Formatted VND price, e.g. "7.890.000₫". */
    private String price;

    @JsonProperty("old_price")
    private String oldPrice;

    private String description;

    @JsonProperty("img_url")
    private String imgUrl;

    private String url;

    private Map<String, String> specs = new LinkedHashMap<>();
}
