package com.ecommerce.ecommercebackend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * A discount coupon (tasks D01/D02).
 *
 * <p>Encodes the agreed coupon rules (D01):
 * <ul>
 *   <li>One order may use at most one coupon — enforced by the unique
 *       {@code (coupon_id, order_id)} constraint on {@link CouponUsage}.</li>
 *   <li>{@code discountType} picks whether {@code discountValue} is a percentage
 *       (0–100) or a fixed VND amount.</li>
 *   <li>{@code scope} picks whether the discount applies to the whole order, to
 *       specific products ({@code applicableProductIds}), or to specific
 *       categories ({@code applicableCategories}).</li>
 * </ul>
 * Application/validation logic (checking limits, computing the discount) is
 * implemented in later tasks (D03–D06); this entity only holds the rules and
 * the running {@code usedCount}.</p>
 */
@Entity
@Table(
        name = "coupons",
        uniqueConstraints = @UniqueConstraint(name = "uk_coupons_code", columnNames = "code")
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Case-insensitive in practice — callers should normalise to uppercase. */
    @Column(nullable = false, length = 50)
    private String code;

    @Column(length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private DiscountType discountType;

    /** Percentage (0–100) when {@code discountType == PERCENTAGE}, else a VND amount. */
    @Column(name = "discount_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountValue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    @Builder.Default
    private CouponScope scope = CouponScope.ALL_ORDER;

    /** Populated only when {@code scope == SPECIFIC_PRODUCTS}. */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "coupon_applicable_products",
            joinColumns = @JoinColumn(name = "coupon_id"))
    @Column(name = "product_id")
    @Builder.Default
    private Set<Long> applicableProductIds = new HashSet<>();

    /** Populated only when {@code scope == SPECIFIC_CATEGORIES}. */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "coupon_applicable_categories",
            joinColumns = @JoinColumn(name = "coupon_id"))
    @Column(name = "category", length = 100)
    @Builder.Default
    private Set<String> applicableCategories = new HashSet<>();

    /** Minimum order subtotal (VND) required to use this coupon; {@code null} = no minimum. */
    @Column(name = "min_order_amount")
    private Long minOrderAmount;

    /** Cap on the discount amount (VND), mainly to bound PERCENTAGE coupons; {@code null} = no cap. */
    @Column(name = "max_discount_amount")
    private Long maxDiscountAmount;

    /** Total number of times this coupon may be used across all users; {@code null} = unlimited. */
    @Column(name = "usage_limit")
    private Integer usageLimit;

    /** Number of times a single user may use this coupon; {@code null} = unlimited. */
    @Column(name = "usage_limit_per_user")
    private Integer usageLimitPerUser;

    /** Running count of {@code USED} redemptions (held-but-not-used slots don't count here). */
    @Column(name = "used_count", nullable = false)
    @Builder.Default
    private int usedCount = 0;

    /** {@code null} = valid immediately. */
    @Column(name = "start_at")
    private LocalDateTime startAt;

    /** {@code null} = never expires. */
    @Column(name = "end_at")
    private LocalDateTime endAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ── Rule helpers (D01) — reused by the D03–D06 application logic ────────────

    /** {@code true} if {@code now} falls within [{@code startAt}, {@code endAt}] (open bounds allowed). */
    public boolean isWithinValidPeriod(LocalDateTime now) {
        if (startAt != null && now.isBefore(startAt)) {
            return false;
        }
        return endAt == null || !now.isAfter(endAt);
    }

    /** {@code true} if the coupon has a total usage cap and it has been reached. */
    public boolean hasReachedUsageLimit() {
        return usageLimit != null && usedCount >= usageLimit;
    }
}
