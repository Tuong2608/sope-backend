package com.ecommerce.ecommercebackend.service;

import com.ecommerce.ecommercebackend.dto.request.PaymentRequest;
import com.ecommerce.ecommercebackend.dto.response.PaymentResponse;
import com.ecommerce.ecommercebackend.entity.Order;
import com.ecommerce.ecommercebackend.entity.OrderStatus;
import com.ecommerce.ecommercebackend.entity.Payment;
import com.ecommerce.ecommercebackend.entity.PaymentMethod;
import com.ecommerce.ecommercebackend.entity.PaymentProvider;
import com.ecommerce.ecommercebackend.entity.PaymentStatus;
import com.ecommerce.ecommercebackend.entity.Role;
import com.ecommerce.ecommercebackend.entity.User;
import com.ecommerce.ecommercebackend.exception.BadRequestException;
import com.ecommerce.ecommercebackend.repository.OrderRepository;
import com.ecommerce.ecommercebackend.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private VnpayService vnpayService;

    @Mock
    private MomoService momoService;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void createPaymentCreatesVnpayTransactionForOwnedOrder() {
        User user = user(1L);
        Order order = order("ORD-20260708-ABC123", user, PaymentMethod.VNPAY, 1_500_000L);
        PaymentRequest request = paymentRequest(order.getOrderCode(), 1_500_000L, PaymentProvider.VNPAY);

        when(orderRepository.findByOrderCodeAndUserId(order.getOrderCode(), 1L))
                .thenReturn(Optional.of(order));
        when(paymentRepository.findFirstByOrderIdAndStatusOrderByCreatedAtDesc(
                order.getOrderCode(), PaymentStatus.PENDING))
                .thenReturn(Optional.empty());
        when(vnpayService.createPaymentUrl(
                eq(order.getOrderCode()),
                eq(1_500_000L),
                eq("Thanh toan don hang " + order.getOrderCode()),
                eq("127.0.0.1")))
                .thenReturn("https://sandbox.vnpay.vn/pay");
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(300L);
            return payment;
        });

        PaymentResponse response = paymentService.createPayment(user, request, "127.0.0.1");

        assertThat(response.getId()).isEqualTo(300L);
        assertThat(response.getOrderId()).isEqualTo(order.getOrderCode());
        assertThat(response.getAmount()).isEqualTo(1_500_000L);
        assertThat(response.getProvider()).isEqualTo(PaymentProvider.VNPAY);
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(response.getPaymentUrl()).isEqualTo("https://sandbox.vnpay.vn/pay");
        verify(momoService, never()).createPaymentUrl(any(), any(), any());
    }

    @Test
    void createPaymentRejectsAmountMismatch() {
        User user = user(1L);
        Order order = order("ORD-20260708-ABC123", user, PaymentMethod.VNPAY, 1_500_000L);
        PaymentRequest request = paymentRequest(order.getOrderCode(), 1_000_000L, PaymentProvider.VNPAY);

        when(orderRepository.findByOrderCodeAndUserId(order.getOrderCode(), 1L))
                .thenReturn(Optional.of(order));

        assertThatThrownBy(() -> paymentService.createPayment(user, request, "127.0.0.1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("amount");

        verify(paymentRepository, never()).save(any(Payment.class));
        verify(vnpayService, never()).createPaymentUrl(any(), any(), any(), any());
        verify(momoService, never()).createPaymentUrl(any(), any(), any());
    }

    @Test
    void handleMomoIpnSuccessMarksPaymentAndOrderPaid() {
        String orderCode = "ORD-20260708-ABC123";
        Payment payment = Payment.builder()
                .id(300L)
                .orderId(orderCode)
                .amount(1_500_000L)
                .provider(PaymentProvider.MOMO)
                .status(PaymentStatus.PENDING)
                .build();
        Order order = order(orderCode, user(1L), PaymentMethod.MOMO, 1_500_000L);
        Map<String, String> params = Map.of(
                "orderId", orderCode,
                "amount", "1500000",
                "resultCode", "0",
                "transId", "MOMO123"
        );

        when(momoService.verifySignature(params)).thenReturn(true);
        when(paymentRepository.findFirstByOrderIdAndStatusOrderByCreatedAtDesc(
                orderCode, PaymentStatus.PENDING))
                .thenReturn(Optional.of(payment));
        when(momoService.resolveStatus(params)).thenReturn(PaymentStatus.SUCCESS);
        when(momoService.extractTransactionId(params)).thenReturn("MOMO123");
        when(orderRepository.findByOrderCode(orderCode)).thenReturn(Optional.of(order));

        paymentService.handleMomoIpn(params);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(payment.getTransactionId()).isEqualTo("MOMO123");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        verify(paymentRepository).save(payment);
        verify(orderRepository).save(order);
    }

    private User user(Long id) {
        return User.builder()
                .id(id)
                .username("nam")
                .email("nam@example.com")
                .password("secret")
                .role(Role.ROLE_USER)
                .build();
    }

    private Order order(String orderCode, User user, PaymentMethod paymentMethod, Long totalAmount) {
        return Order.builder()
                .id(200L)
                .orderCode(orderCode)
                .user(user)
                .status(OrderStatus.PENDING)
                .paymentMethod(paymentMethod)
                .totalAmount(totalAmount)
                .recipientName("Nguyen Van Nam")
                .phone("0901234567")
                .shippingAddress("1 Nguyen Hue, Quan 1, TP HCM")
                .build();
    }

    private PaymentRequest paymentRequest(String orderId, Long amount, PaymentProvider provider) {
        PaymentRequest request = new PaymentRequest();
        request.setOrderId(orderId);
        request.setAmount(amount);
        request.setProvider(provider);
        request.setOrderInfo("Thanh toan don hang " + orderId);
        return request;
    }
}
