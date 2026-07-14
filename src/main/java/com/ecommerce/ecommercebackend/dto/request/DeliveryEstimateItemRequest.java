package com.ecommerce.ecommercebackend.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * One line item used to check stock availability when estimating delivery (task C04/C05).
 */
@Data
public class DeliveryEstimateItemRequest {

    @NotNull(message = "productId is required")
    private Long productId;

    @Min(value = 1, message = "quantity must be at least 1")
    private int quantity = 1;
}
