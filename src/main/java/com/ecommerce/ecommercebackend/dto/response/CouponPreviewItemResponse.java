package com.ecommerce.ecommercebackend.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * One cart line's pricing breakdown in a coupon preview (task D05).
 */
@Data
@Builder
public class CouponPreviewItemResponse {

    private Long productId;
    private String productName;
    private int quantity;
    private Long unitPrice;
    private Long lineTotal;
    private Long discountAmount;
}
