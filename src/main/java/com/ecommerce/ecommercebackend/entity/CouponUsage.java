package com.ecommerce.ecommercebackend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * One redemption record of a {@link Coupon} against an {@link Order} (task D02).
 *
 * <p>Tracks the hold → use/release lifecycle from D01/D06: a slot is
 * {@code HELD} when an order is placed with this coupon, becomes {@code USED}
 * once payment succeeds (counted in {@code Coupon.usedCount}), or
 * {@code RELEASED} if the order is cancelled/fails — freeing the slot back up.
 * The unique {@code (coupon_id, order_id)} constraint enforces the "one order,
 * one coupon" rule at the database level.</p>
 */
@Entity
@Table(
        name = "coupon_usages",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_coupon_usages_coupon_order",
                columnNames = {"coupon_id", "order_id"})
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CouponUsageStatus status = CouponUsageStatus.HELD;

    /** Discount amount (VND) computed at hold time; refunded/released amounts mirror this. */
    @Column(name = "discount_amount", nullable = false)
    private Long discountAmount;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
