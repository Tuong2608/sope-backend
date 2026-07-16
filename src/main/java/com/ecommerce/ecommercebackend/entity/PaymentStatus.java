package com.ecommerce.ecommercebackend.entity;

/** Trạng thái của một giao dịch thanh toán. */
public enum PaymentStatus {
    /** Đã tạo link, chờ người dùng thanh toán. */
    PENDING,
    /** Người dùng đã quay lại nhưng backend vẫn đang chờ IPN đáng tin cậy. */
    PROCESSING,
    /** Thanh toán thành công (đã xác thực qua IPN). */
    SUCCESS,
    /** Thanh toán thất bại hoặc bị huỷ. */
    FAILED,
    /** Người dùng chủ động hủy giao dịch tại cổng thanh toán. */
    CANCELLED,
    /** Link thanh toán đã hết hạn. */
    EXPIRED,
    /** Đã hoàn tiền. */
    REFUNDED
}
