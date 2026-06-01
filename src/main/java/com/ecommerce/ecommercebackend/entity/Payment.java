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
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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
    @Column(nullable = false, length = 10)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    /**
     * Mã giao dịch do cổng thanh toán cấp.
     * Được điền sau khi nhận IPN thành công.
     */
    @Column(name = "transaction_id", length = 100)
    private String transactionId;

    /**
     * Mô tả nội dung thanh toán.
     * Ví dụ: "Thanh toan don hang ORDER_001"
     */
    @Column(name = "order_info", length = 255)
    private String orderInfo;

    /** URL trang thanh toán để chuyển hướng người dùng. */
    @Column(name = "payment_url", columnDefinition = "TEXT")
    private String paymentUrl;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
