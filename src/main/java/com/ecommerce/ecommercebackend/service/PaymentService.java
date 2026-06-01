package com.ecommerce.ecommercebackend.service;

import com.ecommerce.ecommercebackend.dto.request.PaymentRequest;
import com.ecommerce.ecommercebackend.dto.response.PaymentResponse;
import com.ecommerce.ecommercebackend.entity.Payment;
import com.ecommerce.ecommercebackend.entity.PaymentProvider;
import com.ecommerce.ecommercebackend.entity.PaymentStatus;
import com.ecommerce.ecommercebackend.exception.ResourceNotFoundException;
import com.ecommerce.ecommercebackend.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

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
    public PaymentResponse createPayment(PaymentRequest request, String ipAddress) {
        String paymentUrl;

        if (request.getProvider() == PaymentProvider.VNPAY) {
            paymentUrl = vnpayService.createPaymentUrl(
                    request.getOrderId(), request.getAmount(),
                    request.getOrderInfo(), ipAddress);
        } else {
            paymentUrl = momoService.createPaymentUrl(
                    request.getOrderId(), request.getAmount(), request.getOrderInfo());
        }

        Payment payment = Payment.builder()
                .orderId(request.getOrderId())
                .amount(request.getAmount())
                .provider(request.getProvider())
                .orderInfo(request.getOrderInfo())
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
     * <p>Xác thực chữ ký → cập nhật trạng thái Payment trong DB.
     * Gọi bởi VNPAY server, không qua trình duyệt người dùng.</p>
     *
     * @param params Tham số VNPAY gửi qua IPN
     */
    @Transactional
    public void handleVnpayIpn(Map<String, String> params) {
        if (!vnpayService.verifySignature(params)) {
            log.warn("[VNPAY IPN] Chữ ký không hợp lệ — bỏ qua");
            return;
        }
        // vnp_TxnRef = orderId_timestamp
        String txnRef  = params.getOrDefault("vnp_TxnRef", "");
        String orderId = txnRef.contains("_") ? txnRef.substring(0, txnRef.lastIndexOf('_')) : txnRef;

        paymentRepository.findByOrderId(orderId).ifPresent(payment -> {
            PaymentStatus newStatus = vnpayService.resolveStatus(params);
            String        txnId     = vnpayService.extractTransactionId(params);
            payment.setStatus(newStatus);
            payment.setTransactionId(txnId);
            paymentRepository.save(payment);
            log.info("[VNPAY IPN] Cập nhật Payment #{} → {} (txnId={})", payment.getId(), newStatus, txnId);
        });
    }

    // ── Xử lý IPN từ MoMo ────────────────────────────────────────────────────

    /**
     * Xử lý IPN (notify_url) từ MoMo.
     *
     * <p>Xác thực chữ ký → cập nhật trạng thái Payment trong DB.</p>
     *
     * @param params Tham số MoMo gửi qua IPN
     */
    @Transactional
    public void handleMomoIpn(Map<String, String> params) {
        if (!momoService.verifySignature(params)) {
            log.warn("[MOMO IPN] Chữ ký không hợp lệ — bỏ qua");
            return;
        }
        String orderId = params.getOrDefault("orderId", "");

        paymentRepository.findByOrderId(orderId).ifPresent(payment -> {
            PaymentStatus newStatus = momoService.resolveStatus(params);
            String        txnId     = momoService.extractTransactionId(params);
            payment.setStatus(newStatus);
            payment.setTransactionId(txnId);
            paymentRepository.save(payment);
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
    public PaymentResponse getById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));
        return toResponse(payment);
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

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
