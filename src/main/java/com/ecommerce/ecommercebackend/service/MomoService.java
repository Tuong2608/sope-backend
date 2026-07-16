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
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

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
    public record CreateResult(
            String providerOrderId,
            String providerRequestId,
            String payUrl,
            String deeplink,
            String qrCodeUrl,
            String resultCode,
            String message) {}

    // ── Tạo link thanh toán ───────────────────────────────────────────────────

    /**
     * Gọi MoMo API để tạo giao dịch và lấy URL thanh toán.
     *
     * @param orderId   Mã đơn hàng
     * @param amount    Số tiền (VND)
     * @param orderInfo Nội dung thanh toán
     * @return URL trang thanh toán MoMo (dạng {@code https://test-payment.momo.vn/...})
     */
    public CreateResult createPayment(
            String providerOrderId,
            String requestId,
            Long amount,
            String orderInfo) {
        requireConfigured();
        String extraData = "";
        String requestType = momoConfig.getRequestType();

        // Raw signature string — thứ tự tham số theo đúng tài liệu MoMo
        String rawSignature = String.format(
                "accessKey=%s&amount=%s&extraData=%s&ipnUrl=%s&orderId=%s&orderInfo=%s&partnerCode=%s&redirectUrl=%s&requestId=%s&requestType=%s",
                momoConfig.getAccessKey(),
                amount,
                extraData,
                momoConfig.getIpnUrl(),
                providerOrderId,
                orderInfo,
                momoConfig.getPartnerCode(),
                momoConfig.getRedirectUrl(),
                requestId,
                requestType
        );

        String signature = hmacSHA256(momoConfig.getSecretKey(), rawSignature);

        // Build request body
        Map<String, Object> body = new HashMap<>();
        body.put("partnerCode",  momoConfig.getPartnerCode());
        body.put("requestId",    requestId);
        body.put("amount",       amount);
        body.put("orderId",      providerOrderId);
        body.put("orderInfo",    orderInfo);
        body.put("redirectUrl",  momoConfig.getRedirectUrl());
        body.put("ipnUrl",       momoConfig.getIpnUrl());
        body.put("requestType",  requestType);
        body.put("extraData",    extraData);
        body.put("lang",         "vi");
        body.put("signature",    signature);

        log.info("[MOMO] Gọi API tạo thanh toán providerOrderId={}", providerOrderId);

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
                .block(Duration.ofSeconds(35));

        if (response == null) {
            throw new IllegalStateException("MoMo API không trả về response");
        }

        Object resultCode = response.get("resultCode");
        if (resultCode == null || !resultCode.toString().equals("0")) {
            String msg = response.getOrDefault("message", "Lỗi không xác định").toString();
            log.error("[MOMO] Tạo giao dịch thất bại: resultCode={}, message={}", resultCode, msg);
            throw new IllegalStateException("MoMo tạo giao dịch thất bại: " + msg);
        }

        String payUrl = (String) response.get("payUrl");
        if (payUrl == null || payUrl.isBlank()) {
            throw new IllegalStateException("MoMo không trả về payUrl hợp lệ");
        }
        String deeplink = asString(response.get("deeplink"));
        String qrCodeUrl = firstHttpUrl(response.get("qrCodeUrl"), response.get("qrCode"));
        log.info("[MOMO] Tạo link thành công providerOrderId={}", providerOrderId);
        return new CreateResult(
                asString(response.getOrDefault("orderId", providerOrderId)),
                asString(response.getOrDefault("requestId", requestId)),
                payUrl,
                deeplink,
                qrCodeUrl,
                String.valueOf(resultCode),
                asString(response.get("message")));
    }

    // ── Xác thực chữ ký ──────────────────────────────────────────────────────

    /**
     * Xác thực chữ ký HMAC-SHA256 từ IPN/callback của MoMo.
     *
     * @param params Toàn bộ tham số nhận được từ MoMo
     * @return {@code true} nếu chữ ký hợp lệ
     */
    public boolean verifySignature(Map<String, String> params) {
        if (isBlank(momoConfig.getAccessKey()) || isBlank(momoConfig.getSecretKey())) return false;
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

    public boolean hasExpectedPartnerCode(Map<String, String> params) {
        return java.util.Objects.equals(momoConfig.getPartnerCode(), params.get("partnerCode"));
    }

    /**
     * Phân tích kết quả từ tham số IPN/callback của MoMo.
     *
     * @return {@link PaymentStatus#SUCCESS} nếu {@code resultCode = 0},
     *         ngược lại {@link PaymentStatus#FAILED}
     */
    public PaymentStatus resolveStatus(Map<String, String> params) {
        String resultCode = params.getOrDefault("resultCode", "-1");
        if ("0".equals(resultCode)) return PaymentStatus.SUCCESS;
        if ("7000".equals(resultCode) || "9000".equals(resultCode)) return PaymentStatus.PROCESSING;
        if ("1006".equals(resultCode)) return PaymentStatus.CANCELLED;
        if ("1005".equals(resultCode)) return PaymentStatus.EXPIRED;
        return PaymentStatus.FAILED;
    }

    /**
     * Lấy mã giao dịch MoMo từ tham số IPN.
     */
    public String extractTransactionId(Map<String, String> params) {
        return params.get("transId");
    }

    // ── Truy vấn trạng thái (Query) ──────────────────────────────────────────

    public Map<String, Object> queryTransaction(String orderId) {
        String requestId = java.util.UUID.randomUUID().toString();
        
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

        return sendPostRequest(momoConfig.getQueryEndpoint(), body);
    }

    // ── Hoàn tiền (Refund) ──────────────────────────────────────────────────

    public Map<String, Object> refundTransaction(String orderId, Long amount, String transId) {
        String requestId = java.util.UUID.randomUUID().toString();
        
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

        return sendPostRequest(momoConfig.getRefundEndpoint(), body);
    }

    private Map<String, Object> sendPostRequest(String endpoint, Map<String, Object> body) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClientBuilder.build()
                    .post()
                    .uri(endpoint)
                    .header("Content-Type", "application/json")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .map(m -> (Map<String, Object>) m)
                    .block(Duration.ofSeconds(35));
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

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }

    private String firstHttpUrl(Object... values) {
        for (Object value : values) {
            String candidate = asString(value);
            if (candidate != null && (candidate.startsWith("https://") || candidate.startsWith("http://"))) {
                return candidate;
            }
        }
        return null;
    }

    private void requireConfigured() {
        if (isBlank(momoConfig.getPartnerCode()) || isBlank(momoConfig.getAccessKey())
                || isBlank(momoConfig.getSecretKey()) || isBlank(momoConfig.getEndpoint())
                || isBlank(momoConfig.getRedirectUrl()) || isBlank(momoConfig.getIpnUrl())
                || isBlank(momoConfig.getRequestType())) {
            throw new IllegalStateException("MoMo Sandbox chưa được cấu hình đầy đủ");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
