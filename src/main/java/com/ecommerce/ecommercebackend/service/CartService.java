package com.ecommerce.ecommercebackend.service;

import com.ecommerce.ecommercebackend.dto.request.AddToCartRequest;
import com.ecommerce.ecommercebackend.dto.request.UpdateCartItemRequest;
import com.ecommerce.ecommercebackend.dto.response.CartItemResponse;
import com.ecommerce.ecommercebackend.dto.response.CartResponse;
import com.ecommerce.ecommercebackend.entity.Cart;
import com.ecommerce.ecommercebackend.entity.CartItem;
import com.ecommerce.ecommercebackend.entity.Product;
import com.ecommerce.ecommercebackend.entity.User;
import com.ecommerce.ecommercebackend.exception.ResourceNotFoundException;
import com.ecommerce.ecommercebackend.repository.CartRepository;
import com.ecommerce.ecommercebackend.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Business logic for the shopping cart: one cart per user, created lazily.
 *
 * <p>Cart contents always reflect the product's <em>current</em> price; prices
 * are only frozen when an order is placed (see {@code OrderService}).</p>
 */
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;

    // ── Read ────────────────────────────────────────────────────────────────────

    @Transactional
    public CartResponse getCart(User user) {
        return toResponse(getOrCreateCart(user));
    }

    // ── Mutations ─────────────────────────────────────────────────────────────────

    /**
     * Adds a product to the cart. If it is already present, the quantities are
     * merged rather than duplicating the line.
     */
    @Transactional
    public CartResponse addItem(User user, AddToCartRequest request) {
        Cart cart = getOrCreateCart(user);
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with id: " + request.getProductId()));

        CartItem existing = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(product.getId()))
                .findFirst()
                .orElse(null);

        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + request.getQuantity());
        } else {
            cart.addItem(CartItem.builder()
                    .product(product)
                    .quantity(request.getQuantity())
                    .build());
        }

        return toResponse(cartRepository.save(cart));
    }

    /**
     * Sets the absolute quantity of an existing cart line.
     *
     * @throws ResourceNotFoundException if the item does not belong to the user's cart
     */
    @Transactional
    public CartResponse updateItem(User user, Long itemId, UpdateCartItemRequest request) {
        Cart cart = getOrCreateCart(user);
        CartItem item = findItemOrThrow(cart, itemId);
        item.setQuantity(request.getQuantity());
        return toResponse(cartRepository.save(cart));
    }

    @Transactional
    public CartResponse removeItem(User user, Long itemId) {
        Cart cart = getOrCreateCart(user);
        CartItem item = findItemOrThrow(cart, itemId);
        cart.removeItem(item);
        return toResponse(cartRepository.save(cart));
    }

    @Transactional
    public CartResponse clearCart(User user) {
        Cart cart = getOrCreateCart(user);
        cart.getItems().clear();
        return toResponse(cartRepository.save(cart));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    /** Returns the user's cart, creating an empty one on first access. */
    @Transactional
    public Cart getOrCreateCart(User user) {
        return cartRepository.findByUserId(user.getId())
                .orElseGet(() -> cartRepository.save(
                        Cart.builder().user(user).build()));
    }

    private CartItem findItemOrThrow(Cart cart, Long itemId) {
        return cart.getItems().stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cart item not found with id: " + itemId));
    }

    private CartResponse toResponse(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream()
                .map(this::toItemResponse)
                .toList();

        int totalItems = items.stream().mapToInt(CartItemResponse::getQuantity).sum();
        long totalAmount = items.stream()
                .map(CartItemResponse::getLineTotal)
                .filter(java.util.Objects::nonNull)
                .mapToLong(Long::longValue)
                .sum();

        return CartResponse.builder()
                .id(cart.getId())
                .items(items)
                .totalItems(totalItems)
                .totalAmount(totalAmount)
                .build();
    }

    private CartItemResponse toItemResponse(CartItem item) {
        Product product = item.getProduct();
        Long price = product.getPrice();
        Long lineTotal = (price == null) ? null : price * item.getQuantity();

        return CartItemResponse.builder()
                .id(item.getId())
                .productId(product.getId())
                .name(product.getName())
                .imgUrl(product.getImgUrl())
                .price(price)
                .quantity(item.getQuantity())
                .lineTotal(lineTotal)
                .build();
    }
}
