package com.ecommerce.ecommercebackend.service;

import com.ecommerce.ecommercebackend.service.event.OrderPlacedEvent;
import com.ecommerce.ecommercebackend.service.event.OrderStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Bridges committed order transactions to WebSocket notifications.
 *
 * <p>Using AFTER_COMMIT prevents an admin client from refreshing the order
 * table before the newly-created order is visible in the database.</p>
 */
@Component
@RequiredArgsConstructor
public class OrderNotificationListener {

    private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderPlaced(OrderPlacedEvent event) {
        notificationService.notifyOrderPlaced(
                event.userId(),
                event.orderId().toString(),
                event.orderCode());
        notificationService.notifyAdminNewOrder(
                event.orderId().toString(),
                event.orderCode());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderStatusChanged(OrderStatusChangedEvent event) {
        notificationService.notifyOrderStatusUpdated(
                event.userId(),
                event.orderId().toString(),
                event.orderCode(),
                event.status().name());
    }
}
