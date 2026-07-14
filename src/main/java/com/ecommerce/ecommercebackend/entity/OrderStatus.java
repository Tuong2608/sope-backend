package com.ecommerce.ecommercebackend.entity;

/**
 * Lifecycle status of a customer order (task C07).
 *
 * <p>Valid transitions: {@code PENDING → PAID → PROCESSING → SHIPPING → COMPLETED},
 * with {@code CANCELLED} reachable from {@code PENDING}/{@code PAID}/{@code PROCESSING}.
 * {@code COMPLETED} and {@code CANCELLED} are terminal. Enforced in
 * {@code OrderService.assertValidTransition}.</p>
 */
public enum OrderStatus {
    /** Đơn vừa được tạo, chờ xác nhận / thanh toán. */
    PENDING,
    /** Đã thanh toán thành công (đồng bộ từ IPN của cổng thanh toán hoặc admin xác nhận). */
    PAID,
    /** Đang được xử lý/đóng gói sau khi thanh toán. */
    PROCESSING,
    /** Đang giao hàng. */
    SHIPPING,
    /** Đã hoàn tất. */
    COMPLETED,
    /** Đã huỷ / hoàn tiền. */
    CANCELLED
}
