package com.ecommerce.ecommercebackend.dto.response;

import com.ecommerce.ecommercebackend.entity.OrderStatus;
import com.ecommerce.ecommercebackend.entity.PaymentMethod;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Full representation of an order returned to the client.
 *
 * <p>{@code orderCode} + {@code totalAmount} are what the frontend forwards to
 * {@code POST /api/payment/create} when {@code paymentMethod} is VNPAY/MOMO.</p>
 */
@Data
@Builder
public class OrderResponse {

    private Long id;
    private String orderCode;
    private OrderStatus status;
    private PaymentMethod paymentMethod;
    private Long totalAmount;

    private String recipientName;
    private String phone;
    private String shippingAddress;
    private String note;

    private List<OrderItemResponse> items;
    private LocalDateTime createdAt;
}
