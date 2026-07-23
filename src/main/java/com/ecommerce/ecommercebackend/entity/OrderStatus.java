package com.ecommerce.ecommercebackend.entity;

/**
 * Lifecycle status of a customer order (task C07).
 *
 * <p>Online orders use {@code PENDING → PAID → PROCESSING → SHIPPING → COMPLETED}.
 * COD orders are approved with {@code PENDING → PROCESSING → SHIPPING → COMPLETED}.
 * {@code CANCELLED} is reachable from {@code PENDING}/{@code PAID}/{@code PROCESSING}.
 * {@code COMPLETED} and {@code CANCELLED} are terminal. Enforced in
 * {@code OrderService.assertValidTransition}.</p>
 */
public enum OrderStatus {
    /** Đơn vừa được tạo, chờ xác nhận / thanh toán. */
    PENDING,
    /** Đã thanh toán thành công (đồng bộ từ IPN của cổng thanh toán hoặc admin xác nhận). */
    PAID,
    /** Đã được admin duyệt và đang được xử lý/đóng gói. */
    PROCESSING,
    /** Đang giao hàng. */
    SHIPPING,
    /** Đã hoàn tất. */
    COMPLETED,
    /** Đã huỷ / hoàn tiền. */
    CANCELLED
}
