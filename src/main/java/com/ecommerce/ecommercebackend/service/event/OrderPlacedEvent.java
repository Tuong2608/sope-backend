package com.ecommerce.ecommercebackend.service.event;

/**
 * Published inside the order transaction and delivered to notification
 * listeners only after that transaction commits successfully.
 */
public record OrderPlacedEvent(
        Long userId,
        Long orderId,
        String orderCode
) {
}
