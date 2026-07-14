package com.ecommerce.ecommercebackend.dto.request;

import com.ecommerce.ecommercebackend.entity.CouponScope;
import com.ecommerce.ecommercebackend.entity.DiscountType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Request payload for creating/updating a coupon (task D03).
 * Cross-field rules (percentage range, scope-specific ids/categories, date
 * ordering) are enforced in {@code CouponService}, not here.
 */
@Data
public class CouponRequest {

    @NotBlank(message = "code is required")
    private String code;

    private String description;

    @NotNull(message = "discountType is required")
    private DiscountType discountType;

    @NotNull(message = "discountValue is required")
    @DecimalMin(value = "0", inclusive = false, message = "discountValue must be positive")
    private BigDecimal discountValue;

    @NotNull(message = "scope is required")
    private CouponScope scope;

    private Set<Long> applicableProductIds = new HashSet<>();

    private Set<String> applicableCategories = new HashSet<>();

    @Min(value = 0, message = "minOrderAmount cannot be negative")
    private Long minOrderAmount;

    @Min(value = 0, message = "maxDiscountAmount cannot be negative")
    private Long maxDiscountAmount;

    @Min(value = 1, message = "usageLimit must be at least 1")
    private Integer usageLimit;

    @Min(value = 1, message = "usageLimitPerUser must be at least 1")
    private Integer usageLimitPerUser;

    private LocalDateTime startAt;

    private LocalDateTime endAt;
}
