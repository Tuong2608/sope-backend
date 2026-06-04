package com.ecommerce.ecommercebackend.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * A single line in the cart, enriched with live product details.
 */
@Data
@Builder
public class CartItemResponse {

    private Long id;
    private Long productId;
    private String name;
    private String imgUrl;
    /** Current unit price in VND (live from the product). */
    private Long price;
    private int quantity;
    /** {@code price * quantity}; {@code null} when the product has no price yet. */
    private Long lineTotal;
}
