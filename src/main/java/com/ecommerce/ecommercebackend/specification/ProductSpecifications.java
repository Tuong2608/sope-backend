package com.ecommerce.ecommercebackend.specification;

import com.ecommerce.ecommercebackend.entity.Product;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    /** Case-insensitive substring match on the product name. */
    public static Specification<Product> nameContains(String keyword) {
        return (root, query, cb) -> {
            if (isBlank(keyword)) return cb.conjunction(); // Trả về "luôn đúng" thay vì null
            String pattern = "%" + keyword.toLowerCase() + "%";
            return cb.like(cb.lower(root.get("name")), pattern);
        };
    }

    /** Exact (case-insensitive) category match. */
    public static Specification<Product> categoryEquals(String category) {
        return (root, query, cb) -> {
            if (isBlank(category)) return cb.conjunction();
            return cb.equal(cb.lower(root.get("category")), category.toLowerCase());
        };
    }

    /** Case-insensitive substring match on the brand (crawl values are noisy). */
    public static Specification<Product> brandContains(String brand) {
        return (root, query, cb) -> {
            if (isBlank(brand)) return cb.conjunction();
            String pattern = "%" + brand.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("brand")), pattern),
                    cb.like(cb.lower(root.get("name")), pattern));
        };
    }

    public static Specification<Product> priceGreaterThanOrEqual(Long minPrice) {
        return (root, query, cb) -> {
            if (minPrice == null) return cb.conjunction();
            return cb.greaterThanOrEqualTo(root.get("price"), minPrice);
        };
    }

    public static Specification<Product> priceLessThanOrEqual(Long maxPrice) {
        return (root, query, cb) -> {
            if (maxPrice == null) return cb.conjunction();
            return cb.lessThanOrEqualTo(root.get("price"), maxPrice);
        };
    }

    public static Specification<Product> storageContains(String storage) {
        return (root, query, cb) -> {
            if (isBlank(storage)) return cb.conjunction();
            if (query != null) query.distinct(true);
            String pattern = "%" + storage.toLowerCase() + "%";
            return cb.like(
                    cb.lower(root.join("storageVariants", JoinType.LEFT).get("storageName")),
                    pattern);
        };
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
