package com.ecommerce.ecommercebackend.dto.request;

import com.ecommerce.ecommercebackend.entity.PaymentProvider;
import jakarta.validation.constraints.Min;
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

    /** Số tiền thanh toán (đơn vị: VND, tối thiểu 1000đ). */
    @NotNull(message = "amount không được để trống")
    @Min(value = 1000, message = "Số tiền thanh toán tối thiểu là 1.000 VND")
    private Long amount;

    /** Cổng thanh toán: VNPAY hoặc MOMO. */
    @NotNull(message = "provider không được để trống")
    private PaymentProvider provider;

    /** Nội dung thanh toán hiển thị trên cổng thanh toán. */
    @NotBlank(message = "orderInfo không được để trống")
    private String orderInfo;
}
