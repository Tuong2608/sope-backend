package com.ecommerce.ecommercebackend.service;

import com.ecommerce.ecommercebackend.dto.response.NotificationPayload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Test
    void newOrderUsesDedicatedAdminTopic() {
        NotificationService service = new NotificationService(messagingTemplate);
        ArgumentCaptor<NotificationPayload> payloadCaptor =
                ArgumentCaptor.forClass(NotificationPayload.class);

        service.notifyAdminNewOrder("21", "ORD-20260723-ABC123");

        verify(messagingTemplate).convertAndSend(
                org.mockito.ArgumentMatchers.eq(NotificationService.ADMIN_ORDER_TOPIC),
                payloadCaptor.capture());
        NotificationPayload payload = payloadCaptor.getValue();
        assertThat(payload.getType())
                .isEqualTo(NotificationPayload.NotificationType.ADMIN_NEW_ORDER);
        assertThat(payload.getReferenceId()).isEqualTo("21");
        assertThat(payload.getMessage()).contains("ORD-20260723-ABC123");
    }
}
