package com.ecommerce.ecommercebackend.service;

import com.ecommerce.ecommercebackend.dto.request.CreateOrderRequest;
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
import com.ecommerce.ecommercebackend.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @InjectMocks
    private OrderService orderService;

    @Test
    void createOrderSnapshotsCartItemsAndClearsCart() {
        User user = user(1L);
        Product product = product(10L, "Laptop Gaming", 25_000_000L);
        Cart cart = Cart.builder().id(100L).user(user).build();
        cart.addItem(CartItem.builder()
                .id(5L)
                .product(product)
                .quantity(2)
                .build());
        CreateOrderRequest request = createOrderRequest(PaymentMethod.VNPAY);

        when(cartService.getOrCreateCart(user)).thenReturn(cart);
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
        assertThat(response.getTotalAmount()).isEqualTo(50_000_000L);
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getProductId()).isEqualTo(10L);
        assertThat(response.getItems().get(0).getProductName()).isEqualTo("Laptop Gaming");
        assertThat(response.getItems().get(0).getUnitPrice()).isEqualTo(25_000_000L);
        assertThat(response.getItems().get(0).getQuantity()).isEqualTo(2);
        assertThat(cart.getItems()).isEmpty();
        verify(cartRepository).save(cart);
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
        request.setNote("Giao gio hanh chinh");
        request.setPaymentMethod(paymentMethod);
        return request;
    }
}
