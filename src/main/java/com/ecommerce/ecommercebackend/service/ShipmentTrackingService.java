package com.ecommerce.ecommercebackend.service;

import com.ecommerce.ecommercebackend.dto.response.ShipmentTrackingEventResponse;
import com.ecommerce.ecommercebackend.dto.response.ShipmentTrackingResponse;
import com.ecommerce.ecommercebackend.entity.Order;
import com.ecommerce.ecommercebackend.entity.ShipmentStatus;
import com.ecommerce.ecommercebackend.entity.ShipmentTracking;
import com.ecommerce.ecommercebackend.entity.ShipmentTrackingEvent;
import com.ecommerce.ecommercebackend.exception.BadRequestException;
import com.ecommerce.ecommercebackend.exception.ResourceNotFoundException;
import com.ecommerce.ecommercebackend.repository.ShipmentTrackingEventRepository;
import com.ecommerce.ecommercebackend.repository.ShipmentTrackingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;

/**
 * A simulated shipping carrier (task C10): generates tracking numbers and
 * advances a shipment through a fixed set of statuses so the team has
 * something to demo without integrating a real carrier API.
 */
@Service
@RequiredArgsConstructor
public class ShipmentTrackingService {

    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    /** Fixed linear progression; {@code null} after the last status (terminal). */
    private static final ShipmentStatus[] SEQUENCE = {
            ShipmentStatus.CREATED,
            ShipmentStatus.PICKED_UP,
            ShipmentStatus.IN_TRANSIT,
            ShipmentStatus.OUT_FOR_DELIVERY,
            ShipmentStatus.DELIVERED
    };

    private final ShipmentTrackingRepository trackingRepository;
    private final ShipmentTrackingEventRepository eventRepository;

    /**
     * Creates a tracking record for {@code order} if one doesn't already exist
     * (idempotent — safe to call every time an order reaches SHIPPING).
     */
    @Transactional
    public ShipmentTracking createForOrder(Order order) {
        return trackingRepository.findByOrderId(order.getId())
                .orElseGet(() -> {
                    ShipmentTracking tracking = trackingRepository.save(ShipmentTracking.builder()
                            .order(order)
                            .trackingNumber(generateUniqueTrackingNumber())
                            .status(ShipmentStatus.CREATED)
                            .build());
                    recordEvent(tracking, ShipmentStatus.CREATED, "Đã tạo vận đơn");
                    return tracking;
                });
    }

    /**
     * Moves the shipment to its next status in the fixed sequence.
     *
     * @throws ResourceNotFoundException if the order has no tracking yet
     * @throws BadRequestException       if already {@code DELIVERED}
     */
    @Transactional
    public ShipmentTrackingResponse advance(Long orderId) {
        ShipmentTracking tracking = trackingRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order " + orderId + " has no shipment tracking yet."));

        ShipmentStatus next = nextStatus(tracking.getStatus());
        tracking.setStatus(next);
        trackingRepository.save(tracking);
        recordEvent(tracking, next, null);

        return toResponse(tracking);
    }

    @Transactional(readOnly = true)
    public Optional<ShipmentTrackingResponse> getByOrderId(Long orderId) {
        return trackingRepository.findByOrderId(orderId).map(this::toResponse);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    private ShipmentStatus nextStatus(ShipmentStatus current) {
        int index = indexOf(current);
        if (index == SEQUENCE.length - 1) {
            throw new BadRequestException("Đơn hàng đã được giao — không thể cập nhật thêm.");
        }
        return SEQUENCE[index + 1];
    }

    private int indexOf(ShipmentStatus status) {
        for (int i = 0; i < SEQUENCE.length; i++) {
            if (SEQUENCE[i] == status) {
                return i;
            }
        }
        throw new IllegalStateException("Unknown ShipmentStatus: " + status);
    }

    private void recordEvent(ShipmentTracking tracking, ShipmentStatus status, String note) {
        eventRepository.save(ShipmentTrackingEvent.builder()
                .tracking(tracking)
                .status(status)
                .note(note)
                .build());
    }

    private String generateUniqueTrackingNumber() {
        String number;
        do {
            StringBuilder suffix = new StringBuilder(10);
            for (int i = 0; i < 10; i++) {
                suffix.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
            }
            number = "SPX" + suffix;
        } while (trackingRepository.existsByTrackingNumber(number));
        return number;
    }

    private ShipmentTrackingResponse toResponse(ShipmentTracking tracking) {
        List<ShipmentTrackingEventResponse> events =
                eventRepository.findByTrackingIdOrderByCreatedAtAsc(tracking.getId()).stream()
                        .map(e -> ShipmentTrackingEventResponse.builder()
                                .status(e.getStatus())
                                .note(e.getNote())
                                .createdAt(e.getCreatedAt())
                                .build())
                        .toList();

        return ShipmentTrackingResponse.builder()
                .trackingNumber(tracking.getTrackingNumber())
                .carrierName(tracking.getCarrierName())
                .status(tracking.getStatus())
                .createdAt(tracking.getCreatedAt())
                .updatedAt(tracking.getUpdatedAt())
                .events(events)
                .build();
    }
}
