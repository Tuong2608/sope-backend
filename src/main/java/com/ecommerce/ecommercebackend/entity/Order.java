package com.ecommerce.ecommercebackend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A customer order, created from the contents of the user's cart.
 *
 * <p>{@code orderCode} is a human-readable, unique business key (e.g.
 * {@code ORD-20260604-AB12CD}). It is what the payment module references as
 * {@code Payment.orderId} (currently a decoupled String; to become a FK in
 * phase 2). Line items snapshot the product name and price at purchase time so
 * later catalog changes never alter historical orders.</p>
 */
@Entity
@Table(
        name = "orders",
        uniqueConstraints = @UniqueConstraint(name = "uk_orders_code", columnNames = "order_code")
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unique business key shared with the payment module. */
    @Column(name = "order_code", nullable = false, length = 100)
    private String orderCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(
            mappedBy = "order",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    /** Order grand total in VND: {@code subtotalAmount - discountAmount + shippingFee}. */
    @Column(name = "total_amount", nullable = false)
    private Long totalAmount;

    /** Sum of line totals before any discount or shipping fee (task D04). */
    @Column(name = "subtotal_amount", nullable = false)
    @Builder.Default
    private Long subtotalAmount = 0L;

    /** Total coupon discount applied to this order (task D04). */
    @Column(name = "discount_amount", nullable = false)
    @Builder.Default
    private Long discountAmount = 0L;

    /** Coupon code applied at checkout, if any (snapshot — coupon rules may change later). */
    @Column(name = "coupon_code", length = 50)
    private String couponCode;

    // ── Shipping details (task C06 — snapshot so later catalog/rate changes never alter history) ──

    @Column(name = "recipient_name", nullable = false, length = 100)
    private String recipientName;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(name = "shipping_address", nullable = false, length = 255)
    private String shippingAddress;

    /** Shipping method code used, e.g. "STANDARD"/"EXPRESS" (task C06). */
    @Column(name = "shipping_method_code", length = 30)
    private String shippingMethodCode;

    /** Shipping fee in VND, frozen at order time (task C06). */
    @Column(name = "shipping_fee")
    private Long shippingFee;

    @Column(name = "estimated_delivery_min_date")
    private LocalDate estimatedDeliveryMinDate;

    @Column(name = "estimated_delivery_max_date")
    private LocalDate estimatedDeliveryMaxDate;

    @Column(length = 500)
    private String note;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ── Convenience helper ────────────────────────────────────────────────────────

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }
}
