package com.ecommerce.ecommercebackend.service;

import com.ecommerce.ecommercebackend.config.VnpayConfig;
import com.ecommerce.ecommercebackend.entity.PaymentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpEntity;

/**
 * Service xử lý tích hợp thanh toán VNPAY.
 *
 * <p>Tài liệu tham khảo:
 * https://sandbox.vnpayment.vn/apis/docs/thanh-toan-pay/pay.html</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VnpayService {

    private final VnpayConfig vnpayConfig;
    private final RestTemplate restTemplate;

    private static final String ALGORITHM = "HmacSHA512";
    private static final String VNP_VERSION = "2.1.0";
    private static final String VNP_COMMAND = "pay";
    private static final String VNP_CURR_CODE = "VND";
    private static final String VNP_LOCALE = "vn";
    private static final String VNP_ORDER_TYPE = "other";
    private static final DateTimeFormatter VNP_DATE = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    // ── Tạo link thanh toán ───────────────────────────────────────────────────

    /**
     * Tạo URL thanh toán VNPAY cho một đơn hàng.
     *
     * @param orderId   Mã đơn hàng (unique)
     * @param amount    Số tiền (VND)
     * @param orderInfo Nội dung thanh toán
     * @param ipAddress IP của người dùng (dùng "127.0.0.1" cho test)
     * @return URL trang thanh toán VNPAY
     */
    public String createPaymentUrl(
            String txnRef,
            Long amount,
            String orderInfo,
            String ipAddress,
            String bankCode,
            LocalDateTime expiresAt) {
        requireConfigured();
        LocalDateTime now = LocalDateTime.now(VIETNAM_ZONE);
        String createDate = now.format(VNP_DATE);

        // Build sorted param map (VNPAY yêu cầu sắp xếp theo alphabet)
        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Version",    VNP_VERSION);
        params.put("vnp_Command",    VNP_COMMAND);
        params.put("vnp_TmnCode",    vnpayConfig.getTmnCode());
        params.put("vnp_Amount",     String.valueOf(amount * 100)); // VNPAY tính theo đơn vị 1/100
        params.put("vnp_CurrCode",   VNP_CURR_CODE);
        params.put("vnp_TxnRef",     txnRef);
        params.put("vnp_OrderInfo",  orderInfo);
        params.put("vnp_OrderType",  VNP_ORDER_TYPE);
        params.put("vnp_Locale",     VNP_LOCALE);
        params.put("vnp_ReturnUrl",  vnpayConfig.getReturnUrl());
        params.put("vnp_IpAddr",     ipAddress);
        params.put("vnp_CreateDate", createDate);
        params.put("vnp_ExpireDate", expiresAt.format(VNP_DATE));
        if (bankCode != null && !bankCode.isBlank()) {
            params.put("vnp_BankCode", bankCode);
        }

        // Tạo query string để ký
        StringBuilder hashData = new StringBuilder();
        StringBuilder query   = new StringBuilder();
        params.forEach((key, value) -> {
            String encodedKey = encode(key);
            String encoded = encode(value);
            if (!hashData.isEmpty()) { hashData.append('&'); query.append('&'); }
            hashData.append(encodedKey).append('=').append(encoded);
            query.append(encodedKey).append('=').append(encoded);
        });

        String secureHash = hmacSHA512(vnpayConfig.getHashSecret(), hashData.toString());
        query.append("&vnp_SecureHash=").append(secureHash);

        String paymentUrl = vnpayConfig.getUrl() + "?" + query;
        log.info("[VNPAY] Đã tạo URL cho txnRef={}", txnRef);
        return paymentUrl;
    }

    // ── Xác thực chữ ký ──────────────────────────────────────────────────────

    /**
     * Xác thực chữ ký HMAC-SHA512 từ callback/IPN của VNPAY.
     *
     * @param params Toàn bộ tham số nhận được từ VNPAY
     * @return {@code true} nếu chữ ký hợp lệ
     */
    public boolean verifySignature(Map<String, String> params) {
        if (isBlank(vnpayConfig.getTmnCode()) || isBlank(vnpayConfig.getHashSecret())) return false;
        String receivedHash = params.get("vnp_SecureHash");
        if (receivedHash == null) return false;

        // Loại bỏ các tham số liên quan đến chữ ký trước khi tính lại
        Map<String, String> cleanParams = new TreeMap<>(params);
        cleanParams.remove("vnp_SecureHash");
        cleanParams.remove("vnp_SecureHashType");

        StringBuilder hashData = new StringBuilder();
        cleanParams.forEach((key, value) -> {
            if (!hashData.isEmpty()) hashData.append('&');
            hashData.append(encode(key)).append('=').append(encode(value));
        });

        String expectedHash = hmacSHA512(vnpayConfig.getHashSecret(), hashData.toString());
        boolean valid = expectedHash.equalsIgnoreCase(receivedHash);
        if (!valid) log.warn("[VNPAY] Chữ ký không hợp lệ");
        return valid;
    }

    public boolean hasExpectedTmnCode(Map<String, String> params) {
        return Objects.equals(vnpayConfig.getTmnCode(), params.get("vnp_TmnCode"));
    }

    /**
     * Phân tích kết quả từ tham số IPN/callback của VNPAY.
     *
     * @param params Tham số VNPAY gửi về
     * @return {@link PaymentStatus#SUCCESS} nếu {@code vnp_ResponseCode = "00"},
     *         ngược lại {@link PaymentStatus#FAILED}
     */
    public PaymentStatus resolveStatus(Map<String, String> params) {
        String responseCode = params.getOrDefault("vnp_ResponseCode", "");
        String transactionStatus = params.getOrDefault("vnp_TransactionStatus", "");
        if ("00".equals(responseCode) && "00".equals(transactionStatus)) {
            return PaymentStatus.SUCCESS;
        }
        if ("24".equals(responseCode)) return PaymentStatus.CANCELLED;
        if ("11".equals(responseCode)) return PaymentStatus.EXPIRED;
        return PaymentStatus.FAILED;
    }

    /**
     * Lấy mã giao dịch VNPAY từ tham số IPN.
     */
    public String extractTransactionId(Map<String, String> params) {
        return params.get("vnp_TransactionNo");
    }

    // ── Truy vấn trạng thái (QueryDR) ──────────────────────────────────────────

    public Map<String, Object> queryTransaction(String orderId, String transDate, String ipAddress) {
        String requestId = UUID.randomUUID().toString();
        String createDate = LocalDateTime.now(VIETNAM_ZONE).format(VNP_DATE);

        Map<String, Object> params = new HashMap<>();
        params.put("vnp_RequestId", requestId);
        params.put("vnp_Version", VNP_VERSION);
        params.put("vnp_Command", "querydr");
        params.put("vnp_TmnCode", vnpayConfig.getTmnCode());
        params.put("vnp_TxnRef", orderId);
        params.put("vnp_OrderInfo", "Query transaction " + orderId);
        params.put("vnp_TransactionDate", transDate); // yyyyMMddHHmmss format from original payment
        params.put("vnp_CreateDate", createDate);
        params.put("vnp_IpAddr", ipAddress);

        String hashData = requestId + "|" + VNP_VERSION + "|querydr|" + vnpayConfig.getTmnCode() + "|" 
                + orderId + "|" + transDate + "|" + createDate + "|" + ipAddress + "|" + params.get("vnp_OrderInfo");
        
        String secureHash = hmacSHA512(vnpayConfig.getHashSecret(), hashData);
        params.put("vnp_SecureHash", secureHash);

        return sendPostRequest(vnpayConfig.getApiUrl(), params);
    }

    // ── Hoàn tiền (Refund) ──────────────────────────────────────────────────

    public Map<String, Object> refundTransaction(String orderId, Long amount, String transDate, String user, String ipAddress) {
        String requestId = UUID.randomUUID().toString();
        String createDate = LocalDateTime.now(VIETNAM_ZONE).format(VNP_DATE);

        Map<String, Object> params = new HashMap<>();
        params.put("vnp_RequestId", requestId);
        params.put("vnp_Version", VNP_VERSION);
        params.put("vnp_Command", "refund");
        params.put("vnp_TmnCode", vnpayConfig.getTmnCode());
        params.put("vnp_TransactionType", "02"); // 02: Refund toàn phần, 03: Refund 1 phần
        params.put("vnp_TxnRef", orderId);
        params.put("vnp_Amount", amount * 100);
        params.put("vnp_OrderInfo", "Refund order " + orderId);
        params.put("vnp_TransactionDate", transDate);
        params.put("vnp_CreateBy", user);
        params.put("vnp_CreateDate", createDate);
        params.put("vnp_IpAddr", ipAddress);

        String hashData = requestId + "|" + VNP_VERSION + "|refund|" + vnpayConfig.getTmnCode() + "|02|"
                + orderId + "|" + (amount * 100) + "|" + transDate + "|" + user + "|" + createDate + "|" 
                + ipAddress + "|" + params.get("vnp_OrderInfo");

        String secureHash = hmacSHA512(vnpayConfig.getHashSecret(), hashData);
        params.put("vnp_SecureHash", secureHash);

        return sendPostRequest(vnpayConfig.getApiUrl(), params);
    }

    private Map<String, Object> sendPostRequest(String url, Map<String, Object> payload) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
        
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("[VNPAY API] Call failed", e);
            Map<String, Object> error = new HashMap<>();
            error.put("vnp_ResponseCode", "99");
            error.put("vnp_Message", "Exception: " + e.getMessage());
            return error;
        }
    }

    // ── HMAC-SHA512 ───────────────────────────────────────────────────────────

    private String hmacSHA512(String key, String data) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Hex.encodeHexString(rawHmac);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tạo chữ ký HMAC-SHA512", e);
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private void requireConfigured() {
        if (isBlank(vnpayConfig.getTmnCode()) || isBlank(vnpayConfig.getHashSecret())
                || isBlank(vnpayConfig.getUrl()) || isBlank(vnpayConfig.getReturnUrl())) {
            throw new IllegalStateException("VNPAY Sandbox chưa được cấu hình đầy đủ");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
