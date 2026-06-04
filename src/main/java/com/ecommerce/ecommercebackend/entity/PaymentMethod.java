package com.ecommerce.ecommercebackend.entity;

/**
 * Phương thức thanh toán cho một đơn hàng.
 *
 * <p>{@code VNPAY}/{@code MOMO} khớp với {@link PaymentProvider} của module
 * thanh toán (Tưởng) để frontend gọi tiếp {@code POST /api/payment/create};
 * {@code COD} là thanh toán khi nhận hàng, không qua cổng.</p>
 */
public enum PaymentMethod {
    COD,
    VNPAY,
    MOMO
}
