package com.ecommerce.ecommercebackend.dto.request;

import com.ecommerce.ecommercebackend.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request payload for an admin changing an order's status.
 */
@Data
public class UpdateOrderStatusRequest {

    @NotNull(message = "status is required")
    private OrderStatus status;
}
