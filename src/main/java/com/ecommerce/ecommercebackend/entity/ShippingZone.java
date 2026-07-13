package com.ecommerce.ecommercebackend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 * A delivery region matched by province name (task C02).
 *
 * <p>{@code priority} breaks ties when a province could match more than one
 * zone (e.g. a city belongs to both a narrow "inner city" zone and a broader
 * "region" zone) — the lowest priority value wins.</p>
 */
@Entity
@Table(name = "shipping_zones")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingZone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    /** Province names this zone covers (matched case-insensitively by the service). */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "shipping_zone_provinces",
            joinColumns = @JoinColumn(name = "zone_id"))
    @Column(name = "province", length = 100)
    @Builder.Default
    private Set<String> provinces = new HashSet<>();

    /** Lower value = matched first when a province fits multiple zones. */
    @Column(nullable = false)
    @Builder.Default
    private int priority = 100;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
