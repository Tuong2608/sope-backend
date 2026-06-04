package com.ecommerce.ecommercebackend.service;

import com.ecommerce.ecommercebackend.dto.request.CreateOrderRequest;
import com.ecommerce.ecommercebackend.dto.response.OrderItemResponse;
import com.ecommerce.ecommercebackend.dto.response.OrderResponse;
import com.ecommerce.ecommercebackend.entity.*;
import com.ecommerce.ecommercebackend.exception.BadRequestException;
import com.ecommerce.ecommercebackend.exception.ResourceNotFoundException;
import com.ecommerce.ecommercebackend.repository.CartRepository;
import com.ecommerce.ecommercebackend.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Business logic for placing and viewing orders.
 *
 * <p>An order is built from the authenticated user's cart: each line's product
 * name and current price are snapshotted, the cart is emptied, and a unique
 * {@code orderCode} is generated for the payment module to reference.</p>
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final DateTimeFormatter CODE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final OrderRepository orderRepository;
    private final CartService cartService;
    private final CartRepository cartRepository;

    // ── Create ──────────────────────────────────────────────────────────────────

    /**
     * Places an order from the user's current cart and empties the cart.
     *
     * @throws BadRequestException if the cart is empty or a product has no price
     */
    @Transactional
    public OrderResponse createOrder(User user, CreateOrderRequest request) {
        Cart cart = cartService.getOrCreateCart(user);
        if (cart.getItems().isEmpty()) {
            throw new BadRequestException("Giỏ hàng đang trống, không thể đặt hàng.");
        }

        Order order = Order.builder()
                .orderCode(generateUniqueOrderCode())
                .user(user)
                .status(OrderStatus.PENDING)
                .paymentMethod(request.getPaymentMethod())
                .recipientName(request.getRecipientName())
                .phone(request.getPhone())
                .shippingAddress(request.getShippingAddress())
                .note(request.getNote())
                .totalAmount(0L)
                .build();

        long total = 0L;
        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();
            Long unitPrice = product.getPrice();
            if (unitPrice == null) {
                throw new BadRequestException(
                        "Sản phẩm '" + product.getName() + "' chưa có giá, không thể đặt hàng.");
            }
            long lineTotal = unitPrice * cartItem.getQuantity();
            total += lineTotal;

            order.addItem(OrderItem.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .unitPrice(unitPrice)
                    .quantity(cartItem.getQuantity())
                    .lineTotal(lineTotal)
                    .build());
        }
        order.setTotalAmount(total);

        Order saved = orderRepository.save(order);

        // Empty the cart now that its contents have become an order.
        cart.getItems().clear();
        cartRepository.save(cart);

        return toResponse(saved);
    }

    // ── Read ────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(User user) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(User user, Long id) {
        Order order = orderRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found with id: " + id));
        return toResponse(order);
    }

    // ── Mutations ─────────────────────────────────────────────────────────────────

    /**
     * Cancels an order the user owns — only allowed while still {@code PENDING}.
     */
    @Transactional
    public OrderResponse cancelOrder(User user, Long id) {
        Order order = orderRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found with id: " + id));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BadRequestException(
                    "Chỉ có thể huỷ đơn hàng đang ở trạng thái PENDING.");
        }
        order.setStatus(OrderStatus.CANCELLED);
        return toResponse(orderRepository.save(order));
    }

    /**
     * Integration hook: marks an order as paid by its business key.
     *
     * <p>Intended to be called from the payment module's IPN handler once a
     * VNPAY/MoMo transaction is confirmed. Kept here so the payment code can
     * stay decoupled (it currently references the order only by String code).</p>
     */
    @Transactional
    public void markAsPaid(String orderCode) {
        Order order = orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found with code: " + orderCode));
        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    /** Generates "ORD-yyyyMMdd-XXXXXX", retrying on the rare collision. */
    private String generateUniqueOrderCode() {
        String code;
        do {
            StringBuilder suffix = new StringBuilder(6);
            for (int i = 0; i < 6; i++) {
                suffix.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
            }
            code = "ORD-" + LocalDate.now().format(CODE_DATE) + "-" + suffix;
        } while (orderRepository.existsByOrderCode(code));
        return code;
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(item -> OrderItemResponse.builder()
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .unitPrice(item.getUnitPrice())
                        .quantity(item.getQuantity())
                        .lineTotal(item.getLineTotal())
                        .build())
                .toList();

        return OrderResponse.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .status(order.getStatus())
                .paymentMethod(order.getPaymentMethod())
                .totalAmount(order.getTotalAmount())
                .recipientName(order.getRecipientName())
                .phone(order.getPhone())
                .shippingAddress(order.getShippingAddress())
                .note(order.getNote())
                .items(items)
                .createdAt(order.getCreatedAt())
                .build();
    }
}
