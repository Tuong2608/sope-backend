package com.ecommerce.ecommercebackend.controller;

import com.ecommerce.ecommercebackend.dto.request.AddToCartRequest;
import com.ecommerce.ecommercebackend.dto.request.UpdateCartItemRequest;
import com.ecommerce.ecommercebackend.dto.response.CartResponse;
import com.ecommerce.ecommercebackend.entity.User;
import com.ecommerce.ecommercebackend.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for the authenticated user's shopping cart.
 *
 * <p>Every endpoint operates on the cart of the currently logged-in user,
 * resolved from the JWT via {@link AuthenticationPrincipal}. All routes require
 * authentication (enforced by {@code SecurityConfig}'s {@code anyRequest()}).</p>
 */
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    /** Returns the current user's cart (creating an empty one if needed). */
    @GetMapping
    public ResponseEntity<CartResponse> getCart(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(cartService.getCart(user));
    }

    /** Adds a product to the cart (quantities merge if already present). */
    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItem(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody AddToCartRequest request) {
        return ResponseEntity.ok(cartService.addItem(user, request));
    }

    /** Updates the quantity of an existing cart line. */
    @PutMapping("/items/{itemId}")
    public ResponseEntity<CartResponse> updateItem(
            @AuthenticationPrincipal User user,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        return ResponseEntity.ok(cartService.updateItem(user, itemId, request));
    }

    /** Removes a single line from the cart. */
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<CartResponse> removeItem(
            @AuthenticationPrincipal User user,
            @PathVariable Long itemId) {
        return ResponseEntity.ok(cartService.removeItem(user, itemId));
    }

    /** Empties the cart entirely. */
    @DeleteMapping
    public ResponseEntity<CartResponse> clearCart(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(cartService.clearCart(user));
    }
}
