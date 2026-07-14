package com.ecommerce.ecommercebackend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A delivery method, e.g. standard or express (task C02).
 * Actual fee/ETA per zone lives in {@link ShippingRate}.
 */
@Entity
@Table(
        name = "shipping_methods",
        uniqueConstraints = @UniqueConstraint(name = "uk_shipping_methods_code", columnNames = "code")
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Stable identifier used by clients, e.g. "STANDARD", "EXPRESS". */
    @Column(nullable = false, length = 30)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
