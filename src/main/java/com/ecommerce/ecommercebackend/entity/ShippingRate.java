package com.ecommerce.ecommercebackend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The fee and expected delivery window for one (zone, method) pair (task C02).
 * E.g. EXPRESS to the inner-city zone costs less and arrives faster than
 * EXPRESS to a remote zone.
 */
@Entity
@Table(
        name = "shipping_rates",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_shipping_rates_zone_method",
                columnNames = {"zone_id", "method_id"})
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id", nullable = false)
    private ShippingZone zone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "method_id", nullable = false)
    private ShippingMethod method;

    /** Shipping fee in VND. */
    @Column(nullable = false)
    private Long fee;

    /** Minimum/maximum expected transit days (business days, before holiday adjustment). */
    @Column(name = "min_days", nullable = false)
    private int minDays;

    @Column(name = "max_days", nullable = false)
    private int maxDays;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
