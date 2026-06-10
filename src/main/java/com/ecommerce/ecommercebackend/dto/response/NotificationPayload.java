package com.ecommerce.ecommercebackend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Payload thông báo hệ thống gửi tới một user cụ thể.
 * Topic đích: {@code /topic/notification.{userId}}
 *
 * <p>Được trigger bởi {@link com.ecommerce.ecommercebackend.service.NotificationService}
 * khi có các sự kiện như: đặt hàng thành công, thanh toán hoàn tất,
 * có tin nhắn mới từ người bán/mua, v.v.</p>
 */
@Data
@Builder
public class NotificationPayload {

    /** Loại thông báo. */
    private NotificationType type;

    /** Tiêu đề ngắn gọn. */
    private String title;

    /** Nội dung chi tiết. */
    private String message;

    /** ID tài nguyên liên quan (orderId, paymentId, roomId, ...). */
    private String referenceId;

    private LocalDateTime timestamp;

    public enum NotificationType {
        /** Có tin nhắn mới từ buyer hoặc seller. */
        NEW_MESSAGE,
        /** Đơn hàng đã được đặt thành công. */
        ORDER_PLACED,
        /** Thanh toán hoàn tất. */
        PAYMENT_SUCCESS,
        /** Thanh toán thất bại. */
        PAYMENT_FAILED,
        /** Đơn hàng được cập nhật trạng thái. */
        ORDER_STATUS_UPDATED,
        /** Thông báo hệ thống chung. */
        SYSTEM
    }
}
