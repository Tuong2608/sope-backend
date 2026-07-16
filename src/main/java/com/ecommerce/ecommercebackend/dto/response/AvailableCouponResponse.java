package com.ecommerce.ecommercebackend.dto.response;

import com.ecommerce.ecommercebackend.entity.DiscountType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Public coupon summary shown on a product detail page.
 * Sensitive/admin-only fields and full applicability lists are intentionally omitted.
 */
@Data
@Builder
public class AvailableCouponResponse {

    private String code;
    private String description;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private Long minOrderAmount;
    private Long maxDiscountAmount;
    private LocalDateTime endAt;
}
