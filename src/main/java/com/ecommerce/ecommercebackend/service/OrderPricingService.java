package com.ecommerce.ecommercebackend.service;

import com.ecommerce.ecommercebackend.entity.*;
import com.ecommerce.ecommercebackend.exception.BadRequestException;
import com.ecommerce.ecommercebackend.repository.CouponRepository;
import com.ecommerce.ecommercebackend.repository.CouponUsageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Computes order totals from a cart, optionally applying a coupon (task D04).
 *
 * <p>Shared by the D05 preview endpoint ({@code CouponPreviewController}) and
 * C06 order creation ({@code OrderService.createOrder}) so the exact same rules
 * apply whether the customer is just checking a code or actually placing the
 * order — the backend always re-reads current cart/prices/stock and never
 * trusts a total sent from the frontend.</p>
 */
@Service
@RequiredArgsConstructor
public class OrderPricingService {

    private final CouponRepository couponRepository;
    private final CouponUsageRepository couponUsageRepository;

    /**
     * Prices every item in {@code cart} and, if {@code couponCode} is given,
     * validates and applies the coupon's discount.
     *
     * @throws BadRequestException if the cart has an item with no price, or the
     *                             coupon is invalid/expired/exhausted/not applicable
     */
    @Transactional(readOnly = true)
    public OrderPricingResult price(Cart cart, String couponCode, User user) {
        List<OrderPricingResult.Item> items = new ArrayList<>();
        long subtotal = 0L;

        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();
            ProductVariant variant = cartItem.getVariant();
            Long unitPrice = (variant != null)
                    ? variant.getEffectivePrice(product.getPrice())
                    : product.getPrice();
            if (unitPrice == null) {
                throw new BadRequestException(
                        "Sản phẩm '" + product.getName() + "' chưa có giá, không thể đặt hàng.");
            }
            long lineTotal = unitPrice * cartItem.getQuantity();
            subtotal += lineTotal;

            items.add(OrderPricingResult.Item.builder()
                    .cartItem(cartItem)
                    .unitPrice(unitPrice)
                    .lineTotal(lineTotal)
                    .discountAmount(0L)
                    .build());
        }

        Coupon coupon = null;
        long totalDiscount = 0L;

        if (StringUtils.hasText(couponCode)) {
            coupon = resolveAndValidateCoupon(couponCode, subtotal, user);
            totalDiscount = applyDiscount(coupon, items, subtotal);
        }

        return OrderPricingResult.builder()
                .subtotalAmount(subtotal)
                .discountAmount(totalDiscount)
                .appliedCoupon(coupon)
                .items(items)
                .build();
    }

    // ── Coupon validation ────────────────────────────────────────────────────────

    private Coupon resolveAndValidateCoupon(String rawCode, long subtotal, User user) {
        String code = rawCode.trim().toUpperCase(Locale.ROOT);
        Coupon coupon = couponRepository.findByCode(code)
                .orElseThrow(() -> new BadRequestException("Mã giảm giá không tồn tại: " + code));

        if (!coupon.isActive()) {
            throw new BadRequestException("Mã giảm giá đã bị tắt.");
        }
        if (!coupon.isWithinValidPeriod(LocalDateTime.now())) {
            throw new BadRequestException("Mã giảm giá đã hết hạn hoặc chưa bắt đầu.");
        }
        if (coupon.hasReachedUsageLimit()) {
            throw new BadRequestException("Mã giảm giá đã hết lượt sử dụng.");
        }
        if (coupon.getUsageLimitPerUser() != null) {
            long usedByUser = couponUsageRepository.countByCouponIdAndUserIdAndStatus(
                    coupon.getId(), user.getId(), CouponUsageStatus.USED);
            if (usedByUser >= coupon.getUsageLimitPerUser()) {
                throw new BadRequestException("Bạn đã sử dụng hết lượt cho mã giảm giá này.");
            }
        }
        if (coupon.getMinOrderAmount() != null && subtotal < coupon.getMinOrderAmount()) {
            throw new BadRequestException(
                    "Đơn hàng tối thiểu " + coupon.getMinOrderAmount() + "đ để dùng mã này.");
        }
        return coupon;
    }

    /** Computes and allocates the discount across items; returns the total discount. */
    private long applyDiscount(Coupon coupon, List<OrderPricingResult.Item> items, long subtotal) {
        long eligibleAmount = switch (coupon.getScope()) {
            case ALL_ORDER -> subtotal;
            case SPECIFIC_PRODUCTS -> items.stream()
                    .filter(item -> coupon.getApplicableProductIds()
                            .contains(item.getCartItem().getProduct().getId()))
                    .mapToLong(OrderPricingResult.Item::getLineTotal)
                    .sum();
            case SPECIFIC_CATEGORIES -> items.stream()
                    .filter(item -> coupon.getApplicableCategories()
                            .contains(item.getCartItem().getProduct().getCategory()))
                    .mapToLong(OrderPricingResult.Item::getLineTotal)
                    .sum();
        };

        if (eligibleAmount <= 0) {
            throw new BadRequestException("Mã giảm giá không áp dụng cho sản phẩm nào trong giỏ hàng.");
        }

        long discount = switch (coupon.getDiscountType()) {
            case PERCENTAGE -> BigDecimal.valueOf(eligibleAmount)
                    .multiply(coupon.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR)
                    .longValue();
            case FIXED_AMOUNT -> Math.min(coupon.getDiscountValue().longValue(), eligibleAmount);
        };
        if (coupon.getMaxDiscountAmount() != null) {
            discount = Math.min(discount, coupon.getMaxDiscountAmount());
        }

        allocateDiscount(coupon, items, eligibleAmount, discount);
        return discount;
    }

    /** Splits {@code totalDiscount} across eligible items proportionally to their line total. */
    private void allocateDiscount(
            Coupon coupon, List<OrderPricingResult.Item> items, long eligibleAmount, long totalDiscount) {
        List<OrderPricingResult.Item> eligibleItems = items.stream()
                .filter(item -> isEligible(coupon, item))
                .toList();

        long allocated = 0L;
        for (int i = 0; i < eligibleItems.size(); i++) {
            OrderPricingResult.Item item = eligibleItems.get(i);
            long itemDiscount;
            if (i == eligibleItems.size() - 1) {
                // Last item absorbs the rounding remainder so the sum matches exactly.
                itemDiscount = totalDiscount - allocated;
            } else {
                itemDiscount = BigDecimal.valueOf(totalDiscount)
                        .multiply(BigDecimal.valueOf(item.getLineTotal()))
                        .divide(BigDecimal.valueOf(eligibleAmount), 0, RoundingMode.FLOOR)
                        .longValue();
                allocated += itemDiscount;
            }
            item.setDiscountAmount(itemDiscount);
        }
    }

    private boolean isEligible(Coupon coupon, OrderPricingResult.Item item) {
        return switch (coupon.getScope()) {
            case ALL_ORDER -> true;
            case SPECIFIC_PRODUCTS -> coupon.getApplicableProductIds()
                    .contains(item.getCartItem().getProduct().getId());
            case SPECIFIC_CATEGORIES -> coupon.getApplicableCategories()
                    .contains(item.getCartItem().getProduct().getCategory());
        };
    }
}
