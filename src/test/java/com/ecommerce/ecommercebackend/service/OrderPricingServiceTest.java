package com.ecommerce.ecommercebackend.service;

import com.ecommerce.ecommercebackend.entity.*;
import com.ecommerce.ecommercebackend.exception.BadRequestException;
import com.ecommerce.ecommercebackend.repository.CouponRepository;
import com.ecommerce.ecommercebackend.repository.CouponUsageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * D04 — Coverage for order pricing: discount allocation across items by scope,
 * the max-discount cap, and coupon validity rules (min order amount, usage
 * limits, expiry) that D05's preview endpoint also relies on.
 */
@ExtendWith(MockitoExtension.class)
class OrderPricingServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private CouponUsageRepository couponUsageRepository;

    private OrderPricingService orderPricingService;
    private User user;

    @BeforeEach
    void setUp() {
        orderPricingService = new OrderPricingService(couponRepository, couponUsageRepository);
        user = User.builder().id(1L).username("nam").email("nam@example.com")
                .password("secret").role(Role.ROLE_USER).build();
        lenient().when(couponUsageRepository.countByCouponIdAndUserIdAndStatus(
                anyLong(), anyLong(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(0L);
    }

    @Test
    void noCouponReturnsSubtotalWithoutDiscount() {
        Cart cart = cartWith(item(product(1L, "Laptop", "laptop", 10_000_000L), 2));

        OrderPricingResult result = orderPricingService.price(cart, null, user);

        assertThat(result.getSubtotalAmount()).isEqualTo(20_000_000L);
        assertThat(result.getDiscountAmount()).isEqualTo(0L);
        assertThat(result.getAppliedCoupon()).isNull();
        assertThat(result.getItems().get(0).getDiscountAmount()).isEqualTo(0L);
    }

    @Test
    void percentageAllOrderCouponAllocatesAcrossAllItems() {
        Cart cart = cartWith(
                item(product(1L, "Laptop", "laptop", 10_000_000L), 1),
                item(product(2L, "Chuot", "accessory", 500_000L), 2));
        Coupon coupon = coupon("SALE10", DiscountType.PERCENTAGE, "10", CouponScope.ALL_ORDER, null, null);
        when(couponRepository.findByCode("SALE10")).thenReturn(Optional.of(coupon));

        OrderPricingResult result = orderPricingService.price(cart, "sale10", user);

        // subtotal = 10.000.000 + 1.000.000 = 11.000.000 -> 10% = 1.100.000
        assertThat(result.getSubtotalAmount()).isEqualTo(11_000_000L);
        assertThat(result.getDiscountAmount()).isEqualTo(1_100_000L);
        long sumItemDiscount = result.getItems().stream()
                .mapToLong(OrderPricingResult.Item::getDiscountAmount).sum();
        assertThat(sumItemDiscount).isEqualTo(result.getDiscountAmount());
    }

    @Test
    void specificProductsCouponOnlyDiscountsEligibleItems() {
        Product eligible = product(1L, "Laptop", "laptop", 10_000_000L);
        Product other = product(2L, "Chuot", "accessory", 500_000L);
        Cart cart = cartWith(item(eligible, 1), item(other, 1));

        Coupon coupon = coupon("LAPTOP20", DiscountType.PERCENTAGE, "20",
                CouponScope.SPECIFIC_PRODUCTS, Set.of(1L), null);
        when(couponRepository.findByCode("LAPTOP20")).thenReturn(Optional.of(coupon));

        OrderPricingResult result = orderPricingService.price(cart, "LAPTOP20", user);

        // Chỉ laptop (10.000.000) được giảm 20% = 2.000.000; chuột không đổi.
        assertThat(result.getDiscountAmount()).isEqualTo(2_000_000L);
        OrderPricingResult.Item laptopItem = result.getItems().stream()
                .filter(i -> i.getCartItem().getProduct().getId().equals(1L)).findFirst().orElseThrow();
        OrderPricingResult.Item mouseItem = result.getItems().stream()
                .filter(i -> i.getCartItem().getProduct().getId().equals(2L)).findFirst().orElseThrow();
        assertThat(laptopItem.getDiscountAmount()).isEqualTo(2_000_000L);
        assertThat(mouseItem.getDiscountAmount()).isEqualTo(0L);
    }

    @Test
    void discountIsCappedByMaxDiscountAmount() {
        Cart cart = cartWith(item(product(1L, "Laptop", "laptop", 10_000_000L), 1));
        Coupon coupon = coupon("SALE50", DiscountType.PERCENTAGE, "50",
                CouponScope.ALL_ORDER, null, 1_000_000L);
        when(couponRepository.findByCode("SALE50")).thenReturn(Optional.of(coupon));

        OrderPricingResult result = orderPricingService.price(cart, "SALE50", user);

        // 50% cua 10.000.000 = 5.000.000 nhung bi cap o 1.000.000
        assertThat(result.getDiscountAmount()).isEqualTo(1_000_000L);
    }

    @Test
    void belowMinOrderAmountThrowsBadRequest() {
        Cart cart = cartWith(item(product(1L, "Chuot", "accessory", 100_000L), 1));
        Coupon coupon = coupon("SALE10", DiscountType.PERCENTAGE, "10", CouponScope.ALL_ORDER, null, null);
        coupon.setMinOrderAmount(1_000_000L);
        when(couponRepository.findByCode("SALE10")).thenReturn(Optional.of(coupon));

        assertThatThrownBy(() -> orderPricingService.price(cart, "SALE10", user))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void expiredCouponThrowsBadRequest() {
        Cart cart = cartWith(item(product(1L, "Laptop", "laptop", 10_000_000L), 1));
        Coupon coupon = coupon("OLD10", DiscountType.PERCENTAGE, "10", CouponScope.ALL_ORDER, null, null);
        coupon.setEndAt(LocalDateTime.now().minusDays(1));
        when(couponRepository.findByCode("OLD10")).thenReturn(Optional.of(coupon));

        assertThatThrownBy(() -> orderPricingService.price(cart, "OLD10", user))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void usageLimitReachedThrowsBadRequest() {
        Cart cart = cartWith(item(product(1L, "Laptop", "laptop", 10_000_000L), 1));
        Coupon coupon = coupon("LIMIT1", DiscountType.PERCENTAGE, "10", CouponScope.ALL_ORDER, null, null);
        coupon.setUsageLimit(5);
        coupon.setUsedCount(5);
        when(couponRepository.findByCode("LIMIT1")).thenReturn(Optional.of(coupon));

        assertThatThrownBy(() -> orderPricingService.price(cart, "LIMIT1", user))
                .isInstanceOf(BadRequestException.class);
    }

    // ── D06: concurrency — held slots count against the usage limit at checkout ──

    @Test
    void holdForCheckoutCountsHeldSlotsFromOtherUsersAgainstUsageLimit() {
        Cart cart = cartWith(item(product(1L, "Laptop", "laptop", 10_000_000L), 1));
        Coupon coupon = coupon("ONEUSE", DiscountType.PERCENTAGE, "10", CouponScope.ALL_ORDER, null, null);
        coupon.setUsageLimit(1);
        coupon.setUsedCount(0); // no one has finished checkout yet...
        when(couponRepository.findByCodeForUpdate("ONEUSE")).thenReturn(Optional.of(coupon));
        // ...but someone else is mid-checkout, holding the only slot.
        when(couponUsageRepository.countByCouponIdAndStatus(1L, CouponUsageStatus.HELD)).thenReturn(1L);

        assertThatThrownBy(() -> orderPricingService.price(cart, "ONEUSE", user, true))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void previewDoesNotCountHeldSlotsFromOtherCheckouts() {
        Cart cart = cartWith(item(product(1L, "Laptop", "laptop", 10_000_000L), 1));
        Coupon coupon = coupon("ONEUSE", DiscountType.PERCENTAGE, "10", CouponScope.ALL_ORDER, null, null);
        coupon.setUsageLimit(1);
        coupon.setUsedCount(0);
        when(couponRepository.findByCode("ONEUSE")).thenReturn(Optional.of(coupon));

        OrderPricingResult result = orderPricingService.price(cart, "ONEUSE", user, false);

        assertThat(result.getDiscountAmount()).isGreaterThan(0L);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    private Cart cartWith(CartItem... items) {
        Cart cart = Cart.builder().id(100L).user(user).build();
        for (CartItem item : items) {
            cart.addItem(item);
        }
        return cart;
    }

    private CartItem item(Product product, int quantity) {
        return CartItem.builder().id(product.getId()).product(product).quantity(quantity).build();
    }

    private Product product(Long id, String name, String category, Long price) {
        return Product.builder().id(id).name(name).category(category).price(price).build();
    }

    private Coupon coupon(String code, DiscountType type, String value, CouponScope scope,
                           Set<Long> productIds, Long maxDiscountAmount) {
        return Coupon.builder()
                .id(1L)
                .code(code)
                .discountType(type)
                .discountValue(new BigDecimal(value))
                .scope(scope)
                .applicableProductIds(productIds == null ? new HashSet<>() : new HashSet<>(productIds))
                .applicableCategories(new HashSet<>())
                .maxDiscountAmount(maxDiscountAmount)
                .active(true)
                .usedCount(0)
                .build();
    }
}
