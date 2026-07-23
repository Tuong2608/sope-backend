package com.ecommerce.ecommercebackend.service;

import com.ecommerce.ecommercebackend.dto.request.CreateOrderRequest;
import com.ecommerce.ecommercebackend.dto.request.DeliveryEstimateItemRequest;
import com.ecommerce.ecommercebackend.dto.request.DeliveryEstimateRequest;
import com.ecommerce.ecommercebackend.dto.response.DeliveryEstimateResponse;
import com.ecommerce.ecommercebackend.dto.response.OrderItemResponse;
import com.ecommerce.ecommercebackend.dto.response.OrderResponse;
import com.ecommerce.ecommercebackend.entity.*;
import com.ecommerce.ecommercebackend.exception.BadRequestException;
import com.ecommerce.ecommercebackend.exception.ResourceNotFoundException;
import com.ecommerce.ecommercebackend.repository.CartRepository;
import com.ecommerce.ecommercebackend.repository.CouponRepository;
import com.ecommerce.ecommercebackend.repository.CouponUsageRepository;
import com.ecommerce.ecommercebackend.repository.OrderRepository;
import com.ecommerce.ecommercebackend.service.event.OrderPlacedEvent;
import com.ecommerce.ecommercebackend.service.event.OrderStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Business logic for placing and viewing orders.
 *
 * <p>An order is built from the authenticated user's cart: each line's product
 * name and current price are snapshotted, shipping fee/ETA and coupon discount
 * are frozen at checkout time (task C06/D04), and a unique {@code orderCode} is
 * generated for the payment module to reference.</p>
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final DateTimeFormatter CODE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Task C07 — the only statuses an order may move to from each status.
     * A status absent from the map, or mapped to an empty set, is terminal.
     */
    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS =
            new EnumMap<>(OrderStatus.class);
    static {
        ALLOWED_TRANSITIONS.put(OrderStatus.PENDING,
                EnumSet.of(OrderStatus.PAID, OrderStatus.PROCESSING, OrderStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(OrderStatus.PAID,
                EnumSet.of(OrderStatus.PROCESSING, OrderStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(OrderStatus.PROCESSING,
                EnumSet.of(OrderStatus.SHIPPING, OrderStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(OrderStatus.SHIPPING,
                EnumSet.of(OrderStatus.COMPLETED));
        ALLOWED_TRANSITIONS.put(OrderStatus.COMPLETED, EnumSet.noneOf(OrderStatus.class));
        ALLOWED_TRANSITIONS.put(OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class));
    }

    private final OrderRepository orderRepository;
    private final CartService cartService;
    private final CartRepository cartRepository;
    private final InventoryService inventoryService; // B08/B09
    private final OrderPricingService orderPricingService; // D04
    private final DeliveryEstimateService deliveryEstimateService; // C04/C06
    private final CouponRepository couponRepository; // D04/C07
    private final CouponUsageRepository couponUsageRepository; // hold/use/release history
    private final ApplicationEventPublisher eventPublisher;

    // ── Create ──────────────────────────────────────────────────────────────────

    /**
     * Places an order from the user's current cart and empties the cart.
     *
     * <p>Prices, coupon discount (D04) and shipping fee/ETA (C06) are all
     * computed here from live data and frozen onto the order — nothing is
     * trusted from the client except the province/method/coupon code chosen.</p>
     *
     * @throws BadRequestException if the cart is empty, a product has no price,
     *                             the coupon is invalid, or the province is unsupported
     */
    @Transactional
    public OrderResponse createOrder(User user, CreateOrderRequest request) {
        Cart cart = cartService.getOrCreateCart(user);
        if (cart.getItems().isEmpty()) {
            throw new BadRequestException("Giỏ hàng đang trống, không thể đặt hàng.");
        }

        OrderPricingResult pricing = orderPricingService.price(cart, request.getCouponCode(), user);
        DeliveryEstimateResponse delivery = estimateDelivery(request, cart);

        Order order = Order.builder()
                .orderCode(generateUniqueOrderCode())
                .user(user)
                .status(OrderStatus.PENDING)
                .paymentMethod(request.getPaymentMethod())
                .recipientName(request.getRecipientName())
                .phone(request.getPhone())
                .shippingAddress(request.getShippingAddress())
                .shippingMethodCode(delivery.getMethodCode())
                .shippingFee(delivery.getFee())
                .estimatedDeliveryMinDate(delivery.getEstimatedMinDate())
                .estimatedDeliveryMaxDate(delivery.getEstimatedMaxDate())
                .note(request.getNote())
                .subtotalAmount(pricing.getSubtotalAmount())
                .discountAmount(pricing.getDiscountAmount())
                .couponCode(pricing.getAppliedCoupon() != null ? pricing.getAppliedCoupon().getCode() : null)
                .totalAmount(pricing.getSubtotalAmount() - pricing.getDiscountAmount() + delivery.getFee())
                .build();

        for (OrderPricingResult.Item priced : pricing.getItems()) {
            CartItem cartItem = priced.getCartItem();
            Product product = cartItem.getProduct();
            order.addItem(OrderItem.builder()
                    .productId(product.getId())
                    .variantId(cartItem.getVariant() != null ? cartItem.getVariant().getId() : null)
                    .productName(product.getName())
                    .unitPrice(priced.getUnitPrice())
                    .quantity(cartItem.getQuantity())
                    .lineTotal(priced.getLineTotal())
                    .discountAmount(priced.getDiscountAmount())
                    .build());
        }

        Order saved = orderRepository.save(order);

        if (pricing.getAppliedCoupon() != null) {
            couponUsageRepository.save(CouponUsage.builder()
                    .coupon(pricing.getAppliedCoupon())
                    .user(user)
                    .order(saved)
                    .status(CouponUsageStatus.HELD)
                    .discountAmount(pricing.getDiscountAmount())
                    .build());
        }

        // Empty the cart now that its contents have become an order.
        cart.getItems().clear();
        cartRepository.save(cart);

        eventPublisher.publishEvent(new OrderPlacedEvent(
                user.getId(),
                saved.getId(),
                saved.getOrderCode()));

        return toResponse(saved);
    }

    private DeliveryEstimateResponse estimateDelivery(CreateOrderRequest request, Cart cart) {
        DeliveryEstimateRequest deliveryRequest = new DeliveryEstimateRequest();
        deliveryRequest.setProvince(request.getProvince());
        deliveryRequest.setMethodCode(request.getShippingMethodCode());
        deliveryRequest.setItems(cart.getItems().stream().map(cartItem -> {
            DeliveryEstimateItemRequest item = new DeliveryEstimateItemRequest();
            item.setProductId(cartItem.getProduct().getId());
            item.setQuantity(cartItem.getQuantity());
            return item;
        }).toList());
        return deliveryEstimateService.estimate(deliveryRequest);
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
     * Stock is never deducted for a PENDING order, so there is nothing to restore
     * here (only {@link #updateStatus} restores stock, for orders cancelled after
     * having reached PAID/PROCESSING).
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
        Order saved = orderRepository.save(order);
        releaseCouponHold(saved);
        publishStatusChanged(saved);
        return toResponse(saved);
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
        Order order = orderRepository.findLockedByOrderCode(orderCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found with code: " + orderCode));

        if (order.getStatus() == OrderStatus.PAID
                || order.getStatus() == OrderStatus.PROCESSING
                || order.getStatus() == OrderStatus.SHIPPING
                || order.getStatus() == OrderStatus.COMPLETED) {
            return;
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BadRequestException(
                    "Không thể xác nhận thanh toán cho đơn hàng ở trạng thái " + order.getStatus() + ".");
        }
        order.setStatus(OrderStatus.PAID);
        Order saved = orderRepository.save(order);
        // B08: Giảm tồn kho khi đơn hàng được thanh toán thành công
        inventoryService.deductStockForOrder(saved);
        markCouponUsed(saved);
        publishStatusChanged(saved);
    }

    // ── Admin operations ──────────────────────────────────────────────────────────

    /** All orders (optionally filtered by status), newest first. */
    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders(OrderStatus status) {
        List<Order> orders = (status == null)
                ? orderRepository.findAllByOrderByCreatedAtDesc()
                : orderRepository.findByStatusOrderByCreatedAtDesc(status);
        return orders.stream().map(this::toResponse).toList();
    }

    /** Any order by id, regardless of owner (admin). */
    @Transactional(readOnly = true)
    public OrderResponse getAnyOrder(Long id) {
        return toResponse(findAnyOrThrow(id));
    }

    /**
     * Admin transitions an order's status (task C07) — only along the allowed
     * path (see {@link #ALLOWED_TRANSITIONS}). Deducts/restores stock and
     * settles the coupon hold exactly once, at the transition that actually
     * causes it (PAID deducts stock/uses the coupon; CANCELLED from a
     * stock-deducted status restores it).
     *
     * @throws BadRequestException if the transition is not allowed from the
     *                             order's current status
     */
    @Transactional
    public OrderResponse updateStatus(Long id, OrderStatus newStatus) {
        Order order = findAnyOrThrow(id);
        OrderStatus oldStatus = order.getStatus();
        assertValidTransition(order, newStatus);

        order.setStatus(newStatus);
        Order saved = orderRepository.save(order);

        boolean activatesStock = newStatus == OrderStatus.PAID
                || (oldStatus == OrderStatus.PENDING
                    && newStatus == OrderStatus.PROCESSING);
        if (activatesStock) {
            inventoryService.deductStockForOrder(saved);
            markCouponUsed(saved);
        } else if (newStatus == OrderStatus.CANCELLED) {
            if (oldStatus == OrderStatus.PAID || oldStatus == OrderStatus.PROCESSING) {
                inventoryService.restoreStockForOrder(saved);
            }
            releaseCouponHold(saved);
        }

        publishStatusChanged(saved);
        return toResponse(saved);
    }

    private void assertValidTransition(Order order, OrderStatus to) {
        OrderStatus from = order.getStatus();
        Set<OrderStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(from, Set.of());
        if (!allowed.contains(to)) {
            throw new BadRequestException(
                    "Không thể chuyển trạng thái đơn hàng từ " + from + " sang " + to + ".");
        }

        if (from == OrderStatus.PENDING) {
            boolean isCod = order.getPaymentMethod() == PaymentMethod.COD;
            if (isCod && to == OrderStatus.PAID) {
                throw new BadRequestException(
                        "Đơn COD cần được duyệt sang PROCESSING, không xác nhận PAID trước khi giao.");
            }
            if (!isCod && to == OrderStatus.PROCESSING) {
                throw new BadRequestException(
                        "Đơn thanh toán online phải được xác nhận PAID trước khi duyệt.");
            }
        }
    }

    private void publishStatusChanged(Order order) {
        eventPublisher.publishEvent(new OrderStatusChangedEvent(
                order.getUser().getId(),
                order.getId(),
                order.getOrderCode(),
                order.getStatus()));
    }

    private Order findAnyOrThrow(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found with id: " + id));
    }

    // ── Coupon hold settlement (foundation for D06) ─────────────────────────────

    private void markCouponUsed(Order order) {
        couponUsageRepository.findByOrderId(order.getId()).ifPresent(usage -> {
            if (usage.getStatus() != CouponUsageStatus.HELD) {
                return;
            }
            usage.setStatus(CouponUsageStatus.USED);
            couponUsageRepository.save(usage);

            Coupon coupon = usage.getCoupon();
            coupon.setUsedCount(coupon.getUsedCount() + 1);
            couponRepository.save(coupon);
        });
    }

    private void releaseCouponHold(Order order) {
        couponUsageRepository.findByOrderId(order.getId()).ifPresent(usage -> {
            if (usage.getStatus() == CouponUsageStatus.HELD) {
                usage.setStatus(CouponUsageStatus.RELEASED);
                couponUsageRepository.save(usage);
            }
        });
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
                        .discountAmount(item.getDiscountAmount())
                        .build())
                .toList();

        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUser().getId())
                .orderCode(order.getOrderCode())
                .status(order.getStatus())
                .paymentMethod(order.getPaymentMethod())
                .totalAmount(order.getTotalAmount())
                .subtotalAmount(order.getSubtotalAmount())
                .discountAmount(order.getDiscountAmount())
                .couponCode(order.getCouponCode())
                .recipientName(order.getRecipientName())
                .phone(order.getPhone())
                .shippingAddress(order.getShippingAddress())
                .shippingMethodCode(order.getShippingMethodCode())
                .shippingFee(order.getShippingFee())
                .estimatedDeliveryMinDate(order.getEstimatedDeliveryMinDate())
                .estimatedDeliveryMaxDate(order.getEstimatedDeliveryMaxDate())
                .note(order.getNote())
                .items(items)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
