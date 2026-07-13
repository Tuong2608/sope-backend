package com.ecommerce.ecommercebackend.entity;

/**
 * Lifecycle of one {@link CouponUsage} record (task D02, enforced later in D06).
 */
public enum CouponUsageStatus {
    /** A slot is reserved for this order while checkout/payment is in progress. */
    HELD,
    /** Payment succeeded; the usage is final and counts against the coupon's limits. */
    USED,
    /** The order was cancelled/failed; the held slot was returned to the pool. */
    RELEASED
}
