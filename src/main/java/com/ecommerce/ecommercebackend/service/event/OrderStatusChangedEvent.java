package com.ecommerce.ecommercebackend.service.event;

import com.ecommerce.ecommercebackend.entity.OrderStatus;

/**
 * Carries an order status change to the realtime notification layer.
 */
public record OrderStatusChangedEvent(
        Long userId,
        Long orderId,
        String orderCode,
        OrderStatus status
) {
}
