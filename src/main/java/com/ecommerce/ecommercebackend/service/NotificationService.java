package com.ecommerce.ecommercebackend.service;

import com.ecommerce.ecommercebackend.dto.response.NotificationPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Service trigger thông báo hệ thống qua WebSocket STOMP.
 *
 * <h2>Task 5 — Hàm trigger thông báo hệ thống</h2>
 * <p>Các service khác (PaymentService, OrderService, ...) gọi các method
 * ở đây để push notification real-time tới client đang kết nối WebSocket.</p>
 *
 * <p>Topic đích: {@code /topic/notification.{userId}}</p>
 *
 * <pre>
 * Ví dụ: khi thanh toán thành công
 *   PaymentService.handleVnpayIpn(...)
 *     → notificationService.notifyPaymentSuccess(userId, paymentId)
 *       → SimpMessagingTemplate → /topic/notification.42
 *         → Frontend nhận, hiển thị Toast "Thanh toán thành công"
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    public static final String ADMIN_ORDER_TOPIC = "/topic/admin.orders";

    private final SimpMessagingTemplate messagingTemplate;

    // ── Destination builder ───────────────────────────────────────────────────

    /** Topic nhận thông báo của một user cụ thể. */
    private String notificationTopic(Long userId) {
        return "/topic/notification." + userId;
    }

    // ── Generic trigger ───────────────────────────────────────────────────────

    /**
     * Gửi một thông báo bất kỳ tới user.
     *
     * @param userId  ID người nhận thông báo
     * @param payload Nội dung thông báo
     */
    public void send(Long userId, NotificationPayload payload) {
        String destination = notificationTopic(userId);
        messagingTemplate.convertAndSend(destination, payload);
        log.info("[NOTIFY] → {} | type={} | title={}", destination, payload.getType(), payload.getTitle());
    }

    // ── Trigger: Tin nhắn mới ─────────────────────────────────────────────────

    /**
     * Thông báo cho user rằng có tin nhắn mới trong phòng chat.
     *
     * @param recipientUserId ID người nhận thông báo
     * @param senderUsername  Tên người gửi tin nhắn
     * @param roomId          ID phòng chat (buyer_{x}_seller_{y})
     */
    public void notifyNewMessage(Long recipientUserId, String senderUsername, String roomId) {
        send(recipientUserId, NotificationPayload.builder()
                .type(NotificationPayload.NotificationType.NEW_MESSAGE)
                .title("Tin nhắn mới")
                .message(senderUsername + " đã gửi cho bạn một tin nhắn")
                .referenceId(roomId)
                .timestamp(LocalDateTime.now())
                .build());
    }

    // ── Trigger: Đặt hàng thành công ──────────────────────────────────────────

    /**
     * Thông báo đặt hàng thành công.
     *
     * @param userId  ID người đặt hàng
     * @param orderId Mã đơn hàng
     */
    public void notifyOrderPlaced(Long userId, String orderId, String orderCode) {
        send(userId, NotificationPayload.builder()
                .type(NotificationPayload.NotificationType.ORDER_PLACED)
                .title("Đặt hàng thành công")
                .message("Đơn hàng " + orderCode + " đã được tạo thành công")
                .referenceId(orderId)
                .timestamp(LocalDateTime.now())
                .build());
    }

    /**
     * Broadcasts a newly committed order to every connected admin client.
     */
    public void notifyAdminNewOrder(String orderId, String orderCode) {
        NotificationPayload payload = NotificationPayload.builder()
                .type(NotificationPayload.NotificationType.ADMIN_NEW_ORDER)
                .title("Có đơn hàng mới cần duyệt")
                .message("Đơn hàng " + orderCode + " vừa được tạo")
                .referenceId(orderId)
                .timestamp(LocalDateTime.now())
                .build();
        messagingTemplate.convertAndSend(ADMIN_ORDER_TOPIC, payload);
        log.info("[NOTIFY] → {} | orderId={} | orderCode={}",
                ADMIN_ORDER_TOPIC, orderId, orderCode);
    }

    // ── Trigger: Thanh toán thành công ───────────────────────────────────────

    /**
     * Thông báo thanh toán thành công.
     *
     * @param userId    ID người dùng
     * @param paymentId ID giao dịch thanh toán
     */
    public void notifyPaymentSuccess(Long userId, String paymentId) {
        send(userId, NotificationPayload.builder()
                .type(NotificationPayload.NotificationType.PAYMENT_SUCCESS)
                .title("Thanh toán thành công")
                .message("Giao dịch #" + paymentId + " đã hoàn tất")
                .referenceId(paymentId)
                .timestamp(LocalDateTime.now())
                .build());
    }

    // ── Trigger: Thanh toán thất bại ─────────────────────────────────────────

    /**
     * Thông báo thanh toán thất bại.
     *
     * @param userId    ID người dùng
     * @param paymentId ID giao dịch thanh toán
     */
    public void notifyPaymentFailed(Long userId, String paymentId) {
        send(userId, NotificationPayload.builder()
                .type(NotificationPayload.NotificationType.PAYMENT_FAILED)
                .title("Thanh toán thất bại")
                .message("Giao dịch #" + paymentId + " không thành công. Vui lòng thử lại.")
                .referenceId(paymentId)
                .timestamp(LocalDateTime.now())
                .build());
    }

    // ── Trigger: Cập nhật trạng thái đơn hàng ────────────────────────────────

    /**
     * Thông báo trạng thái đơn hàng thay đổi.
     *
     * @param userId    ID người dùng
     * @param orderId   Mã đơn hàng
     * @param newStatus Trạng thái mới (ví dụ: "SHIPPED", "DELIVERED")
     */
    public void notifyOrderStatusUpdated(
            Long userId,
            String orderId,
            String orderCode,
            String newStatus
    ) {
        send(userId, NotificationPayload.builder()
                .type(NotificationPayload.NotificationType.ORDER_STATUS_UPDATED)
                .title("Cập nhật đơn hàng")
                .message("Đơn hàng " + orderCode + " đã chuyển sang trạng thái: " + newStatus)
                .referenceId(orderId)
                .timestamp(LocalDateTime.now())
                .build());
    }

    // ── Trigger: Thông báo hệ thống chung ────────────────────────────────────

    /**
     * Gửi thông báo hệ thống tuỳ chỉnh.
     *
     * @param userId  ID người nhận
     * @param title   Tiêu đề
     * @param message Nội dung
     */
    public void notifySystem(Long userId, String title, String message) {
        send(userId, NotificationPayload.builder()
                .type(NotificationPayload.NotificationType.SYSTEM)
                .title(title)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build());
    }
}
