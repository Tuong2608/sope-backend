package com.ecommerce.ecommercebackend.specification;

import com.ecommerce.ecommercebackend.entity.Product;
import org.springframework.data.jpa.domain.Specification;

/**
 * Reusable {@link Specification} fragments for dynamic product querying.
 *
 * <p>Each method returns {@code null} when its argument is absent so callers
 * can compose them with {@code .and(...)} without null checks — a null
 * specification is treated as "always true" by Spring Data.</p>
 */
public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    /** Case-insensitive substring match on the product name. */
    public static Specification<Product> nameContains(String keyword) {
        if (isBlank(keyword)) {
            return null;
        }
        String pattern = "%" + keyword.toLowerCase() + "%";
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("name")), pattern);
    }

    /** Exact (case-insensitive) category match. */
    public static Specification<Product> categoryEquals(String category) {
        if (isBlank(category)) {
            return null;
        }
        return (root, query, cb) ->
                cb.equal(cb.lower(root.get("category")), category.toLowerCase());
    }

    /** Case-insensitive substring match on the brand (crawl values are noisy). */
    public static Specification<Product> brandContains(String brand) {
        if (isBlank(brand)) {
            return null;
        }
        String pattern = "%" + brand.toLowerCase() + "%";
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("brand")), pattern);
    }

    public static Specification<Product> priceGreaterThanOrEqual(Long minPrice) {
        if (minPrice == null) {
            return null;
        }
        return (root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("price"), minPrice);
    }

    public static Specification<Product> priceLessThanOrEqual(Long maxPrice) {
        if (maxPrice == null) {
            return null;
        }
        return (root, query, cb) ->
                cb.lessThanOrEqualTo(root.get("price"), maxPrice);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
