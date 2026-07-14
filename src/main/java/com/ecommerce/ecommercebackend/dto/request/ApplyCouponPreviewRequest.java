package com.ecommerce.ecommercebackend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request payload for previewing a coupon (task D05).
 *
 * <p>Only the code is sent — the backend re-reads the caller's current cart,
 * live prices and stock levels itself; it never trusts totals from the client.</p>
 */
@Data
public class ApplyCouponPreviewRequest {

    @NotBlank(message = "couponCode is required")
    private String couponCode;
}
