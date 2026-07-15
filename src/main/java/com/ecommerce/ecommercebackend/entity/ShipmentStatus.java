package com.ecommerce.ecommercebackend.entity;

/**
 * Lifecycle status of a simulated shipment (task C10). Strictly linear —
 * {@link com.ecommerce.ecommercebackend.service.ShipmentTrackingService#advance}
 * always moves to the next value; {@code DELIVERED} is terminal.
 */
public enum ShipmentStatus {
    /** Tracking number generated, waiting for pickup. */
    CREATED,
    /** Carrier has picked up the package. */
    PICKED_UP,
    /** In transit between hubs. */
    IN_TRANSIT,
    /** Out with the courier for final delivery. */
    OUT_FOR_DELIVERY,
    /** Delivered to the recipient. */
    DELIVERED
}
