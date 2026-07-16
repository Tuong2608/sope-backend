package com.ecommerce.ecommercebackend.service;

import com.ecommerce.ecommercebackend.dto.request.CouponRequest;
import com.ecommerce.ecommercebackend.dto.response.AvailableCouponResponse;
import com.ecommerce.ecommercebackend.dto.response.CouponResponse;
import com.ecommerce.ecommercebackend.entity.Coupon;
import com.ecommerce.ecommercebackend.entity.CouponScope;
import com.ecommerce.ecommercebackend.entity.DiscountType;
import com.ecommerce.ecommercebackend.entity.Product;
import com.ecommerce.ecommercebackend.exception.BadRequestException;
import com.ecommerce.ecommercebackend.exception.ResourceNotFoundException;
import com.ecommerce.ecommercebackend.repository.CouponRepository;
import com.ecommerce.ecommercebackend.repository.CouponUsageRepository;
import com.ecommerce.ecommercebackend.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final CouponUsageRepository couponUsageRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<CouponResponse> getAll(Boolean active) {
        return couponRepository.findAll().stream()
                .filter(coupon -> active == null || coupon.isActive() == active)
                .sorted(Comparator.comparing(
                        Coupon::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CouponResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    /**
     * Returns globally usable coupons whose scope matches the requested product.
     * Per-user usage limits and cart subtotal requirements are still revalidated by
     * CouponPreviewService during cart/checkout.
     */
    @Transactional(readOnly = true)
    public List<AvailableCouponResponse> getAvailableForProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with id: " + productId));
        LocalDateTime now = LocalDateTime.now();

        return couponRepository.findAll().stream()
                .filter(Coupon::isActive)
                .filter(coupon -> coupon.isWithinValidPeriod(now))
                .filter(coupon -> !coupon.hasReachedUsageLimit())
                .filter(coupon -> appliesToProduct(coupon, product))
                .sorted(Comparator
                        .comparing(Coupon::getEndAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Coupon::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5)
                .map(this::toAvailableResponse)
                .toList();
    }

    @Transactional
    public CouponResponse create(CouponRequest request) {
        String code = normalizeCode(request.getCode());
        if (couponRepository.existsByCode(code)) {
            throw new BadRequestException("Mã giảm giá '" + code + "' đã tồn tại.");
        }
        validateRules(request);

        Coupon coupon = Coupon.builder()
                .code(code)
                .active(true)
                .build();
        applyRequest(coupon, request);
        return toResponse(couponRepository.save(coupon));
    }

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

    @Transactional
    public CouponResponse setActive(Long id, boolean active) {
        Coupon coupon = findOrThrow(id);
        coupon.setActive(active);
        return toResponse(couponRepository.save(coupon));
    }

    @Transactional
    public void delete(Long id) {
        Coupon coupon = findOrThrow(id);
        if (couponUsageRepository.existsByCouponId(id)) {
            throw new BadRequestException(
                    "Mã giảm giá đã phát sinh lịch sử sử dụng nên không thể xóa. Hãy chuyển sang trạng thái tạm ngưng.");
        }
        couponRepository.delete(coupon);
    }

    private Coupon findOrThrow(Long id) {
        return couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found with id: " + id));
    }

    private String normalizeCode(String rawCode) {
        String normalized = rawCode == null ? "" : rawCode.trim().toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z0-9_-]{3,50}")) {
            throw new BadRequestException(
                    "Mã giảm giá phải dài 3-50 ký tự và chỉ gồm chữ in hoa, số, dấu gạch ngang hoặc gạch dưới.");
        }
        return normalized;
    }

    private void validateRules(CouponRequest request) {
        if (request.getDiscountType() == DiscountType.PERCENTAGE
                && request.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BadRequestException("Giảm giá theo % không được vượt quá 100.");
        }

        if (request.getScope() == CouponScope.SPECIFIC_PRODUCTS
                && (request.getApplicableProductIds() == null || request.getApplicableProductIds().isEmpty())) {
            throw new BadRequestException(
                    "Phải chỉ định ít nhất 1 sản phẩm khi phạm vi là SPECIFIC_PRODUCTS.");
        }

        if (request.getScope() == CouponScope.SPECIFIC_CATEGORIES
                && (request.getApplicableCategories() == null || request.getApplicableCategories().isEmpty())) {
            throw new BadRequestException(
                    "Phải chỉ định ít nhất 1 danh mục khi phạm vi là SPECIFIC_CATEGORIES.");
        }

        if (request.getStartAt() != null && request.getEndAt() != null
                && !request.getStartAt().isBefore(request.getEndAt())) {
            throw new BadRequestException("Thời gian bắt đầu phải trước thời gian kết thúc.");
        }

        if (request.getUsageLimit() != null && request.getUsageLimitPerUser() != null
                && request.getUsageLimitPerUser() > request.getUsageLimit()) {
            throw new BadRequestException(
                    "Giới hạn mỗi người không được lớn hơn tổng giới hạn sử dụng.");
        }
    }

    private void applyRequest(Coupon coupon, CouponRequest request) {
        coupon.setDescription(normalizeOptionalText(request.getDescription()));
        coupon.setDiscountType(request.getDiscountType());
        coupon.setDiscountValue(request.getDiscountValue());
        coupon.setScope(request.getScope());

        Set<Long> productIds = request.getApplicableProductIds() == null
                ? new HashSet<>()
                : request.getApplicableProductIds().stream()
                        .filter(id -> id != null && id > 0)
                        .collect(Collectors.toSet());
        Set<String> categories = request.getApplicableCategories() == null
                ? new HashSet<>()
                : request.getApplicableCategories().stream()
                        .filter(value -> value != null && !value.isBlank())
                        .map(String::trim)
                        .collect(Collectors.toSet());

        coupon.setApplicableProductIds(
                request.getScope() == CouponScope.SPECIFIC_PRODUCTS ? productIds : new HashSet<>());
        coupon.setApplicableCategories(
                request.getScope() == CouponScope.SPECIFIC_CATEGORIES ? categories : new HashSet<>());
        coupon.setMinOrderAmount(request.getMinOrderAmount());
        coupon.setMaxDiscountAmount(
                request.getDiscountType() == DiscountType.PERCENTAGE
                        ? request.getMaxDiscountAmount()
                        : null);
        coupon.setUsageLimit(request.getUsageLimit());
        coupon.setUsageLimitPerUser(request.getUsageLimitPerUser());
        coupon.setStartAt(request.getStartAt());
        coupon.setEndAt(request.getEndAt());
    }

    private boolean appliesToProduct(Coupon coupon, Product product) {
        if (coupon.getScope() == CouponScope.ALL_ORDER) {
            return true;
        }
        if (coupon.getScope() == CouponScope.SPECIFIC_PRODUCTS) {
            return coupon.getApplicableProductIds() != null
                    && coupon.getApplicableProductIds().contains(product.getId());
        }
        if (coupon.getScope() == CouponScope.SPECIFIC_CATEGORIES) {
            if (product.getCategory() == null || coupon.getApplicableCategories() == null) {
                return false;
            }
            String productCategory = product.getCategory().trim();
            return coupon.getApplicableCategories().stream()
                    .filter(value -> value != null && !value.isBlank())
                    .anyMatch(value -> value.trim().equalsIgnoreCase(productCategory));
        }
        return false;
    }

    private AvailableCouponResponse toAvailableResponse(Coupon coupon) {
        return AvailableCouponResponse.builder()
                .code(coupon.getCode())
                .description(coupon.getDescription())
                .discountType(coupon.getDiscountType())
                .discountValue(coupon.getDiscountValue())
                .minOrderAmount(coupon.getMinOrderAmount())
                .maxDiscountAmount(coupon.getMaxDiscountAmount())
                .endAt(coupon.getEndAt())
                .build();
    }

    private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    private CouponResponse toResponse(Coupon coupon) {
        return CouponResponse.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .description(coupon.getDescription())
                .discountType(coupon.getDiscountType())
                .discountValue(coupon.getDiscountValue())
                .scope(coupon.getScope())
                .applicableProductIds(Set.copyOf(coupon.getApplicableProductIds()))
                .applicableCategories(Set.copyOf(coupon.getApplicableCategories()))
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
