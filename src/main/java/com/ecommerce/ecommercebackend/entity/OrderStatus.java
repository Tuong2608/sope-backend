package com.ecommerce.ecommercebackend.entity;

/** Lifecycle status of a customer order. */
public enum OrderStatus {
    /** Đơn vừa được tạo, chờ thanh toán / xử lý. */
    PENDING,
    /** Đã thanh toán thành công (đồng bộ từ IPN của cổng thanh toán). */
    PAID,
    /** Đang giao hàng. */
    SHIPPING,
    /** Đã hoàn tất. */
    COMPLETED,
    /** Đã huỷ. */
    CANCELLED
}
