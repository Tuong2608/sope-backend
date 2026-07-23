package com.ecommerce.ecommercebackend.service;

import com.ecommerce.ecommercebackend.entity.OrderStatus;
import com.ecommerce.ecommercebackend.service.event.OrderPlacedEvent;
import com.ecommerce.ecommercebackend.service.event.OrderStatusChangedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderNotificationListenerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private OrderNotificationListener listener;

    @Test
    void placedOrderNotifiesBuyerAndAdmins() {
        listener.onOrderPlaced(new OrderPlacedEvent(7L, 21L, "ORD-20260723-ABC123"));

        verify(notificationService)
                .notifyOrderPlaced(7L, "21", "ORD-20260723-ABC123");
        verify(notificationService)
                .notifyAdminNewOrder("21", "ORD-20260723-ABC123");
    }

    @Test
    void statusChangeNotifiesOrderOwner() {
        listener.onOrderStatusChanged(new OrderStatusChangedEvent(
                7L,
                21L,
                "ORD-20260723-ABC123",
                OrderStatus.PROCESSING));

        verify(notificationService).notifyOrderStatusUpdated(
                7L,
                "21",
                "ORD-20260723-ABC123",
                "PROCESSING");
    }
}
