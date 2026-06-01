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
import java.text.SimpleDateFormat;
import java.util.*;

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

    private static final String ALGORITHM = "HmacSHA512";
    private static final String VNP_VERSION = "2.1.0";
    private static final String VNP_COMMAND = "pay";
    private static final String VNP_CURR_CODE = "VND";
    private static final String VNP_LOCALE = "vn";
    private static final String VNP_ORDER_TYPE = "other";

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
    public String createPaymentUrl(String orderId, Long amount, String orderInfo, String ipAddress) {
        String txnRef = orderId + "_" + System.currentTimeMillis();
        String createDate = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());

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

        // Tạo query string để ký
        StringBuilder hashData = new StringBuilder();
        StringBuilder query   = new StringBuilder();
        params.forEach((key, value) -> {
            String encoded = URLEncoder.encode(value, StandardCharsets.US_ASCII);
            if (!hashData.isEmpty()) { hashData.append('&'); query.append('&'); }
            hashData.append(key).append('=').append(value);
            query.append(key).append('=').append(encoded);
        });

        String secureHash = hmacSHA512(vnpayConfig.getHashSecret(), hashData.toString());
        query.append("&vnp_SecureHash=").append(secureHash);

        String paymentUrl = vnpayConfig.getUrl() + "?" + query;
        log.info("[VNPAY] Tạo link thanh toán cho đơn hàng {}: {}", orderId, txnRef);
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
        String receivedHash = params.get("vnp_SecureHash");
        if (receivedHash == null) return false;

        // Loại bỏ các tham số liên quan đến chữ ký trước khi tính lại
        Map<String, String> cleanParams = new TreeMap<>(params);
        cleanParams.remove("vnp_SecureHash");
        cleanParams.remove("vnp_SecureHashType");

        StringBuilder hashData = new StringBuilder();
        cleanParams.forEach((key, value) -> {
            if (!hashData.isEmpty()) hashData.append('&');
            hashData.append(key).append('=').append(value);
        });

        String expectedHash = hmacSHA512(vnpayConfig.getHashSecret(), hashData.toString());
        boolean valid = expectedHash.equalsIgnoreCase(receivedHash);
        if (!valid) log.warn("[VNPAY] Chữ ký không hợp lệ! Expected: {}, Received: {}", expectedHash, receivedHash);
        return valid;
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
        return "00".equals(responseCode) ? PaymentStatus.SUCCESS : PaymentStatus.FAILED;
    }

    /**
     * Lấy mã giao dịch VNPAY từ tham số IPN.
     */
    public String extractTransactionId(Map<String, String> params) {
        return params.get("vnp_TransactionNo");
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
}
