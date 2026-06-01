package com.ecommerce.ecommercebackend.entity;

/** Trạng thái của một giao dịch thanh toán. */
public enum PaymentStatus {
    /** Đã tạo link, chờ người dùng thanh toán. */
    PENDING,
    /** Thanh toán thành công (đã xác thực qua IPN). */
    SUCCESS,
    /** Thanh toán thất bại hoặc bị huỷ. */
    FAILED,
    /** Đã hoàn tiền. */
    REFUNDED
}
