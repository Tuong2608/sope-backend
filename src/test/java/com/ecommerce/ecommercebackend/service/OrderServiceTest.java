package com.ecommerce.ecommercebackend.service;

import com.ecommerce.ecommercebackend.dto.request.CreateOrderRequest;
import com.ecommerce.ecommercebackend.dto.response.DeliveryEstimateResponse;
import com.ecommerce.ecommercebackend.dto.response.OrderResponse;
import com.ecommerce.ecommercebackend.entity.Cart;
import com.ecommerce.ecommercebackend.entity.CartItem;
import com.ecommerce.ecommercebackend.entity.Order;
import com.ecommerce.ecommercebackend.entity.OrderStatus;
import com.ecommerce.ecommercebackend.entity.PaymentMethod;
import com.ecommerce.ecommercebackend.entity.Product;
import com.ecommerce.ecommercebackend.entity.Role;
import com.ecommerce.ecommercebackend.entity.User;
import com.ecommerce.ecommercebackend.exception.BadRequestException;
import com.ecommerce.ecommercebackend.repository.CartRepository;
import com.ecommerce.ecommercebackend.repository.CouponRepository;
import com.ecommerce.ecommercebackend.repository.CouponUsageRepository;
import com.ecommerce.ecommercebackend.repository.OrderRepository;
import com.ecommerce.ecommercebackend.service.event.OrderPlacedEvent;
import com.ecommerce.ecommercebackend.service.event.OrderStatusChangedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartService cartService;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private OrderPricingService orderPricingService;

    @Mock
    private DeliveryEstimateService deliveryEstimateService;

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private CouponUsageRepository couponUsageRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private OrderService orderService;

    @Test
    void createOrderSnapshotsCartItemsAndClearsCart() {
        User user = user(1L);
        Product product = product(10L, "Laptop Gaming", 25_000_000L);
        Cart cart = Cart.builder().id(100L).user(user).build();
        CartItem cartItem = CartItem.builder()
                .id(5L)
                .product(product)
                .quantity(2)
                .build();
        cart.addItem(cartItem);
        CreateOrderRequest request = createOrderRequest(PaymentMethod.VNPAY);

        when(cartService.getOrCreateCart(user)).thenReturn(cart);
        when(orderPricingService.price(cart, null, user)).thenReturn(
                OrderPricingResult.builder()
                        .subtotalAmount(50_000_000L)
                        .discountAmount(0L)
                        .appliedCoupon(null)
                        .items(List.of(OrderPricingResult.Item.builder()
                                .cartItem(cartItem)
                                .unitPrice(25_000_000L)
                                .lineTotal(50_000_000L)
                                .discountAmount(0L)
                                .build()))
                        .build());
        when(deliveryEstimateService.estimate(any())).thenReturn(
                DeliveryEstimateResponse.builder()
                        .zoneName("Miền Nam")
                        .methodCode("STANDARD")
                        .methodName("Giao hàng tiêu chuẩn")
                        .fee(25_000L)
                        .estimatedMinDate(LocalDate.now().plusDays(2))
                        .estimatedMaxDate(LocalDate.now().plusDays(4))
                        .build());
        when(orderRepository.existsByOrderCode(anyString())).thenReturn(false);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(200L);
            return order;
        });
        when(cartRepository.save(cart)).thenReturn(cart);

        OrderResponse response = orderService.createOrder(user, request);

        assertThat(response.getId()).isEqualTo(200L);
        assertThat(response.getOrderCode()).startsWith("ORD-");
        assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.getPaymentMethod()).isEqualTo(PaymentMethod.VNPAY);
        assertThat(response.getSubtotalAmount()).isEqualTo(50_000_000L);
        assertThat(response.getShippingFee()).isEqualTo(25_000L);
        assertThat(response.getTotalAmount()).isEqualTo(50_025_000L);
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getProductId()).isEqualTo(10L);
        assertThat(response.getItems().get(0).getProductName()).isEqualTo("Laptop Gaming");
        assertThat(response.getItems().get(0).getUnitPrice()).isEqualTo(25_000_000L);
        assertThat(response.getItems().get(0).getQuantity()).isEqualTo(2);
        assertThat(cart.getItems()).isEmpty();
        verify(cartRepository).save(cart);
        verify(eventPublisher).publishEvent(any(OrderPlacedEvent.class));
    }

    @Test
    void createOrderRejectsEmptyCart() {
        User user = user(1L);
        Cart emptyCart = Cart.builder().id(100L).user(user).build();

        when(cartService.getOrCreateCart(user)).thenReturn(emptyCart);

        assertThatThrownBy(() -> orderService.createOrder(user, createOrderRequest(PaymentMethod.COD)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("tr");

        verify(orderRepository, never()).save(any(Order.class));
        verify(cartRepository, never()).save(any(Cart.class));
    }

    // ── C07: order status transitions ───────────────────────────────────────────

    @Test
    void updateStatusFromPendingToPaidDeductsStock() {
        Order order = orderInStatus(OrderStatus.PENDING);
        order.setPaymentMethod(PaymentMethod.VNPAY);
        when(orderRepository.findById(1L)).thenReturn(java.util.Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);
        when(couponUsageRepository.findByOrderId(order.getId())).thenReturn(java.util.Optional.empty());

        OrderResponse response = orderService.updateStatus(1L, OrderStatus.PAID);

        assertThat(response.getStatus()).isEqualTo(OrderStatus.PAID);
        verify(inventoryService).deductStockForOrder(order);
        verify(inventoryService, never()).restoreStockForOrder(any(Order.class));
        verify(eventPublisher).publishEvent(any(OrderStatusChangedEvent.class));
    }

    @Test
    void updateStatusApprovesCodDirectlyFromPendingToProcessing() {
        Order order = orderInStatus(OrderStatus.PENDING);
        when(orderRepository.findById(1L)).thenReturn(java.util.Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);
        when(couponUsageRepository.findByOrderId(order.getId())).thenReturn(java.util.Optional.empty());

        OrderResponse response = orderService.updateStatus(1L, OrderStatus.PROCESSING);

        assertThat(response.getStatus()).isEqualTo(OrderStatus.PROCESSING);
        verify(inventoryService).deductStockForOrder(order);
        verify(eventPublisher).publishEvent(any(OrderStatusChangedEvent.class));
    }

    @Test
    void updateStatusRejectsProcessingUnpaidOnlineOrder() {
        Order order = orderInStatus(OrderStatus.PENDING);
        order.setPaymentMethod(PaymentMethod.MOMO);
        when(orderRepository.findById(1L)).thenReturn(java.util.Optional.of(order));

        assertThatThrownBy(() -> orderService.updateStatus(1L, OrderStatus.PROCESSING))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("PAID");

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void updateStatusFromPaidToCancelledRestoresStock() {
        Order order = orderInStatus(OrderStatus.PAID);
        when(orderRepository.findById(1L)).thenReturn(java.util.Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);
        when(couponUsageRepository.findByOrderId(order.getId())).thenReturn(java.util.Optional.empty());

        OrderResponse response = orderService.updateStatus(1L, OrderStatus.CANCELLED);

        assertThat(response.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(inventoryService).restoreStockForOrder(order);
        verify(inventoryService, never()).deductStockForOrder(any(Order.class));
    }

    @Test
    void updateStatusFromPendingToCancelledDoesNotTouchStock() {
        Order order = orderInStatus(OrderStatus.PENDING);
        when(orderRepository.findById(1L)).thenReturn(java.util.Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);
        when(couponUsageRepository.findByOrderId(order.getId())).thenReturn(java.util.Optional.empty());

        orderService.updateStatus(1L, OrderStatus.CANCELLED);

        verify(inventoryService, never()).restoreStockForOrder(any(Order.class));
        verify(inventoryService, never()).deductStockForOrder(any(Order.class));
    }

    @Test
    void updateStatusRejectsSkippingSteps() {
        Order order = orderInStatus(OrderStatus.PENDING);
        when(orderRepository.findById(1L)).thenReturn(java.util.Optional.of(order));

        assertThatThrownBy(() -> orderService.updateStatus(1L, OrderStatus.SHIPPING))
                .isInstanceOf(BadRequestException.class);

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void updateStatusRejectsTransitionFromTerminalStatus() {
        Order order = orderInStatus(OrderStatus.COMPLETED);
        when(orderRepository.findById(1L)).thenReturn(java.util.Optional.of(order));

        assertThatThrownBy(() -> orderService.updateStatus(1L, OrderStatus.CANCELLED))
                .isInstanceOf(BadRequestException.class);
    }

    private Order orderInStatus(OrderStatus status) {
        return Order.builder()
                .id(1L)
                .orderCode("ORD-20260714-ABCDEF")
                .user(user(1L))
                .status(status)
                .paymentMethod(PaymentMethod.COD)
                .totalAmount(1_000_000L)
                .recipientName("Nguyen Van Nam")
                .phone("0901234567")
                .shippingAddress("1 Nguyen Hue")
                .build();
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

    private Product product(Long id, String name, Long price) {
        return Product.builder()
                .id(id)
                .name(name)
                .price(price)
                .build();
    }

    private CreateOrderRequest createOrderRequest(PaymentMethod paymentMethod) {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setRecipientName("Nguyen Van Nam");
        request.setPhone("0901234567");
        request.setShippingAddress("1 Nguyen Hue, Quan 1, TP HCM");
        request.setProvince("TP. Hồ Chí Minh");
        request.setShippingMethodCode("STANDARD");
        request.setNote("Giao gio hanh chinh");
        request.setPaymentMethod(paymentMethod);
        return request;
    }
}
