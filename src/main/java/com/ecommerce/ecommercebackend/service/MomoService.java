package com.ecommerce.ecommercebackend.service;

import com.ecommerce.ecommercebackend.config.MomoConfig;
import com.ecommerce.ecommercebackend.entity.PaymentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service xử lý tích hợp thanh toán MoMo Sandbox.
 *
 * <p>Tài liệu tham khảo:
 * https://developers.momo.vn/v3/vi/docs/payment/api/payment-api/pay-gate</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MomoService {

    private final MomoConfig   momoConfig;
    private final WebClient.Builder webClientBuilder;

    private static final String ALGORITHM    = "HmacSHA256";
    private static final String REQUEST_TYPE = "payWithMethod";

    // ── Tạo link thanh toán ───────────────────────────────────────────────────

    /**
     * Gọi MoMo API để tạo giao dịch và lấy URL thanh toán.
     *
     * @param orderId   Mã đơn hàng
     * @param amount    Số tiền (VND)
     * @param orderInfo Nội dung thanh toán
     * @return URL trang thanh toán MoMo (dạng {@code https://test-payment.momo.vn/...})
     */
    public String createPaymentUrl(String orderId, Long amount, String orderInfo) {
        String requestId = UUID.randomUUID().toString();
        String extraData = "";

        // Raw signature string — thứ tự tham số theo đúng tài liệu MoMo
        String rawSignature = String.format(
                "accessKey=%s&amount=%s&extraData=%s&ipnUrl=%s&orderId=%s&orderInfo=%s&partnerCode=%s&redirectUrl=%s&requestId=%s&requestType=%s",
                momoConfig.getAccessKey(),
                amount,
                extraData,
                momoConfig.getNotifyUrl(),
                orderId,
                orderInfo,
                momoConfig.getPartnerCode(),
                momoConfig.getReturnUrl(),
                requestId,
                REQUEST_TYPE
        );

        String signature = hmacSHA256(momoConfig.getSecretKey(), rawSignature);

        // Build request body
        Map<String, Object> body = new HashMap<>();
        body.put("partnerCode",  momoConfig.getPartnerCode());
        body.put("requestId",    requestId);
        body.put("amount",       amount);
        body.put("orderId",      orderId);
        body.put("orderInfo",    orderInfo);
        body.put("redirectUrl",  momoConfig.getReturnUrl());
        body.put("ipnUrl",       momoConfig.getNotifyUrl());
        body.put("requestType",  REQUEST_TYPE);
        body.put("extraData",    extraData);
        body.put("lang",         "vi");
        body.put("signature",    signature);

        log.info("[MOMO] Gọi API tạo thanh toán cho đơn hàng {}", orderId);

        // Gọi MoMo API và lấy payUrl
        @SuppressWarnings("unchecked")
        Map<String, Object> response = webClientBuilder.build()
                .post()
                .uri(momoConfig.getEndpoint())
                .header("Content-Type", "application/json")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .map(m -> (Map<String, Object>) m)
                .block();

        if (response == null) {
            throw new RuntimeException("MoMo API không trả về response");
        }

        Object resultCode = response.get("resultCode");
        if (resultCode == null || !resultCode.toString().equals("0")) {
            String msg = response.getOrDefault("message", "Lỗi không xác định").toString();
            log.error("[MOMO] Tạo giao dịch thất bại: resultCode={}, message={}", resultCode, msg);
            throw new RuntimeException("MoMo tạo giao dịch thất bại: " + msg);
        }

        String payUrl = (String) response.get("payUrl");
        log.info("[MOMO] Tạo link thành công cho đơn hàng {}: {}", orderId, payUrl);
        return payUrl;
    }

    // ── Xác thực chữ ký ──────────────────────────────────────────────────────

    /**
     * Xác thực chữ ký HMAC-SHA256 từ IPN/callback của MoMo.
     *
     * @param params Toàn bộ tham số nhận được từ MoMo
     * @return {@code true} nếu chữ ký hợp lệ
     */
    public boolean verifySignature(Map<String, String> params) {
        String receivedSignature = params.get("signature");
        if (receivedSignature == null) return false;

        // Raw signature string theo đúng thứ tự tài liệu MoMo IPN
        String rawSignature = String.format(
                "accessKey=%s&amount=%s&extraData=%s&message=%s&orderId=%s&orderInfo=%s&orderType=%s&partnerCode=%s&payType=%s&requestId=%s&responseTime=%s&resultCode=%s&transId=%s",
                momoConfig.getAccessKey(),
                params.getOrDefault("amount", ""),
                params.getOrDefault("extraData", ""),
                params.getOrDefault("message", ""),
                params.getOrDefault("orderId", ""),
                params.getOrDefault("orderInfo", ""),
                params.getOrDefault("orderType", ""),
                params.getOrDefault("partnerCode", ""),
                params.getOrDefault("payType", ""),
                params.getOrDefault("requestId", ""),
                params.getOrDefault("responseTime", ""),
                params.getOrDefault("resultCode", ""),
                params.getOrDefault("transId", "")
        );

        String expectedSignature = hmacSHA256(momoConfig.getSecretKey(), rawSignature);
        boolean valid = expectedSignature.equalsIgnoreCase(receivedSignature);
        if (!valid) log.warn("[MOMO] Chữ ký không hợp lệ!");
        return valid;
    }

    /**
     * Phân tích kết quả từ tham số IPN/callback của MoMo.
     *
     * @return {@link PaymentStatus#SUCCESS} nếu {@code resultCode = 0},
     *         ngược lại {@link PaymentStatus#FAILED}
     */
    public PaymentStatus resolveStatus(Map<String, String> params) {
        String resultCode = params.getOrDefault("resultCode", "-1");
        return "0".equals(resultCode) ? PaymentStatus.SUCCESS : PaymentStatus.FAILED;
    }

    /**
     * Lấy mã giao dịch MoMo từ tham số IPN.
     */
    public String extractTransactionId(Map<String, String> params) {
        return params.get("transId");
    }

    // ── Truy vấn trạng thái (Query) ──────────────────────────────────────────

    public Map<String, Object> queryTransaction(String orderId) {
        String requestId = UUID.randomUUID().toString();
        String requestType = "transactionStatus";
        
        String rawSignature = String.format(
                "accessKey=%s&orderId=%s&partnerCode=%s&requestId=%s",
                momoConfig.getAccessKey(), orderId, momoConfig.getPartnerCode(), requestId
        );
        String signature = hmacSHA256(momoConfig.getSecretKey(), rawSignature);

        Map<String, Object> body = new HashMap<>();
        body.put("partnerCode", momoConfig.getPartnerCode());
        body.put("requestId", requestId);
        body.put("orderId", orderId);
        body.put("lang", "vi");
        body.put("signature", signature);

        return sendPostRequest(body);
    }

    // ── Hoàn tiền (Refund) ──────────────────────────────────────────────────

    public Map<String, Object> refundTransaction(String orderId, Long amount, String transId) {
        String requestId = UUID.randomUUID().toString();
        
        String rawSignature = String.format(
                "accessKey=%s&amount=%s&description=%s&orderId=%s&partnerCode=%s&requestId=%s&transId=%s",
                momoConfig.getAccessKey(), amount, "Refund order " + orderId, orderId, momoConfig.getPartnerCode(), requestId, transId
        );
        String signature = hmacSHA256(momoConfig.getSecretKey(), rawSignature);

        Map<String, Object> body = new HashMap<>();
        body.put("partnerCode", momoConfig.getPartnerCode());
        body.put("requestId", requestId);
        body.put("orderId", orderId);
        body.put("amount", amount);
        body.put("transId", transId);
        body.put("lang", "vi");
        body.put("description", "Refund order " + orderId);
        body.put("signature", signature);

        return sendPostRequest(body);
    }

    private Map<String, Object> sendPostRequest(Map<String, Object> body) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClientBuilder.build()
                    .post()
                    .uri(momoConfig.getEndpoint()) // using the same endpoint for pay, query, refund
                    .header("Content-Type", "application/json")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .map(m -> (Map<String, Object>) m)
                    .block();
            return response != null ? response : new HashMap<>();
        } catch (Exception e) {
            log.error("[MOMO API] Call failed", e);
            Map<String, Object> error = new HashMap<>();
            error.put("resultCode", 99);
            error.put("message", "Exception: " + e.getMessage());
            return error;
        }
    }

    // ── HMAC-SHA256 ───────────────────────────────────────────────────────────

    private String hmacSHA256(String key, String data) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Hex.encodeHexString(rawHmac);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tạo chữ ký HMAC-SHA256", e);
        }
    }
}
