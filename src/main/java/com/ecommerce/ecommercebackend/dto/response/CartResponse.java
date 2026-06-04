package com.ecommerce.ecommercebackend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * The user's cart with its lines and roll-up totals.
 */
@Data
@Builder
public class CartResponse {

    private Long id;
    private List<CartItemResponse> items;
    /** Total quantity across all lines. */
    private int totalItems;
    /** Sum of every line total in VND. */
    private Long totalAmount;
}
