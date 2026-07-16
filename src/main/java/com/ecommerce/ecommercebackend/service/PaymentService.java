package com.ecommerce.ecommercebackend.service;

import com.ecommerce.ecommercebackend.dto.request.PaymentRequest;
import com.ecommerce.ecommercebackend.dto.response.PaymentResponse;
import com.ecommerce.ecommercebackend.entity.*;
import com.ecommerce.ecommercebackend.exception.BadRequestException;
import com.ecommerce.ecommercebackend.exception.ResourceNotFoundException;
import com.ecommerce.ecommercebackend.repository.OrderRepository;
import com.ecommerce.ecommercebackend.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Điều phối payment attempt và là ranh giới transaction của callback/IPN. */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final int PAYMENT_EXPIRY_MINUTES = 15;
    private static final Set<String> VNPAY_CHANNELS = Set.of("VNPAYQR", "VNBANK", "INTCARD");

    public record IpnResult(String code, String message) {}

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final VnpayService vnpayService;
    private final MomoService momoService;

    @Transactional
    public PaymentResponse createPayment(User user, PaymentRequest request, String ipAddress) {
        Order order = orderRepository.findByOrderCodeAndUserId(request.getOrderId(), user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found with code: " + request.getOrderId()));
        validateOrderForPayment(order, request.getProvider());
        String channel = normalizeChannel(request.getProvider(), request.getChannel());

        Optional<Payment> activeAttempt = paymentRepository
                .findFirstByOrderIdAndProviderAndStatusInOrderByCreatedAtDesc(
                        order.getOrderCode(), request.getProvider(),
                        List.of(PaymentStatus.PENDING, PaymentStatus.PROCESSING));
        if (activeAttempt.isPresent()) {
            Payment existing = activeAttempt.get();
            if (existing.getExpiredAt() == null || !existing.getExpiredAt().isAfter(LocalDateTime.now())) {
                if (existing.getStatus() != PaymentStatus.SUCCESS) {
                    existing.setStatus(PaymentStatus.EXPIRED);
                    existing.setResponseMessage("Giao dịch đã hết hạn");
                    paymentRepository.save(existing);
                }
            } else if (Objects.equals(existing.getPaymentChannel(), channel)) {
                return toResponse(existing, order);
            } else {
                throw new BadRequestException("Đơn hàng đang có một giao dịch chờ xử lý.");
            }
        }

        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(PAYMENT_EXPIRY_MINUTES);
        String providerOrderId = generateProviderOrderId(request.getProvider());
        String providerRequestId = request.getProvider() == PaymentProvider.MOMO
                ? "REQ" + UUID.randomUUID().toString().replace("-", "")
                : null;
        String orderInfo = "Thanh toan don hang " + order.getOrderCode();

        Payment payment = Payment.builder()
                .orderId(order.getOrderCode())
                .amount(order.getTotalAmount())
                .provider(request.getProvider())
                .providerOrderId(providerOrderId)
                .providerRequestId(providerRequestId)
                .paymentChannel(channel)
                .orderInfo(orderInfo)
                .status(PaymentStatus.PENDING)
                .expiredAt(expiresAt)
                .build();
        payment = paymentRepository.saveAndFlush(payment);

        try {
            if (request.getProvider() == PaymentProvider.VNPAY) {
                payment.setPaymentUrl(vnpayService.createPaymentUrl(
                        providerOrderId,
                        payment.getAmount(),
                        orderInfo,
                        ipAddress,
                        channel,
                        expiresAt));
                payment.setResponseCode("00");
                payment.setResponseMessage("Đã tạo URL thanh toán VNPAY");
            } else {
                MomoService.CreateResult result = momoService.createPayment(
                        providerOrderId,
                        providerRequestId,
                        payment.getAmount(),
                        orderInfo);
                payment.setProviderOrderId(result.providerOrderId());
                payment.setProviderRequestId(result.providerRequestId());
                payment.setPaymentUrl(result.payUrl());
                payment.setDeeplink(result.deeplink());
                payment.setQrCodeUrl(result.qrCodeUrl());
                payment.setResponseCode(result.resultCode());
                payment.setResponseMessage(result.message());
            }
        } catch (RuntimeException ex) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(safeMessage(ex));
            payment.setResponseMessage("Không thể khởi tạo giao dịch với cổng thanh toán");
            log.warn("[PAYMENT] Khởi tạo provider thất bại paymentId={} provider={}: {}",
                    payment.getId(), payment.getProvider(), safeMessage(ex));
        }

        payment = paymentRepository.save(payment);
        log.info("[PAYMENT] Tạo paymentId={} provider={} orderCode={} providerOrderId={}",
                payment.getId(), payment.getProvider(), payment.getOrderId(), payment.getProviderOrderId());
        return toResponse(payment, order);
    }

    @Transactional
    public PaymentResponse retry(User user, Long paymentId, String ipAddress) {
        Payment oldPayment = paymentRepository.findLockedById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + paymentId));
        Order order = requireOwnedOrder(user, oldPayment);
        if (!canRetry(oldPayment, order)) {
            throw new BadRequestException("Giao dịch này không thể thanh toán lại.");
        }

        PaymentRequest request = new PaymentRequest();
        request.setOrderId(order.getOrderCode());
        request.setProvider(oldPayment.getProvider());
        request.setChannel(oldPayment.getPaymentChannel());
        return createPayment(user, request, ipAddress);
    }

    /** Return URL chỉ lưu dữ liệu đã ký; SUCCESS vẫn phải chờ IPN. */
    @Transactional
    public Long handleVnpayReturn(Map<String, String> params) {
        String providerOrderId = params.get("vnp_TxnRef");
        if (providerOrderId == null) return null;
        Optional<Payment> found = paymentRepository.findLockedByProviderOrderId(providerOrderId);
        if (found.isEmpty()) return null;
        Payment payment = found.get();

        boolean valid = vnpayService.verifySignature(params)
                && vnpayService.hasExpectedTmnCode(params)
                && amountMatchesVnpay(payment, params.get("vnp_Amount"));
        payment.setSignatureVerified(valid);
        if (valid) {
            copyVnpayMetadata(payment, params);
            if (payment.getStatus() == PaymentStatus.PENDING) {
                payment.setStatus(PaymentStatus.PROCESSING);
                payment.setResponseMessage("Đã nhận Return URL, đang chờ VNPAY IPN xác nhận");
            }
        } else {
            payment.setFailureReason("Return URL VNPAY không vượt qua bước xác minh");
        }
        paymentRepository.save(payment);
        return payment.getId();
    }

    @Transactional
    public IpnResult handleVnpayIpn(Map<String, String> params) {
        if (!vnpayService.verifySignature(params) || !vnpayService.hasExpectedTmnCode(params)) {
            return new IpnResult("97", "Invalid Checksum");
        }
        String providerOrderId = params.get("vnp_TxnRef");
        Optional<Payment> found = paymentRepository.findLockedByProviderOrderId(providerOrderId);
        if (found.isEmpty()) return new IpnResult("01", "Order not Found");
        Payment payment = found.get();
        if (!amountMatchesVnpay(payment, params.get("vnp_Amount"))) {
            return new IpnResult("04", "Invalid Amount");
        }
        if (payment.getStatus() == PaymentStatus.SUCCESS || payment.getStatus() == PaymentStatus.REFUNDED) {
            return new IpnResult("02", "Order already confirmed");
        }

        PaymentStatus newStatus = vnpayService.resolveStatus(params);
        copyVnpayMetadata(payment, params);
        payment.setSignatureVerified(true);
        applyVerifiedStatus(payment, newStatus);
        paymentRepository.save(payment);
        return new IpnResult("00", "Confirm Success");
    }

    /** Return URL chỉ lưu dữ liệu đã ký; SUCCESS vẫn phải chờ IPN. */
    @Transactional
    public Long handleMomoReturn(Map<String, String> params) {
        String providerOrderId = params.get("orderId");
        if (providerOrderId == null) return null;
        Optional<Payment> found = paymentRepository.findLockedByProviderOrderId(providerOrderId);
        if (found.isEmpty()) return null;
        Payment payment = found.get();

        boolean valid = momoService.verifySignature(params)
                && momoService.hasExpectedPartnerCode(params)
                && amountMatches(payment, params.get("amount"))
                && requestIdMatches(payment, params.get("requestId"));
        payment.setSignatureVerified(valid);
        if (valid) {
            copyMomoMetadata(payment, params);
            if (payment.getStatus() == PaymentStatus.PENDING) {
                payment.setStatus(PaymentStatus.PROCESSING);
                payment.setResponseMessage("Đã nhận Return URL, đang chờ MoMo IPN xác nhận");
            }
        } else {
            payment.setFailureReason("Return URL MoMo không vượt qua bước xác minh");
        }
        paymentRepository.save(payment);
        return payment.getId();
    }

    @Transactional
    public boolean handleMomoIpn(Map<String, String> params) {
        if (!momoService.verifySignature(params) || !momoService.hasExpectedPartnerCode(params)) {
            return false;
        }
        String providerOrderId = params.get("orderId");
        Optional<Payment> found = paymentRepository.findLockedByProviderOrderId(providerOrderId);
        if (found.isEmpty()) return false;
        Payment payment = found.get();
        if (!amountMatches(payment, params.get("amount"))
                || !requestIdMatches(payment, params.get("requestId"))) {
            return false;
        }
        if (payment.getStatus() == PaymentStatus.SUCCESS || payment.getStatus() == PaymentStatus.REFUNDED) {
            return true;
        }

        PaymentStatus newStatus = momoService.resolveStatus(params);
        copyMomoMetadata(payment, params);
        payment.setSignatureVerified(true);
        applyVerifiedStatus(payment, newStatus);
        paymentRepository.save(payment);
        return true;
    }

    private void applyVerifiedStatus(Payment payment, PaymentStatus newStatus) {
        if (payment.getStatus() == PaymentStatus.SUCCESS || payment.getStatus() == PaymentStatus.REFUNDED) {
            return;
        }
        payment.setStatus(newStatus);
        if (newStatus == PaymentStatus.SUCCESS) {
            payment.setPaidAt(LocalDateTime.now());
            orderService.markAsPaid(payment.getOrderId());
        }
    }

    @Transactional(readOnly = true)
    public PaymentResponse getById(User user, Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));
        return toResponse(payment, requireOwnedOrder(user, payment));
    }

    @Transactional
    public Map<String, Object> checkPaymentStatus(Long id, String ipAddress) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));
        if (payment.getProvider() == PaymentProvider.VNPAY) {
            String transDate = payment.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            return vnpayService.queryTransaction(payment.getProviderOrderId(), transDate, ipAddress);
        }
        return momoService.queryTransaction(payment.getProviderOrderId());
    }

    @Transactional
    public Map<String, Object> refundPayment(Long id, User user, String ipAddress) {
        Payment payment = paymentRepository.findLockedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));
        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new BadRequestException("Only successful payments can be refunded.");
        }
        Map<String, Object> result;
        if (payment.getProvider() == PaymentProvider.VNPAY) {
            String transDate = payment.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            result = vnpayService.refundTransaction(payment.getProviderOrderId(), payment.getAmount(),
                    transDate, user.getEmail(), ipAddress);
        } else {
            result = momoService.refundTransaction(payment.getProviderOrderId(), payment.getAmount(),
                    payment.getTransactionId());
        }
        boolean accepted = payment.getProvider() == PaymentProvider.VNPAY
                ? "00".equals(String.valueOf(result.get("vnp_ResponseCode")))
                : "0".equals(String.valueOf(result.get("resultCode")));
        if (accepted) payment.setStatus(PaymentStatus.REFUNDED);
        return result;
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getAllPayments(PaymentStatus status) {
        List<Payment> payments = status == null
                ? paymentRepository.findAllByOrderByCreatedAtDesc()
                : paymentRepository.findByStatusOrderByCreatedAtDesc(status);
        return payments.stream().map(payment -> {
            Order order = orderRepository.findByOrderCode(payment.getOrderId()).orElse(null);
            return toResponse(payment, order);
        }).toList();
    }

    private void validateOrderForPayment(Order order, PaymentProvider provider) {
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Cannot create payment for a cancelled order.");
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BadRequestException("Order has already been paid or processed.");
        }
        if (order.getPaymentMethod() == PaymentMethod.COD) {
            throw new BadRequestException("COD orders do not need an online payment link.");
        }
        if (PaymentProvider.valueOf(order.getPaymentMethod().name()) != provider) {
            throw new BadRequestException("Payment provider does not match the order payment method.");
        }
        if (order.getTotalAmount() == null || order.getTotalAmount() < 1_000) {
            throw new BadRequestException("Order total is invalid for online payment.");
        }
    }

    private String normalizeChannel(PaymentProvider provider, String rawChannel) {
        if (provider == PaymentProvider.MOMO) {
            if (rawChannel != null && !rawChannel.isBlank()) {
                throw new BadRequestException("MoMo does not support VNPAY channel.");
            }
            return null;
        }
        if (rawChannel == null || rawChannel.isBlank()) return null;
        String channel = rawChannel.trim().toUpperCase(Locale.ROOT);
        if (!VNPAY_CHANNELS.contains(channel)) {
            throw new BadRequestException("Unsupported VNPAY channel: " + channel);
        }
        return channel;
    }

    private Order requireOwnedOrder(User user, Payment payment) {
        return orderRepository.findByOrderCodeAndUserId(payment.getOrderId(), user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment not found with id: " + payment.getId()));
    }

    private boolean amountMatchesVnpay(Payment payment, String rawAmount) {
        try {
            return Math.multiplyExact(payment.getAmount(), 100L) == Long.parseLong(rawAmount);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private boolean amountMatches(Payment payment, String rawAmount) {
        try {
            return payment.getAmount() == Long.parseLong(rawAmount);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private boolean requestIdMatches(Payment payment, String requestId) {
        return requestId != null && requestId.equals(payment.getProviderRequestId());
    }

    private void copyVnpayMetadata(Payment payment, Map<String, String> params) {
        payment.setTransactionId(params.get("vnp_TransactionNo"));
        payment.setResponseCode(params.get("vnp_ResponseCode"));
        payment.setTransactionStatus(params.get("vnp_TransactionStatus"));
        payment.setBankCode(params.get("vnp_BankCode"));
        payment.setCardType(params.get("vnp_CardType"));
        payment.setProviderPayDate(params.get("vnp_PayDate"));
        payment.setResponseMessage(vnpayMessage(params.get("vnp_ResponseCode")));
    }

    private void copyMomoMetadata(Payment payment, Map<String, String> params) {
        payment.setTransactionId(params.get("transId"));
        payment.setResponseCode(params.get("resultCode"));
        payment.setTransactionStatus(params.get("resultCode"));
        payment.setResponseMessage(params.get("message"));
        payment.setProviderPayDate(params.get("responseTime"));
    }

    private boolean canRetry(Payment payment, Order order) {
        if (order == null || order.getStatus() != OrderStatus.PENDING) return false;
        if (payment.getStatus() == PaymentStatus.FAILED
                || payment.getStatus() == PaymentStatus.CANCELLED
                || payment.getStatus() == PaymentStatus.EXPIRED) return true;
        return payment.getStatus() == PaymentStatus.PENDING
                && (payment.getExpiredAt() == null
                || !payment.getExpiredAt().isAfter(LocalDateTime.now()));
    }

    private PaymentResponse toResponse(Payment payment, Order order) {
        boolean retry = canRetry(payment, order);
        return PaymentResponse.builder()
                .id(payment.getId())
                .paymentId(payment.getId())
                .orderId(order != null ? order.getId() : null)
                .orderCode(payment.getOrderId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .provider(payment.getProvider())
                .status(payment.getStatus())
                .orderStatus(order != null ? order.getStatus() : null)
                .transactionId(payment.getTransactionId())
                .providerTransactionId(payment.getTransactionId())
                .providerOrderId(payment.getProviderOrderId())
                .providerRequestId(payment.getProviderRequestId())
                .orderInfo(payment.getOrderInfo())
                .paymentChannel(payment.getPaymentChannel())
                .responseCode(payment.getResponseCode())
                .transactionStatus(payment.getTransactionStatus())
                .responseMessage(payment.getResponseMessage())
                .bankCode(payment.getBankCode())
                .cardType(payment.getCardType())
                .payDate(payment.getProviderPayDate())
                .paidAt(payment.getPaidAt())
                .expiresAt(payment.getExpiredAt())
                .signatureVerified(payment.isSignatureVerified())
                .canRetry(retry)
                .paymentUrl(payment.getPaymentUrl())
                .payUrl(payment.getPaymentUrl())
                .deeplink(payment.getDeeplink())
                .qrCodeUrl(payment.getQrCodeUrl())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }

    private String generateProviderOrderId(PaymentProvider provider) {
        String prefix = provider == PaymentProvider.VNPAY ? "VP" : "MM";
        return prefix + System.currentTimeMillis() + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }

    private String vnpayMessage(String code) {
        if ("00".equals(code)) return "Giao dịch thành công tại VNPAY";
        if ("24".equals(code)) return "Người dùng đã hủy giao dịch";
        if ("11".equals(code)) return "Giao dịch đã hết hạn";
        return code == null ? null : "VNPAY response code: " + code;
    }

    private String safeMessage(Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null || message.isBlank()) return throwable.getClass().getSimpleName();
        return message.length() > 450 ? message.substring(0, 450) : message;
    }
}
