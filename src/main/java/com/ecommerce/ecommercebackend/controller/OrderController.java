package com.ecommerce.ecommercebackend.controller;

import com.ecommerce.ecommercebackend.dto.request.CreateOrderRequest;
import com.ecommerce.ecommercebackend.dto.response.OrderResponse;
import com.ecommerce.ecommercebackend.entity.User;
import com.ecommerce.ecommercebackend.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for the authenticated user's orders.
 *
 * <p>An order is created from the user's cart; the response carries
 * {@code orderCode} + {@code totalAmount} which the frontend then forwards to
 * {@code POST /api/payment/create} for VNPAY/MoMo. All routes require auth.</p>
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /** Places an order from the current user's cart. */
    @PostMapping
    public ResponseEntity<OrderResponse> create(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateOrderRequest request) {
        OrderResponse created = orderService.createOrder(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /** Returns the current user's order history (newest first). */
    @GetMapping
    public ResponseEntity<List<OrderResponse>> myOrders(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(orderService.getMyOrders(user));
    }

    /** Returns a single order owned by the current user. */
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOne(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrder(user, id));
    }

    /** Cancels a still-pending order owned by the current user. */
    @PutMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancel(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        return ResponseEntity.ok(orderService.cancelOrder(user, id));
    }
}
