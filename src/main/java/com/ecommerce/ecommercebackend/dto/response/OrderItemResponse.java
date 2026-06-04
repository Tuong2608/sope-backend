package com.ecommerce.ecommercebackend.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * A snapshot line within an {@link OrderResponse}.
 */
@Data
@Builder
public class OrderItemResponse {

    private Long productId;
    private String productName;
    private Long unitPrice;
    private int quantity;
    private Long lineTotal;
}
