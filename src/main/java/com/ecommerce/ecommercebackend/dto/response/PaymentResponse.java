package com.ecommerce.ecommercebackend.dto.response;

import com.ecommerce.ecommercebackend.entity.PaymentProvider;
import com.ecommerce.ecommercebackend.entity.PaymentStatus;
import com.ecommerce.ecommercebackend.entity.OrderStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Response trả về sau khi tạo hoặc truy vấn một giao dịch thanh toán.
 */
@Data
@Builder
public class PaymentResponse {

    /** Giữ cả id và paymentId để tương thích client cũ trong giai đoạn chuyển đổi. */
    private Long id;
    private Long paymentId;
    private Long orderId;
    private String orderCode;
    private Long amount;
    private String currency;
    private PaymentProvider provider;
    private PaymentStatus status;
    private OrderStatus orderStatus;
    private String transactionId;
    private String providerTransactionId;
    private String providerOrderId;
    private String providerRequestId;
    private String orderInfo;
    private String paymentChannel;
    private String responseCode;
    private String transactionStatus;
    private String responseMessage;
    private String bankCode;
    private String cardType;
    private String payDate;
    private LocalDateTime paidAt;
    private LocalDateTime expiresAt;
    private boolean signatureVerified;
    private boolean canRetry;

    /** URL trang thanh toán — chuyển hướng người dùng tới đây. */
    private String paymentUrl;
    private String payUrl;
    private String deeplink;
    private String qrCodeUrl;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
