package com.ecommerce.ecommercebackend.dto.response;

import com.ecommerce.ecommercebackend.entity.PaymentProvider;
import com.ecommerce.ecommercebackend.entity.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Response trả về sau khi tạo hoặc truy vấn một giao dịch thanh toán.
 */
@Data
@Builder
public class PaymentResponse {

    private Long id;
    private String orderId;
    private Long amount;
    private PaymentProvider provider;
    private PaymentStatus status;
    private String transactionId;
    private String orderInfo;

    /** URL trang thanh toán — chuyển hướng người dùng tới đây. */
    private String paymentUrl;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
