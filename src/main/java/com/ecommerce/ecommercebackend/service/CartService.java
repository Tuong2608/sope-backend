package com.ecommerce.ecommercebackend.service;

import com.ecommerce.ecommercebackend.dto.request.AddToCartRequest;
import com.ecommerce.ecommercebackend.dto.request.UpdateCartItemRequest;
import com.ecommerce.ecommercebackend.dto.response.CartItemResponse;
import com.ecommerce.ecommercebackend.dto.response.CartResponse;
import com.ecommerce.ecommercebackend.entity.Cart;
import com.ecommerce.ecommercebackend.entity.CartItem;
import com.ecommerce.ecommercebackend.entity.Product;
import com.ecommerce.ecommercebackend.entity.ProductVariant;
import com.ecommerce.ecommercebackend.entity.User;
import com.ecommerce.ecommercebackend.exception.BadRequestException;
import com.ecommerce.ecommercebackend.exception.ResourceNotFoundException;
import com.ecommerce.ecommercebackend.repository.CartRepository;
import com.ecommerce.ecommercebackend.repository.ProductRepository;
import com.ecommerce.ecommercebackend.repository.ProductVariantRepository;
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

    private final CartRepository            cartRepository;
    private final ProductRepository         productRepository;
    private final ProductVariantRepository  variantRepository;

    // ── Read ────────────────────────────────────────────────────────────────────

    @Transactional
    public CartResponse getCart(User user) {
        return toResponse(getOrCreateCart(user));
    }

    // ── Mutations ─────────────────────────────────────────────────────────────────

    /**
     * Adds a product (with optional variant) to the cart.
     * B05: Phân biệt cùng SP nhưng khác variant (màu/dung lượng).
     * Không cho chọn quá số lượng tồn kho.
     */
    @Transactional
    public CartResponse addItem(User user, AddToCartRequest request) {
        Cart cart = getOrCreateCart(user);
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with id: " + request.getProductId()));

        // B05: Tìm variant nếu có variantId
        ProductVariant variant = null;
        if (request.getVariantId() != null) {
            variant = variantRepository.findById(request.getVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Variant not found: " + request.getVariantId()));
        }

        // B05: Kiểm tra tồn kho trước khi thêm
        int available = (variant != null)
                ? variant.getAvailableQuantity()
                : product.getAvailableQuantity();
        if (request.getQuantity() > available) {
            throw new BadRequestException(
                    "Chỉ còn " + available + " sản phẩm trong kho");
        }

        // B05: Tìm item trùng (cùng product VÀ cùng variant)
        final Long variantId = variant != null ? variant.getId() : null;
        CartItem existing = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(product.getId())
                        && java.util.Objects.equals(
                                item.getVariant() != null ? item.getVariant().getId() : null,
                                variantId))
                .findFirst()
                .orElse(null);

        if (existing != null) {
            int newQty = existing.getQuantity() + request.getQuantity();
            if (newQty > available) {
                throw new BadRequestException("Không thể thêm: vượt quá số lượng còn trong kho ("+available+")");
            }
            existing.setQuantity(newQty);
        } else {
            ProductVariant finalVariant = variant;
            cart.addItem(CartItem.builder()
                    .product(product)
                    .variant(finalVariant)
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
        ProductVariant variant = item.getVariant();

        // B05: Dùng giá variant nếu có, fallback về giá product
        Long price = (variant != null && variant.getPrice() != null)
                ? variant.getPrice()
                : product.getPrice();
        Long lineTotal = (price == null) ? null : price * item.getQuantity();

        return CartItemResponse.builder()
                .id(item.getId())
                .productId(product.getId())
                .name(product.getName())
                .imgUrl(variant != null && variant.getImageUrl() != null
                        ? variant.getImageUrl() : product.getImgUrl())
                .price(price)
                .quantity(item.getQuantity())
                .lineTotal(lineTotal)
                .build();
    }
}
