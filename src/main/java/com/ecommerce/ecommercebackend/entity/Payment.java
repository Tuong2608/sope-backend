package com.ecommerce.ecommercebackend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * JPA entity đại diện cho một giao dịch thanh toán.
 *
 * <p>Lưu ý: {@code orderId} hiện là {@code String} để độc lập với
 * Order entity (sẽ được tạo ở Task 5). Sau khi Task 5 hoàn thành,
 * trường này sẽ được migrate thành FK tới bảng {@code orders}.</p>
 */
@Entity
@Table(name = "payments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Optimistic version is a second safety net in addition to repository row locks. */
    @Version
    private Long version;

    /** Mã đơn hàng liên kết với giao dịch này. */
    @Column(name = "order_id", nullable = false, length = 100)
    private String orderId;

    /** Số tiền thanh toán (đơn vị: VND). */
    @Column(nullable = false)
    private Long amount;

    /** Cổng thanh toán được sử dụng (VNPAY hoặc MOMO). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PaymentProvider provider;

    /** Trạng thái hiện tại của giao dịch. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "VND";

    /** Mã duy nhất của lần thử thanh toán gửi sang provider. */
    @Column(name = "provider_order_id", nullable = false, unique = true, length = 100)
    private String providerOrderId;

    @Column(name = "provider_request_id", length = 100)
    private String providerRequestId;

    /**
     * Mã giao dịch do cổng thanh toán cấp.
     * Được điền sau khi nhận IPN thành công.
     */
    @Column(name = "transaction_id", length = 100)
    private String transactionId;

    @Column(name = "payment_channel", length = 30)
    private String paymentChannel;

    @Column(name = "response_code", length = 30)
    private String responseCode;

    @Column(name = "transaction_status", length = 30)
    private String transactionStatus;

    @Column(name = "response_message", length = 500)
    private String responseMessage;

    @Column(name = "bank_code", length = 50)
    private String bankCode;

    @Column(name = "card_type", length = 50)
    private String cardType;

    /**
     * Mô tả nội dung thanh toán.
     * Ví dụ: "Thanh toan don hang ORDER_001"
     */
    @Column(name = "order_info", length = 255)
    private String orderInfo;

    /** URL trang thanh toán để chuyển hướng người dùng. */
    @Column(name = "payment_url", columnDefinition = "TEXT")
    private String paymentUrl;

    @Column(columnDefinition = "TEXT")
    private String deeplink;

    @Column(name = "qr_code_url", columnDefinition = "TEXT")
    private String qrCodeUrl;

    @Column(name = "signature_verified", nullable = false)
    @Builder.Default
    private boolean signatureVerified = false;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "provider_pay_date", length = 30)
    private String providerPayDate;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
