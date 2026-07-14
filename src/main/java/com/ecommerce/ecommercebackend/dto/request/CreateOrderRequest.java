package com.ecommerce.ecommercebackend.dto.request;

import com.ecommerce.ecommercebackend.entity.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request payload for placing an order.
 *
 * <p>Line items are <strong>not</strong> supplied here — they are taken from
 * the authenticated user's current cart.</p>
 */
@Data
public class CreateOrderRequest {

    @NotBlank(message = "recipientName is required")
    private String recipientName;

    @NotBlank(message = "phone is required")
    private String phone;

    @NotBlank(message = "shippingAddress is required")
    private String shippingAddress;

    /** Province/city name — used to compute shipping fee and ETA (task C06). */
    @NotBlank(message = "province is required")
    private String province;

    /** Shipping method code, e.g. "STANDARD" or "EXPRESS"; defaults to STANDARD. */
    private String shippingMethodCode = "STANDARD";

    /** Optional coupon code to apply at checkout (task D04). */
    private String couponCode;

    private String note;

    @NotNull(message = "paymentMethod is required (COD, VNPAY or MOMO)")
    private PaymentMethod paymentMethod;
}
