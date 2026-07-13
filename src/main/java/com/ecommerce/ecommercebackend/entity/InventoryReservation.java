package com.ecommerce.ecommercebackend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * B07 – Bản ghi giữ hàng tạm thời khi bắt đầu checkout.
 *
 * <p>Khi user bắt đầu đặt hàng, hệ thống tạo một {@link InventoryReservation}
 * để "lock" số lượng hàng trong một khoảng thời gian (mặc định 15 phút).
 * Nếu hết hạn mà chưa thanh toán, hàng được trả lại kho.</p>
 *
 * <p>Cơ chế:
 * <pre>
 * User checkout
 *   → InventoryReservationService.reserve(productId/variantId, qty, userId)
 *     → reservedQuantity += qty trên Product/ProductVariant
 *     → tạo InventoryReservation với expiresAt = now + 15 phút
 *   → trả expiresAt cho frontend (đếm ngược)
 *   → Thanh toán thành công → confirm reservation → stockQuantity -= qty
 *   → Thanh toán thất bại / hết hạn → release reservation → reservedQuantity -= qty
 * </pre>
 * </p>
 */
@Entity
@Table(
        name = "inventory_reservations",
        indexes = {
                @Index(name = "idx_reservation_user",      columnList = "user_id"),
                @Index(name = "idx_reservation_expires_at", columnList = "expires_at")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** User đang checkout. */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** Sản phẩm được giữ. */
    @Column(name = "product_id", nullable = false)
    private Long productId;

    /** Variant được giữ (null nếu không có variant). */
    @Column(name = "variant_id")
    private Long variantId;

    /** Số lượng đang được giữ. */
    @Column(nullable = false)
    private int quantity;

    /** Thời điểm reservation hết hạn — frontend dùng để đếm ngược. */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /** Trạng thái: PENDING → CONFIRMED hoặc RELEASED. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ReservationStatus status = ReservationStatus.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum ReservationStatus {
        PENDING,    // Đang giữ hàng, chờ thanh toán
        CONFIRMED,  // Thanh toán thành công, hàng đã trừ khỏi stock
        RELEASED    // Hết hạn hoặc huỷ, hàng đã trả lại kho
    }

    /** @return {@code true} nếu reservation đã hết hạn */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
