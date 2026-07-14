package com.ecommerce.ecommercebackend.dto.response;

import com.ecommerce.ecommercebackend.entity.CouponScope;
import com.ecommerce.ecommercebackend.entity.DiscountType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * A coupon returned to the admin management UI (task D03).
 */
@Data
@Builder
public class CouponResponse {

    private Long id;
    private String code;
    private String description;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private CouponScope scope;
    private Set<Long> applicableProductIds;
    private Set<String> applicableCategories;
    private Long minOrderAmount;
    private Long maxDiscountAmount;
    private Integer usageLimit;
    private Integer usageLimitPerUser;
    private int usedCount;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
