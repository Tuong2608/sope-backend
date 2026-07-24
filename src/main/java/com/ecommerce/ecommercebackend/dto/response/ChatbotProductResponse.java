package com.ecommerce.ecommercebackend.dto.response;

import com.ecommerce.ecommercebackend.entity.ProductStatus;
import lombok.Getter;
import lombok.Setter;

/**
 * Scalar catalog projection for the chatbot. Deliberately excludes lazy
 * collections, variants, reviews, images and long descriptions.
 */
@Getter
public class ChatbotProductResponse {

    private final Long id;
    private final String sku;
    private final String name;
    private final String category;
    private final String brand;
    private final String shortDescription;
    private final Long price;
    private final Long oldPrice;
    private final ProductStatus status;
    private final Integer availableQuantity;
    private final boolean inStock;
    @Setter
    private String specificationSummary;

    public ChatbotProductResponse(
            Long id,
            String sku,
            String name,
            String category,
            String brand,
            String shortDescription,
            Long price,
            Long oldPrice,
            Integer stockQuantity,
            Integer reservedQuantity,
            ProductStatus status) {
        this.id = id;
        this.sku = sku;
        this.name = name;
        this.category = category;
        this.brand = brand;
        this.shortDescription = truncate(shortDescription, 300);
        this.price = price;
        this.oldPrice = oldPrice;
        this.status = status;
        int stock = stockQuantity == null ? 0 : stockQuantity;
        int reserved = reservedQuantity == null ? 0 : reservedQuantity;
        this.availableQuantity = Math.max(0, stock - reserved);
        this.inStock = status == ProductStatus.ACTIVE && availableQuantity > 0;
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
