package com.ecommerce.ecommercebackend.service;

import com.ecommerce.ecommercebackend.dto.request.PaymentRequest;
import com.ecommerce.ecommercebackend.dto.response.PaymentResponse;
import com.ecommerce.ecommercebackend.entity.Order;
import com.ecommerce.ecommercebackend.entity.OrderStatus;
import com.ecommerce.ecommercebackend.entity.Payment;
import com.ecommerce.ecommercebackend.entity.PaymentMethod;
import com.ecommerce.ecommercebackend.entity.PaymentProvider;
import com.ecommerce.ecommercebackend.entity.PaymentStatus;
import com.ecommerce.ecommercebackend.entity.User;
import com.ecommerce.ecommercebackend.exception.BadRequestException;
import com.ecommerce.ecommercebackend.exception.ResourceNotFoundException;
import com.ecommerce.ecommercebackend.repository.OrderRepository;
import com.ecommerce.ecommercebackend.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Objects;

/**
 * Service điều phối toàn bộ luồng thanh toán.
 *
 * <p>Nhận request từ controller, gọi đúng service (VNPAY hoặc MoMo),
 * lưu trạng thái vào DB và trả về kết quả.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository   orderRepository;
    private final VnpayService      vnpayService;
    private final MomoService       momoService;

    // ── Tạo giao dịch ────────────────────────────────────────────────────────

    /**
     * Tạo link thanh toán cho đơn hàng, lưu giao dịch vào DB với
     * trạng thái {@link PaymentStatus#PENDING} và trả về URL thanh toán.
     *
     * @param request   Thông tin thanh toán từ client
     * @param ipAddress IP của người dùng (dùng cho VNPAY)
     * @return PaymentResponse chứa {@code paymentUrl}
     */
    @Transactional
    public PaymentResponse createPayment(User user, PaymentRequest request, String ipAddress) {
        Order order = orderRepository.findByOrderCodeAndUserId(request.getOrderId(), user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found with code: " + request.getOrderId()));

        validatePaymentRequest(order, request);

        var existingPending = paymentRepository.findFirstByOrderIdAndStatusOrderByCreatedAtDesc(
                order.getOrderCode(),
                PaymentStatus.PENDING
        );
        if (existingPending.isPresent()) {
            return toResponse(existingPending.get());
        }

        String paymentUrl;
        Long amount = order.getTotalAmount();
        String orderInfo = buildOrderInfo(request, order);

        if (request.getProvider() == PaymentProvider.VNPAY) {
            paymentUrl = vnpayService.createPaymentUrl(
                    order.getOrderCode(), amount, orderInfo, ipAddress);
        } else {
            paymentUrl = momoService.createPaymentUrl(
                    order.getOrderCode(), amount, orderInfo);
        }

        Payment payment = Payment.builder()
                .orderId(order.getOrderCode())
                .amount(amount)
                .provider(request.getProvider())
                .orderInfo(orderInfo)
                .status(PaymentStatus.PENDING)
                .paymentUrl(paymentUrl)
                .build();

        payment = paymentRepository.save(payment);
        log.info("[PAYMENT] Tạo giao dịch #{} - Provider: {} - OrderId: {}",
                payment.getId(), payment.getProvider(), payment.getOrderId());
        return toResponse(payment);
    }

    // ── Xử lý IPN từ VNPAY ───────────────────────────────────────────────────

    /**
     * Xử lý IPN (Instant Payment Notification) từ VNPAY.
     *
     * <p>Các bước xác thực theo thứ tự:
     * <ol>
     *   <li>Xác thực chữ ký HMAC-SHA512</li>
     *   <li><b>Idempotency</b> — bỏ qua nếu giao dịch đã được xử lý (status != PENDING)</li>
     *   <li><b>Amount Validation</b> — số tiền trong IPN phải khớp với DB</li>
     *   <li>Cập nhật trạng thái</li>
     * </ol>
     * </p>
     *
     * @param params Tham số VNPAY gửi qua IPN
     */
    @Transactional
    public void handleVnpayIpn(Map<String, String> params) {
        // Bước 1: Xác thực chữ ký
        if (!vnpayService.verifySignature(params)) {
            log.warn("[VNPAY IPN] Chữ ký không hợp lệ — bỏ qua");
            return;
        }

        // vnp_TxnRef = orderId_timestamp
        String txnRef  = params.getOrDefault("vnp_TxnRef", "");
        String orderId = txnRef.contains("_") ? txnRef.substring(0, txnRef.lastIndexOf('_')) : txnRef;

        paymentRepository.findFirstByOrderIdAndStatusOrderByCreatedAtDesc(
                orderId,
                PaymentStatus.PENDING
        ).ifPresent(payment -> {

            // Bước 2: Idempotency — chỉ xử lý nếu đang PENDING
            // VNPAY có thể gọi IPN nhiều lần (retry). Nếu đã xử lý rồi thì bỏ qua.
            if (payment.getStatus() != PaymentStatus.PENDING) {
                log.info("[VNPAY IPN] Giao dịch #{} đã được xử lý (status={}), bỏ qua IPN trùng lặp",
                        payment.getId(), payment.getStatus());
                return;
            }

            // Bước 3: Amount Validation
            // VNPAY gửi vnp_Amount = số tiền * 100 (đơn vị 1/100 VND)
            String vnpAmountStr = params.getOrDefault("vnp_Amount", "0");
            long   vnpAmount;
            try {
                vnpAmount = Long.parseLong(vnpAmountStr) / 100; // chuyển về VND
            } catch (NumberFormatException e) {
                log.warn("[VNPAY IPN] vnp_Amount không hợp lệ: '{}'", vnpAmountStr);
                return;
            }

            if (vnpAmount != payment.getAmount()) {
                log.warn("[VNPAY IPN] Amount không khớp! DB={} VND, IPN={} VND (orderId={})",
                        payment.getAmount(), vnpAmount, orderId);
                // Đánh dấu FAILED để tránh xử lý tiếp
                payment.setStatus(PaymentStatus.FAILED);
                paymentRepository.save(payment);
                return;
            }

            // Bước 4: Cập nhật trạng thái
            PaymentStatus newStatus = vnpayService.resolveStatus(params);
            String        txnId     = vnpayService.extractTransactionId(params);
            payment.setStatus(newStatus);
            payment.setTransactionId(txnId);
            paymentRepository.save(payment);
            markOrderPaidIfSuccessful(payment, newStatus);
            log.info("[VNPAY IPN] Cập nhật Payment #{} → {} (txnId={})", payment.getId(), newStatus, txnId);
        });
    }

    // ── Xử lý IPN từ MoMo ────────────────────────────────────────────────────

    /**
     * Xử lý IPN (notify_url) từ MoMo.
     *
     * <p>Các bước xác thực theo thứ tự:
     * <ol>
     *   <li>Xác thực chữ ký HMAC-SHA256</li>
     *   <li><b>Idempotency</b> — bỏ qua nếu giao dịch đã được xử lý (status != PENDING)</li>
     *   <li>Cập nhật trạng thái</li>
     * </ol>
     * </p>
     *
     * @param params Tham số MoMo gửi qua IPN
     */
    @Transactional
    public void handleMomoIpn(Map<String, String> params) {
        // Bước 1: Xác thực chữ ký
        if (!momoService.verifySignature(params)) {
            log.warn("[MOMO IPN] Chữ ký không hợp lệ — bỏ qua");
            return;
        }

        String orderId = params.getOrDefault("orderId", "");

        paymentRepository.findFirstByOrderIdAndStatusOrderByCreatedAtDesc(
                orderId,
                PaymentStatus.PENDING
        ).ifPresent(payment -> {

            // Bước 2: Idempotency — chỉ xử lý nếu đang PENDING
            // MoMo có thể gọi IPN nhiều lần nếu backend không trả về đúng format.
            if (payment.getStatus() != PaymentStatus.PENDING) {
                log.info("[MOMO IPN] Giao dịch #{} đã được xử lý (status={}), bỏ qua IPN trùng lặp",
                        payment.getId(), payment.getStatus());
                return;
            }

            // Bước 3: Cập nhật trạng thái
            String momoAmountStr = params.getOrDefault("amount", "0");
            long momoAmount;
            try {
                momoAmount = Long.parseLong(momoAmountStr);
            } catch (NumberFormatException e) {
                log.warn("[MOMO IPN] amount khÃ´ng há»£p lá»‡: '{}'", momoAmountStr);
                return;
            }

            if (momoAmount != payment.getAmount()) {
                log.warn("[MOMO IPN] Amount khÃ´ng khá»›p! DB={} VND, IPN={} VND (orderId={})",
                        payment.getAmount(), momoAmount, orderId);
                payment.setStatus(PaymentStatus.FAILED);
                paymentRepository.save(payment);
                return;
            }

            PaymentStatus newStatus = momoService.resolveStatus(params);
            String        txnId     = momoService.extractTransactionId(params);
            payment.setStatus(newStatus);
            payment.setTransactionId(txnId);
            paymentRepository.save(payment);
            markOrderPaidIfSuccessful(payment, newStatus);
            log.info("[MOMO IPN] Cập nhật Payment #{} → {} (txnId={})", payment.getId(), newStatus, txnId);
        });
    }

    // ── Truy vấn trạng thái ───────────────────────────────────────────────────

    /**
     * Lấy thông tin giao dịch thanh toán theo ID.
     *
     * @param id ID của Payment
     * @return PaymentResponse
     * @throws ResourceNotFoundException nếu không tìm thấy
     */
    @Transactional(readOnly = true)
    public PaymentResponse getById(User user, Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));
        orderRepository.findByOrderCodeAndUserId(payment.getOrderId(), user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));
        return toResponse(payment);
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private void validatePaymentRequest(Order order, PaymentRequest request) {
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Cannot create payment for a cancelled order.");
        }
        if (order.getStatus() == OrderStatus.PAID || order.getStatus() == OrderStatus.COMPLETED) {
            throw new BadRequestException("Order has already been paid.");
        }
        if (order.getPaymentMethod() == PaymentMethod.COD) {
            throw new BadRequestException("COD orders do not need an online payment link.");
        }

        PaymentProvider expectedProvider = PaymentProvider.valueOf(order.getPaymentMethod().name());
        if (request.getProvider() != expectedProvider) {
            throw new BadRequestException("Payment provider does not match the order payment method.");
        }
        if (!Objects.equals(request.getAmount(), order.getTotalAmount())) {
            throw new BadRequestException("Payment amount does not match the order total.");
        }
    }

    private String buildOrderInfo(PaymentRequest request, Order order) {
        if (request.getOrderInfo() == null || request.getOrderInfo().isBlank()) {
            return "Thanh toan don hang " + order.getOrderCode();
        }
        return request.getOrderInfo().trim();
    }

    private void markOrderPaidIfSuccessful(Payment payment, PaymentStatus status) {
        if (status != PaymentStatus.SUCCESS) {
            return;
        }

        orderRepository.findByOrderCode(payment.getOrderId()).ifPresent(order -> {
            if (order.getStatus() == OrderStatus.PENDING) {
                order.setStatus(OrderStatus.PAID);
                orderRepository.save(order);
            }
        });
    }

    private PaymentResponse toResponse(Payment p) {
        return PaymentResponse.builder()
                .id(p.getId())
                .orderId(p.getOrderId())
                .amount(p.getAmount())
                .provider(p.getProvider())
                .status(p.getStatus())
                .transactionId(p.getTransactionId())
                .orderInfo(p.getOrderInfo())
                .paymentUrl(p.getPaymentUrl())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
