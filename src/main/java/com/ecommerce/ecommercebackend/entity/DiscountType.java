package com.ecommerce.ecommercebackend.entity;

/**
 * How a {@link Coupon}'s {@code discountValue} is interpreted (task D01).
 */
public enum DiscountType {
    /** {@code discountValue} is a percentage (0–100) off the applicable amount. */
    PERCENTAGE,
    /** {@code discountValue} is a fixed VND amount off the applicable amount. */
    FIXED_AMOUNT
}
