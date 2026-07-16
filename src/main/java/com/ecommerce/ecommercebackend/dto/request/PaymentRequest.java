package com.ecommerce.ecommercebackend.dto.request;

import com.ecommerce.ecommercebackend.entity.PaymentProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request body để tạo một giao dịch thanh toán mới.
 */
@Data
public class PaymentRequest {

    /** Mã đơn hàng cần thanh toán. */
    @NotBlank(message = "orderId không được để trống")
    private String orderId;

    /** Cổng thanh toán: VNPAY hoặc MOMO. */
    @NotNull(message = "provider không được để trống")
    private PaymentProvider provider;

    /** Kênh VNPAY: để trống, VNPAYQR, VNBANK hoặc INTCARD. */
    private String channel;
}
