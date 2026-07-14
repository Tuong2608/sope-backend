package com.ecommerce.ecommercebackend.service;

import com.ecommerce.ecommercebackend.entity.CartItem;
import com.ecommerce.ecommercebackend.entity.Coupon;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Internal pricing breakdown produced by {@link OrderPricingService} (task D04).
 * Used by both the D05 preview endpoint and C06 order creation — kept out of the
 * HTTP DTO packages since it carries entity references for internal reuse.
 */
@Data
@Builder
public class OrderPricingResult {

    /** Sum of every line total before discount. */
    private long subtotalAmount;

    /** Total discount granted by the coupon (0 if none applied). */
    private long discountAmount;

    /** The resolved, validated coupon — {@code null} if none was requested. */
    private Coupon appliedCoupon;

    private List<Item> items;

    @Data
    @Builder
    public static class Item {
        private CartItem cartItem;
        private Long unitPrice;
        private Long lineTotal;
        /** This line's share of {@link OrderPricingResult#discountAmount}. */
        private long discountAmount;
    }
}
