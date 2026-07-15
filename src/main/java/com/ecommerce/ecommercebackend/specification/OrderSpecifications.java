package com.ecommerce.ecommercebackend.specification;

import com.ecommerce.ecommercebackend.entity.Order;
import com.ecommerce.ecommercebackend.entity.OrderStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

/**
 * Dynamic search/filter fragments for the admin order list (task H03).
 */
public final class OrderSpecifications {

    private OrderSpecifications() {
    }

    /** Matches the order code, recipient name (case-insensitive) or phone. */
    public static Specification<Order> keywordContains(String keyword) {
        return (root, query, cb) -> {
            if (isBlank(keyword)) return cb.conjunction();
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            Predicate byCode = cb.like(cb.lower(root.get("orderCode")), pattern);
            Predicate byRecipient = cb.like(cb.lower(root.get("recipientName")), pattern);
            Predicate byPhone = cb.like(root.get("phone"), "%" + keyword.trim() + "%");
            return cb.or(byCode, byRecipient, byPhone);
        };
    }

    public static Specification<Order> statusEquals(OrderStatus status) {
        return (root, query, cb) -> {
            if (status == null) return cb.conjunction();
            return cb.equal(root.get("status"), status);
        };
    }

    /** Inclusive range on {@code createdAt}; either bound may be {@code null}. */
    public static Specification<Order> createdBetween(LocalDateTime from, LocalDateTime to) {
        return (root, query, cb) -> {
            if (from == null && to == null) return cb.conjunction();
            if (from != null && to != null) {
                return cb.between(root.get("createdAt"), from, to);
            }
            return (from != null)
                    ? cb.greaterThanOrEqualTo(root.get("createdAt"), from)
                    : cb.lessThanOrEqualTo(root.get("createdAt"), to);
        };
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
