package com.ecommerce.ecommercebackend.controller;

import com.ecommerce.ecommercebackend.entity.InventoryReservation;
import com.ecommerce.ecommercebackend.entity.User;
import com.ecommerce.ecommercebackend.service.InventoryReservationService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * B07 – REST Controller giữ hàng tạm thời khi bắt đầu checkout.
 *
 * <ul>
 *   <li>POST /api/inventory/reserve       – giữ hàng, trả expiresAt</li>
 *   <li>POST /api/inventory/release/{id}  – giải phóng reservation</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryReservationController {

    private final InventoryReservationService reservationService;

    // ── Request / Response DTOs ───────────────────────────────────────────────

    @Data
    public static class ReserveRequest {
        @NotNull private Long productId;
        private Long variantId;
        @NotNull @Min(1) private Integer quantity;
    }

    public record ReserveResponse(
            Long   reservationId,
            Long   productId,
            Long   variantId,
            int    quantity,
            LocalDateTime expiresAt,
            String message
    ) {}

    // ── Endpoints ─────────────────────────────────────────────────────────────

    /**
     * B07 – Giữ hàng tạm thời 15 phút.
     *
     * <p>POST /api/inventory/reserve<br>
     * Body: {@code { "productId": 1, "variantId": 2, "quantity": 1 }}</p>
     *
     * <p>Response: {@code { "reservationId": 99, "expiresAt": "2026-07-13T12:15:00" }}</p>
     */
    @PostMapping("/reserve")
    public ResponseEntity<ReserveResponse> reserve(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ReserveRequest request) {

        InventoryReservation res = reservationService.reserve(
                user.getId(),
                request.getProductId(),
                request.getVariantId(),
                request.getQuantity()
        );

        return ResponseEntity.ok(new ReserveResponse(
                res.getId(),
                res.getProductId(),
                res.getVariantId(),
                res.getQuantity(),
                res.getExpiresAt(),
                "Đã giữ hàng thành công. Vui lòng thanh toán trước " + res.getExpiresAt()
        ));
    }

    /**
     * Giải phóng reservation (user huỷ checkout).
     *
     * <p>POST /api/inventory/release/{reservationId}</p>
     */
    @PostMapping("/release/{reservationId}")
    public ResponseEntity<String> release(@PathVariable Long reservationId) {
        reservationService.release(reservationId);
        return ResponseEntity.ok("Đã giải phóng reservation #" + reservationId);
    }
}
