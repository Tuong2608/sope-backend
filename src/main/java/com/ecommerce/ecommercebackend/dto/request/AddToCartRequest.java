package com.ecommerce.ecommercebackend.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request payload for adding a product to the cart.
 * B05: Có thể kèm variantId để phân biệt màu/dung lượng.
 */
@Data
public class AddToCartRequest {

    @NotNull(message = "productId is required")
    private Long productId;

    /** B05: ID phiên bản sản phẩm (màu + dung lượng). null = sản phẩm không có variant. */
    private Long variantId;

    @Min(value = 1, message = "quantity must be at least 1")
    private int quantity = 1;
}
