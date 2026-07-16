package com.ecommerce.ecommercebackend.service;

import com.ecommerce.ecommercebackend.dto.request.PaymentRequest;
import com.ecommerce.ecommercebackend.dto.response.PaymentResponse;
import com.ecommerce.ecommercebackend.entity.*;
import com.ecommerce.ecommercebackend.repository.OrderRepository;
import com.ecommerce.ecommercebackend.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock PaymentRepository paymentRepository;
    @Mock OrderRepository orderRepository;
    @Mock OrderService orderService;
    @Mock VnpayService vnpayService;
    @Mock MomoService momoService;
    @InjectMocks PaymentService paymentService;

    @Test
    void createPaymentUsesTrustedOrderAmountAndPersistsBeforeProviderCall() {
        User user = user(1L);
        Order order = order("ORD-20260717-ABC123", user, PaymentMethod.VNPAY, 1_500_000L);
        PaymentRequest request = request(order.getOrderCode(), PaymentProvider.VNPAY, "VNPAYQR");
        when(orderRepository.findByOrderCodeAndUserId(order.getOrderCode(), user.getId()))
                .thenReturn(Optional.of(order));
        when(paymentRepository.findFirstByOrderIdAndProviderAndStatusInOrderByCreatedAtDesc(
                eq(order.getOrderCode()), eq(PaymentProvider.VNPAY), anyCollection()))
                .thenReturn(Optional.empty());
        when(paymentRepository.saveAndFlush(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(300L);
            return payment;
        });
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(vnpayService.createPaymentUrl(
                anyString(), eq(1_500_000L), contains(order.getOrderCode()), eq("127.0.0.1"),
                eq("VNPAYQR"), any(LocalDateTime.class)))
                .thenReturn("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?valid=true");

        PaymentResponse response = paymentService.createPayment(user, request, "127.0.0.1");

        assertThat(response.getPaymentId()).isEqualTo(300L);
        assertThat(response.getAmount()).isEqualTo(order.getTotalAmount());
        assertThat(response.getPaymentChannel()).isEqualTo("VNPAYQR");
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.PENDING);
        verify(paymentRepository).saveAndFlush(any(Payment.class));
        verify(vnpayService).createPaymentUrl(anyString(), eq(order.getTotalAmount()), anyString(),
                anyString(), eq("VNPAYQR"), any(LocalDateTime.class));
    }

    @Test
    void momoIpnSuccessCompletesOrderExactlyOnce() {
        Payment payment = pendingMomoPayment();
        Map<String, String> params = Map.of(
                "partnerCode", "MOMO",
                "orderId", payment.getProviderOrderId(),
                "requestId", payment.getProviderRequestId(),
                "amount", "1500000",
                "resultCode", "0",
                "transId", "4088878653",
                "message", "Successful.");
        when(momoService.verifySignature(params)).thenReturn(true);
        when(momoService.hasExpectedPartnerCode(params)).thenReturn(true);
        when(momoService.resolveStatus(params)).thenReturn(PaymentStatus.SUCCESS);
        when(paymentRepository.findLockedByProviderOrderId(payment.getProviderOrderId()))
                .thenReturn(Optional.of(payment));
        when(paymentRepository.save(payment)).thenReturn(payment);

        assertThat(paymentService.handleMomoIpn(params)).isTrue();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(payment.getTransactionId()).isEqualTo("4088878653");
        assertThat(payment.isSignatureVerified()).isTrue();
        verify(orderService).markAsPaid(payment.getOrderId());

        paymentService.handleMomoIpn(params);
        verify(orderService, times(1)).markAsPaid(payment.getOrderId());
    }

    @Test
    void vnpayIpnRejectsWrongAmountWithoutChangingPayment() {
        Payment payment = Payment.builder()
                .id(301L)
                .orderId("ORD-20260717-ABC123")
                .amount(1_500_000L)
                .provider(PaymentProvider.VNPAY)
                .providerOrderId("VP123")
                .status(PaymentStatus.PENDING)
                .build();
        Map<String, String> params = Map.of(
                "vnp_TxnRef", "VP123",
                "vnp_Amount", "100000000",
                "vnp_ResponseCode", "00",
                "vnp_TransactionStatus", "00");
        when(vnpayService.verifySignature(params)).thenReturn(true);
        when(vnpayService.hasExpectedTmnCode(params)).thenReturn(true);
        when(paymentRepository.findLockedByProviderOrderId("VP123")).thenReturn(Optional.of(payment));

        PaymentService.IpnResult result = paymentService.handleVnpayIpn(params);

        assertThat(result.code()).isEqualTo("04");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        verify(paymentRepository, never()).save(any());
        verify(orderService, never()).markAsPaid(anyString());
    }

    private Payment pendingMomoPayment() {
        return Payment.builder()
                .id(300L)
                .orderId("ORD-20260717-ABC123")
                .amount(1_500_000L)
                .provider(PaymentProvider.MOMO)
                .providerOrderId("MM123")
                .providerRequestId("REQ123")
                .status(PaymentStatus.PENDING)
                .build();
    }

    private PaymentRequest request(String orderCode, PaymentProvider provider, String channel) {
        PaymentRequest request = new PaymentRequest();
        request.setOrderId(orderCode);
        request.setProvider(provider);
        request.setChannel(channel);
        return request;
    }

    private User user(Long id) {
        return User.builder().id(id).username("nam").email("nam@example.com")
                .password("secret").role(Role.ROLE_USER).build();
    }

    private Order order(String orderCode, User user, PaymentMethod method, Long total) {
        return Order.builder().id(200L).orderCode(orderCode).user(user).status(OrderStatus.PENDING)
                .paymentMethod(method).totalAmount(total).recipientName("Nguyen Van Nam")
                .phone("0901234567").shippingAddress("1 Nguyen Hue").items(List.of()).build();
    }
}
