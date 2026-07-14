package com.ecommerce.ecommercebackend.service;

import com.ecommerce.ecommercebackend.dto.response.CouponPreviewItemResponse;
import com.ecommerce.ecommercebackend.dto.response.CouponPreviewResponse;
import com.ecommerce.ecommercebackend.entity.Cart;
import com.ecommerce.ecommercebackend.entity.CartItem;
import com.ecommerce.ecommercebackend.entity.Product;
import com.ecommerce.ecommercebackend.entity.ProductVariant;
import com.ecommerce.ecommercebackend.entity.User;
import com.ecommerce.ecommercebackend.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * D05 — Lets a customer try a coupon code against their real cart.
 *
 * <p>Per the task's requirement, this never trusts the frontend for prices or
 * totals: it re-reads the caller's live cart via {@link CartService}, current
 * stock via {@link Product#getAvailableQuantity()}/{@link ProductVariant#getAvailableQuantity()},
 * and prices/coupon rules via {@link OrderPricingService}.</p>
 */
@Service
@RequiredArgsConstructor
public class CouponPreviewService {

    private final CartService cartService;
    private final OrderPricingService orderPricingService;

    @Transactional(readOnly = true)
    public CouponPreviewResponse preview(User user, String couponCode) {
        Cart cart = cartService.getOrCreateCart(user);
        if (cart.getItems().isEmpty()) {
            throw new BadRequestException("Giỏ hàng đang trống.");
        }
        assertAllItemsInStock(cart);

        OrderPricingResult result = orderPricingService.price(cart, couponCode, user);

        var items = result.getItems().stream()
                .map(this::toItemResponse)
                .toList();

        return CouponPreviewResponse.builder()
                .couponCode(couponCode == null ? null : couponCode.trim().toUpperCase(Locale.ROOT))
                .subtotalAmount(result.getSubtotalAmount())
                .discountAmount(result.getDiscountAmount())
                .totalBeforeShipping(result.getSubtotalAmount() - result.getDiscountAmount())
                .items(items)
                .build();
    }

    private void assertAllItemsInStock(Cart cart) {
        for (CartItem cartItem : cart.getItems()) {
            ProductVariant variant = cartItem.getVariant();
            int available = (variant != null)
                    ? variant.getAvailableQuantity()
                    : cartItem.getProduct().getAvailableQuantity();
            if (cartItem.getQuantity() > available) {
                throw new BadRequestException(
                        "Sản phẩm '" + cartItem.getProduct().getName()
                                + "' chỉ còn " + available + " trong kho.");
            }
        }
    }

    private CouponPreviewItemResponse toItemResponse(OrderPricingResult.Item item) {
        Product product = item.getCartItem().getProduct();
        return CouponPreviewItemResponse.builder()
                .productId(product.getId())
                .productName(product.getName())
                .quantity(item.getCartItem().getQuantity())
                .unitPrice(item.getUnitPrice())
                .lineTotal(item.getLineTotal())
                .discountAmount(item.getDiscountAmount())
                .build();
    }
}
