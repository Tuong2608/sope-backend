package com.ecommerce.ecommercebackend.service;

import com.ecommerce.ecommercebackend.entity.InventoryReservation;
import com.ecommerce.ecommercebackend.entity.Product;
import com.ecommerce.ecommercebackend.entity.ProductVariant;
import com.ecommerce.ecommercebackend.exception.BadRequestException;
import com.ecommerce.ecommercebackend.exception.ResourceNotFoundException;
import com.ecommerce.ecommercebackend.repository.InventoryReservationRepository;
import com.ecommerce.ecommercebackend.repository.ProductRepository;
import com.ecommerce.ecommercebackend.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * B07 – Service giữ hàng tạm thời khi bắt đầu checkout.
 *
 * <h2>Luồng hoạt động:</h2>
 * <pre>
 * 1. reserve()  → tạo InventoryReservation, tăng reservedQuantity, trả expiresAt
 * 2a. confirm() → thanh toán thành công: giảm stockQuantity, giảm reservedQuantity, status=CONFIRMED
 * 2b. release() → huỷ/timeout: giảm reservedQuantity, status=RELEASED
 * 3. @Scheduled cleanup → tự động release các reservation hết hạn mỗi phút
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryReservationService {

    /** Thời gian giữ hàng (phút). */
    private static final int RESERVATION_MINUTES = 15;

    private final InventoryReservationRepository reservationRepository;
    private final ProductRepository              productRepository;
    private final ProductVariantRepository       variantRepository;

    // ── Reserve ───────────────────────────────────────────────────────────────

    /**
     * Giữ hàng tạm thời cho một user khi bắt đầu checkout.
     *
     * @param userId    ID user
     * @param productId ID sản phẩm
     * @param variantId ID variant (null nếu không có)
     * @param quantity  Số lượng muốn giữ
     * @return Reservation vừa tạo (chứa {@code expiresAt} để frontend đếm ngược)
     */
    @Transactional
    public InventoryReservation reserve(Long userId, Long productId, Long variantId, int quantity) {
        // Kiểm tra tồn kho
        int available = getAvailableQty(productId, variantId);
        if (quantity > available) {
            throw new BadRequestException(
                    "Chỉ còn " + available + " sản phẩm có thể đặt hàng");
        }

        // Tăng reservedQuantity
        incrementReserved(productId, variantId, quantity);

        // Tạo reservation record
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(RESERVATION_MINUTES);
        InventoryReservation reservation = InventoryReservation.builder()
                .userId(userId)
                .productId(productId)
                .variantId(variantId)
                .quantity(quantity)
                .expiresAt(expiresAt)
                .status(InventoryReservation.ReservationStatus.PENDING)
                .build();

        reservation = reservationRepository.save(reservation);
        log.info("[B07] Reserve: user={} product={} variant={} qty={} expires={}",
                userId, productId, variantId, quantity, expiresAt);
        return reservation;
    }

    // ── Confirm (thanh toán thành công) ───────────────────────────────────────

    /**
     * Xác nhận reservation sau khi thanh toán thành công.
     * Trừ stockQuantity và reservedQuantity.
     */
    @Transactional
    public void confirm(Long reservationId) {
        InventoryReservation res = findOrThrow(reservationId);
        if (res.getStatus() != InventoryReservation.ReservationStatus.PENDING) {
            log.warn("[B07] Confirm bị bỏ qua: reservation #{} status={}", reservationId, res.getStatus());
            return;
        }

        // Trừ stock và reserved
        decrementStock(res.getProductId(), res.getVariantId(), res.getQuantity());
        res.setStatus(InventoryReservation.ReservationStatus.CONFIRMED);
        reservationRepository.save(res);
        log.info("[B07] Confirm reservation #{} — stock giảm {}", reservationId, res.getQuantity());
    }

    // ── Release (huỷ / timeout) ───────────────────────────────────────────────

    /**
     * Giải phóng reservation (hủy đơn hoặc thanh toán thất bại).
     * Trả reservedQuantity về kho.
     */
    @Transactional
    public void release(Long reservationId) {
        InventoryReservation res = findOrThrow(reservationId);
        if (res.getStatus() != InventoryReservation.ReservationStatus.PENDING) return;

        decrementReserved(res.getProductId(), res.getVariantId(), res.getQuantity());
        res.setStatus(InventoryReservation.ReservationStatus.RELEASED);
        reservationRepository.save(res);
        log.info("[B07] Release reservation #{} — trả lại {} sản phẩm vào kho", reservationId, res.getQuantity());
    }

    // ── Scheduled cleanup ─────────────────────────────────────────────────────

    /**
     * B07: Tự động release các reservation hết hạn, chạy mỗi 60 giây.
     */
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void releaseExpiredReservations() {
        List<InventoryReservation> expired = reservationRepository.findExpiredPending(LocalDateTime.now());
        if (expired.isEmpty()) return;

        log.info("[B07] Cleanup: tìm thấy {} reservation hết hạn", expired.size());
        for (InventoryReservation res : expired) {
            try {
                decrementReserved(res.getProductId(), res.getVariantId(), res.getQuantity());
                res.setStatus(InventoryReservation.ReservationStatus.RELEASED);
                reservationRepository.save(res);
                log.debug("[B07] Auto-released reservation #{}", res.getId());
            } catch (Exception e) {
                log.error("[B07] Lỗi release reservation #{}: {}", res.getId(), e.getMessage());
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private int getAvailableQty(Long productId, Long variantId) {
        if (variantId != null) {
            return variantRepository.findById(variantId)
                    .map(ProductVariant::getAvailableQuantity)
                    .orElseThrow(() -> new ResourceNotFoundException("Variant not found: " + variantId));
        }
        return productRepository.findById(productId)
                .map(Product::getAvailableQuantity)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
    }

    private void incrementReserved(Long productId, Long variantId, int qty) {
        if (variantId != null) {
            ProductVariant v = variantRepository.findById(variantId).orElseThrow();
            v.setReservedQuantity(v.getReservedQuantity() + qty);
            variantRepository.save(v);
        } else {
            Product p = productRepository.findById(productId).orElseThrow();
            p.setReservedQuantity(p.getReservedQuantity() + qty);
            productRepository.save(p);
        }
    }

    private void decrementReserved(Long productId, Long variantId, int qty) {
        if (variantId != null) {
            ProductVariant v = variantRepository.findById(variantId).orElseThrow();
            v.setReservedQuantity(Math.max(0, v.getReservedQuantity() - qty));
            variantRepository.save(v);
        } else {
            Product p = productRepository.findById(productId).orElseThrow();
            p.setReservedQuantity(Math.max(0, p.getReservedQuantity() - qty));
            productRepository.save(p);
        }
    }

    private void decrementStock(Long productId, Long variantId, int qty) {
        if (variantId != null) {
            ProductVariant v = variantRepository.findById(variantId).orElseThrow();
            v.setStockQuantity(Math.max(0, v.getStockQuantity() - qty));
            v.setReservedQuantity(Math.max(0, v.getReservedQuantity() - qty));
            variantRepository.save(v);
        } else {
            Product p = productRepository.findById(productId).orElseThrow();
            p.setStockQuantity(Math.max(0, p.getStockQuantity() - qty));
            p.setReservedQuantity(Math.max(0, p.getReservedQuantity() - qty));
            productRepository.save(p);
        }
    }

    private InventoryReservation findOrThrow(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + id));
    }
}
