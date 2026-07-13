package com.ecommerce.ecommercebackend.service;

import com.ecommerce.ecommercebackend.dto.request.CouponRequest;
import com.ecommerce.ecommercebackend.dto.response.CouponResponse;
import com.ecommerce.ecommercebackend.entity.Coupon;
import com.ecommerce.ecommercebackend.entity.CouponScope;
import com.ecommerce.ecommercebackend.entity.DiscountType;
import com.ecommerce.ecommercebackend.exception.BadRequestException;
import com.ecommerce.ecommercebackend.exception.ResourceNotFoundException;
import com.ecommerce.ecommercebackend.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

/**
 * Admin business logic for managing coupons (task D03): create/update/toggle,
 * enforcing the D01 rules (percentage range, scope-specific targets, valid
 * date range, uniqueness) up front so bad data never reaches the {@code coupons}
 * table that D04/D05 will read from.
 */
@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;

    // ── Read ────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<CouponResponse> getAll(Boolean active) {
        return couponRepository.findAll().stream()
                .filter(c -> active == null || c.isActive() == active)
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CouponResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    // ── Create ──────────────────────────────────────────────────────────────────

    @Transactional
    public CouponResponse create(CouponRequest request) {
        String code = normalizeCode(request.getCode());
        if (couponRepository.existsByCode(code)) {
            throw new BadRequestException("Mã giảm giá '" + code + "' đã tồn tại.");
        }
        validateRules(request);

        Coupon coupon = Coupon.builder()
                .code(code)
                .build();
        applyRequest(coupon, request);

        return toResponse(couponRepository.save(coupon));
    }

    // ── Update ──────────────────────────────────────────────────────────────────

    @Transactional
    public CouponResponse update(Long id, CouponRequest request) {
        Coupon coupon = findOrThrow(id);

        String code = normalizeCode(request.getCode());
        if (!code.equals(coupon.getCode()) && couponRepository.existsByCode(code)) {
            throw new BadRequestException("Mã giảm giá '" + code + "' đã tồn tại.");
        }
        validateRules(request);

        coupon.setCode(code);
        applyRequest(coupon, request);

        return toResponse(couponRepository.save(coupon));
    }

    // ── Activate / deactivate ────────────────────────────────────────────────────

    @Transactional
    public CouponResponse setActive(Long id, boolean active) {
        Coupon coupon = findOrThrow(id);
        coupon.setActive(active);
        return toResponse(couponRepository.save(coupon));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    private Coupon findOrThrow(Long id) {
        return couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found with id: " + id));
    }

    private String normalizeCode(String rawCode) {
        return rawCode.trim().toUpperCase(Locale.ROOT);
    }

    /** Enforces the D01 coupon rules that span multiple fields. */
    private void validateRules(CouponRequest request) {
        if (request.getDiscountType() == DiscountType.PERCENTAGE
                && request.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BadRequestException("Giảm giá theo % không được vượt quá 100.");
        }

        if (request.getScope() == CouponScope.SPECIFIC_PRODUCTS
                && (request.getApplicableProductIds() == null || request.getApplicableProductIds().isEmpty())) {
            throw new BadRequestException(
                    "Phải chỉ định ít nhất 1 sản phẩm khi scope là SPECIFIC_PRODUCTS.");
        }

        if (request.getScope() == CouponScope.SPECIFIC_CATEGORIES
                && (request.getApplicableCategories() == null || request.getApplicableCategories().isEmpty())) {
            throw new BadRequestException(
                    "Phải chỉ định ít nhất 1 danh mục khi scope là SPECIFIC_CATEGORIES.");
        }

        if (request.getStartAt() != null && request.getEndAt() != null
                && !request.getStartAt().isBefore(request.getEndAt())) {
            throw new BadRequestException("startAt phải trước endAt.");
        }
    }

    private void applyRequest(Coupon coupon, CouponRequest request) {
        coupon.setDescription(request.getDescription());
        coupon.setDiscountType(request.getDiscountType());
        coupon.setDiscountValue(request.getDiscountValue());
        coupon.setScope(request.getScope());
        coupon.setApplicableProductIds(request.getApplicableProductIds() == null
                ? new HashSet<>() : new HashSet<>(request.getApplicableProductIds()));
        coupon.setApplicableCategories(request.getApplicableCategories() == null
                ? new HashSet<>() : new HashSet<>(request.getApplicableCategories()));
        coupon.setMinOrderAmount(request.getMinOrderAmount());
        coupon.setMaxDiscountAmount(request.getMaxDiscountAmount());
        coupon.setUsageLimit(request.getUsageLimit());
        coupon.setUsageLimitPerUser(request.getUsageLimitPerUser());
        coupon.setStartAt(request.getStartAt());
        coupon.setEndAt(request.getEndAt());
    }

    private CouponResponse toResponse(Coupon coupon) {
        return CouponResponse.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .description(coupon.getDescription())
                .discountType(coupon.getDiscountType())
                .discountValue(coupon.getDiscountValue())
                .scope(coupon.getScope())
                .applicableProductIds(coupon.getApplicableProductIds())
                .applicableCategories(coupon.getApplicableCategories())
                .minOrderAmount(coupon.getMinOrderAmount())
                .maxDiscountAmount(coupon.getMaxDiscountAmount())
                .usageLimit(coupon.getUsageLimit())
                .usageLimitPerUser(coupon.getUsageLimitPerUser())
                .usedCount(coupon.getUsedCount())
                .startAt(coupon.getStartAt())
                .endAt(coupon.getEndAt())
                .active(coupon.isActive())
                .createdAt(coupon.getCreatedAt())
                .updatedAt(coupon.getUpdatedAt())
                .build();
    }
}
