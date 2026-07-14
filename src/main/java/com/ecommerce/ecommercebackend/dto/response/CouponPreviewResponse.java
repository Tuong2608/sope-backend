package com.ecommerce.ecommercebackend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Result of previewing a coupon against the caller's current cart (task D05).
 * Does not include shipping fee — that is only known once a delivery address
 * is provided at checkout (see {@code POST /api/delivery/estimate}).
 */
@Data
@Builder
public class CouponPreviewResponse {

    private String couponCode;
    private long subtotalAmount;
    private long discountAmount;
    /** {@code subtotalAmount - discountAmount}; shipping fee is added at checkout. */
    private long totalBeforeShipping;
    private List<CouponPreviewItemResponse> items;
}
