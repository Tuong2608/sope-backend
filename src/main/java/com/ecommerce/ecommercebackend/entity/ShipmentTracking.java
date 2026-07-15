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
 * A simulated shipment for one {@link Order} (task C10) — generates a tracking
 * number and carries the current delivery status. Created the moment an order
 * transitions to {@code SHIPPING} (see {@code OrderService.updateStatus}).
 */
@Entity
@Table(
        name = "shipment_trackings",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_shipment_trackings_order", columnNames = "order_id"),
                @UniqueConstraint(name = "uk_shipment_trackings_number", columnNames = "tracking_number")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentTracking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /** Unique business key, e.g. "SPX7K2M9QX4A". */
    @Column(name = "tracking_number", nullable = false, length = 30)
    private String trackingNumber;

    /** Simulated carrier — a fixed demo value, not a real shipping partner. */
    @Column(name = "carrier_name", nullable = false, length = 100)
    @Builder.Default
    private String carrierName = "SOPE Express";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ShipmentStatus status = ShipmentStatus.CREATED;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
