package com.ecommerce.ecommercebackend.entity;

/**
 * What portion of an order a {@link Coupon} discounts (task D01).
 */
public enum CouponScope {
    /** Discounts the whole order total. */
    ALL_ORDER,
    /** Discounts only line items whose product id is in {@code applicableProductIds}. */
    SPECIFIC_PRODUCTS,
    /** Discounts only line items whose product category is in {@code applicableCategories}. */
    SPECIFIC_CATEGORIES
}
