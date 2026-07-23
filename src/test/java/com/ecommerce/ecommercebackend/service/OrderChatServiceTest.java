package com.ecommerce.ecommercebackend.service;

import com.ecommerce.ecommercebackend.entity.Order;
import com.ecommerce.ecommercebackend.entity.OrderStatus;
import com.ecommerce.ecommercebackend.entity.PaymentMethod;
import com.ecommerce.ecommercebackend.entity.Role;
import com.ecommerce.ecommercebackend.entity.User;
import com.ecommerce.ecommercebackend.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderChatServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderChatService orderChatService;

    @Test
    void ignoresProductQuestionsSoGeminiCanHandleThem() {
        Optional<String> answer = orderChatService.answer(
                user(7L),
                "Gợi ý cho tôi một chiếc laptop dưới 20 triệu");

        assertThat(answer).isEmpty();
        verify(orderRepository, never()).findByUserIdOrderByCreatedAtDesc(7L);
    }

    @Test
    void asksAnonymousCustomerToLoginWithoutQueryingOrders() {
        Optional<String> answer = orderChatService.answer(
                null,
                "Đơn gần nhất của tôi đang ở đâu?");

        assertThat(answer).hasValueSatisfying(value -> {
            assertThat(value).contains("đăng nhập");
            assertThat(value).contains("(/login)");
        });
        verify(orderRepository, never()).findByUserIdOrderByCreatedAtDesc(7L);
    }

    @Test
    void explicitOrderCodeIsAlwaysScopedToAuthenticatedOwner() {
        User user = user(7L);
        Order order = order(21L, "ORD-20260723-ABC123", OrderStatus.PROCESSING);
        when(orderRepository.findByOrderCodeAndUserId("ORD-20260723-ABC123", 7L))
                .thenReturn(Optional.of(order));

        Optional<String> answer = orderChatService.answer(
                user,
                "Kiểm tra giúp tôi trạng thái ord-20260723-abc123");

        assertThat(answer).hasValueSatisfying(value -> {
            assertThat(value).contains("ORD-20260723-ABC123");
            assertThat(value).contains("Đã duyệt · Đang xử lý");
            assertThat(value).contains("(/orders/21)");
            assertThat(value).doesNotContain(order.getShippingAddress());
        });
        verify(orderRepository)
                .findByOrderCodeAndUserId("ORD-20260723-ABC123", 7L);
        verify(orderRepository, never()).findByOrderCode("ORD-20260723-ABC123");
    }

    @Test
    void latestOrderQuestionReturnsRealStatusAndDeliveryWindow() {
        Order latest = order(22L, "ORD-20260723-NEWEST1", OrderStatus.SHIPPING);
        Order older = order(21L, "ORD-20260722-OLDER01", OrderStatus.COMPLETED);
        when(orderRepository.findByUserIdOrderByCreatedAtDesc(7L))
                .thenReturn(List.of(latest, older));

        Optional<String> answer = orderChatService.answer(
                user(7L),
                "Đơn hàng gần nhất của tôi đang ở đâu?");

        assertThat(answer).hasValueSatisfying(value -> {
            assertThat(value).contains("ORD-20260723-NEWEST1");
            assertThat(value).contains("Đang giao");
            assertThat(value).contains("25/07/2026 - 27/07/2026");
            assertThat(value).contains("Bạn còn 1 đơn khác");
        });
    }

    @Test
    void canListOrdersFilteredByRequestedStatus() {
        Order shipping = order(22L, "ORD-20260723-SHIP001", OrderStatus.SHIPPING);
        Order pending = order(21L, "ORD-20260723-WAIT001", OrderStatus.PENDING);
        when(orderRepository.findByUserIdOrderByCreatedAtDesc(7L))
                .thenReturn(List.of(shipping, pending));

        Optional<String> answer = orderChatService.answer(
                user(7L),
                "Tôi có đơn nào đang giao không?");

        assertThat(answer).hasValueSatisfying(value -> {
            assertThat(value).contains("ORD-20260723-SHIP001");
            assertThat(value).doesNotContain("ORD-20260723-WAIT001");
        });
    }

    @Test
    void missingCodeDoesNotRevealWhetherAnotherCustomerOwnsIt() {
        when(orderRepository.findByOrderCodeAndUserId("ORD-20260723-PRIVATE", 7L))
                .thenReturn(Optional.empty());

        Optional<String> answer = orderChatService.answer(
                user(7L),
                "Đơn ORD-20260723-PRIVATE đang thế nào?");

        assertThat(answer).hasValueSatisfying(value ->
                assertThat(value).contains("trong tài khoản đang đăng nhập"));
        verify(orderRepository, never()).findByOrderCode("ORD-20260723-PRIVATE");
    }

    private User user(Long id) {
        return User.builder()
                .id(id)
                .username("customer-" + id)
                .email("customer-" + id + "@example.com")
                .password("secret")
                .role(Role.ROLE_USER)
                .build();
    }

    private Order order(Long id, String code, OrderStatus status) {
        return Order.builder()
                .id(id)
                .orderCode(code)
                .status(status)
                .paymentMethod(PaymentMethod.COD)
                .totalAmount(1_250_000L)
                .shippingAddress("Thông tin riêng không được đưa vào câu trả lời")
                .createdAt(LocalDateTime.of(2026, 7, 23, 10, 30))
                .updatedAt(LocalDateTime.of(2026, 7, 23, 11, 45))
                .estimatedDeliveryMinDate(LocalDate.of(2026, 7, 25))
                .estimatedDeliveryMaxDate(LocalDate.of(2026, 7, 27))
                .build();
    }
}
