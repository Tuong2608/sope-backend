package com.ecommerce.ecommercebackend.specification;

import com.ecommerce.ecommercebackend.entity.Product;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            List<String> terms = brandSearchTerms(brand);
            if (terms.isEmpty()) return cb.conjunction();

            Expression<String> brandValue = cb.lower(cb.coalesce(root.get("brand"), ""));
            Expression<String> nameValue = cb.lower(cb.coalesce(root.get("name"), ""));
            List<Predicate> predicates = new ArrayList<>();

            for (String term : terms) {
                String pattern = "%" + term + "%";
                predicates.add(cb.like(brandValue, pattern));
                predicates.add(cb.like(nameValue, pattern));
            }

            return cb.or(predicates.toArray(new Predicate[0]));
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

    private static List<String> brandSearchTerms(String brand) {
        String normalized = brand.toLowerCase().trim();
        Set<String> terms = new LinkedHashSet<>();
        addBrandTerm(terms, normalized);

        int openParenIndex = normalized.indexOf('(');
        if (openParenIndex > 0) {
            addBrandTerm(terms, normalized.substring(0, openParenIndex));
        }

        Matcher matcher = Pattern.compile("\\(([^)]+)\\)").matcher(normalized);
        while (matcher.find()) {
            addBrandTerm(terms, matcher.group(1));
        }

        if (normalized.contains("apple")) {
            addBrandTerm(terms, "iphone");
            addBrandTerm(terms, "ipad");
            addBrandTerm(terms, "macbook");
        }

        return new ArrayList<>(terms);
    }

    private static void addBrandTerm(Set<String> terms, String value) {
        String term = value
                .replace('(', ' ')
                .replace(')', ' ')
                .replaceAll("\\s+", " ")
                .trim();
        if (term.length() >= 2) {
            terms.add(term);
        }
    }
}
