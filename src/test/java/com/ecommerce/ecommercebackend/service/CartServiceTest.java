package com.ecommerce.ecommercebackend.service;

import com.ecommerce.ecommercebackend.dto.request.AddToCartRequest;
import com.ecommerce.ecommercebackend.dto.response.CartResponse;
import com.ecommerce.ecommercebackend.entity.Cart;
import com.ecommerce.ecommercebackend.entity.CartItem;
import com.ecommerce.ecommercebackend.entity.Product;
import com.ecommerce.ecommercebackend.entity.Role;
import com.ecommerce.ecommercebackend.entity.User;
import com.ecommerce.ecommercebackend.repository.CartRepository;
import com.ecommerce.ecommercebackend.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CartService cartService;

    @Test
    void addItemCreatesCartAndAddsProduct() {
        User user = user(1L);
        Product product = product(10L, "iPhone 15", 20_000_000L);
        AddToCartRequest request = addToCartRequest(10L, 2);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> {
            Cart cart = invocation.getArgument(0);
            if (cart.getId() == null) {
                cart.setId(100L);
            }
            return cart;
        });

        CartResponse response = cartService.addItem(user, request);

        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getProductId()).isEqualTo(10L);
        assertThat(response.getItems().get(0).getQuantity()).isEqualTo(2);
        assertThat(response.getTotalItems()).isEqualTo(2);
        assertThat(response.getTotalAmount()).isEqualTo(40_000_000L);
    }

    @Test
    void addItemMergesQuantityWhenProductAlreadyExists() {
        User user = user(1L);
        Product product = product(10L, "iPhone 15", 20_000_000L);
        Cart cart = Cart.builder().id(100L).user(user).build();
        cart.addItem(CartItem.builder()
                .id(5L)
                .product(product)
                .quantity(1)
                .build());
        AddToCartRequest request = addToCartRequest(10L, 3);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartResponse response = cartService.addItem(user, request);

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getQuantity()).isEqualTo(4);
        assertThat(response.getTotalItems()).isEqualTo(4);
        assertThat(response.getTotalAmount()).isEqualTo(80_000_000L);
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
                .stockQuantity(100)
                .imgUrl("https://example.com/product.png")
                .build();
    }

    private AddToCartRequest addToCartRequest(Long productId, int quantity) {
        AddToCartRequest request = new AddToCartRequest();
        request.setProductId(productId);
        request.setQuantity(quantity);
        return request;
    }
}
