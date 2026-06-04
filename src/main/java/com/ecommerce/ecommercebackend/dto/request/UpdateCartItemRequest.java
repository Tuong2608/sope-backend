package com.ecommerce.ecommercebackend.dto.request;

import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * Request payload for updating the quantity of an existing cart item.
 */
@Data
public class UpdateCartItemRequest {

    @Min(value = 1, message = "quantity must be at least 1")
    private int quantity;
}
