package com.ecommerce.ecommercebackend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A line in an {@link Order}. Product name and unit price are <em>snapshots</em>
 * taken when the order was placed, so the order is immune to later catalog edits.
 * {@code productId} keeps a soft reference back to the catalog for convenience.
 */
@Entity
@Table(name = "order_items")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /** Soft reference to the originating product (no FK constraint enforced). */
    @Column(name = "product_id")
    private Long productId;

    /** Product name at purchase time. */
    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    /** Unit price in VND at purchase time. */
    @Column(name = "unit_price", nullable = false)
    private Long unitPrice;

    @Column(nullable = false)
    private int quantity;

    /** {@code unitPrice * quantity}. */
    @Column(name = "line_total", nullable = false)
    private Long lineTotal;
}
