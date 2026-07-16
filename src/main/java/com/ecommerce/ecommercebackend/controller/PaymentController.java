package com.ecommerce.ecommercebackend.controller;

import com.ecommerce.ecommercebackend.dto.request.PaymentRequest;
import com.ecommerce.ecommercebackend.dto.response.PaymentResponse;
import com.ecommerce.ecommercebackend.entity.User;
import com.ecommerce.ecommercebackend.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.net.URI;
import java.util.stream.Collectors;

/**
 * REST controller xử lý tất cả các API liên quan đến thanh toán.
 *
 * <p>Danh sách endpoint:
 * <ul>
 *   <li>POST /api/payment/create            — Tạo link thanh toán (cần JWT)</li>
 *   <li>GET  /api/payment/{id}              — Xem trạng thái giao dịch (cần JWT)</li>
 *   <li>GET  /api/payment/vnpay/callback    — VNPAY redirect về sau thanh toán (public)</li>
 *   <li>POST /api/payment/vnpay/ipn         — VNPAY IPN server-to-server (public)</li>
 *   <li>GET  /api/payment/momo/callback     — MoMo redirect về sau thanh toán (public)</li>
 *   <li>POST /api/payment/momo/ipn          — MoMo IPN server-to-server (public)</li>
 * </ul>
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    // ── Tạo link thanh toán ───────────────────────────────────────────────────

    /**
     * Tạo link thanh toán (VNPAY hoặc MoMo).
     * Yêu cầu JWT token.
     *
     * @return PaymentResponse chứa {@code paymentUrl} để chuyển hướng người dùng
     */
    @PostMapping("/create")
    public ResponseEntity<PaymentResponse> createPayment(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody PaymentRequest request,
            HttpServletRequest httpRequest) {

        String ipAddress = getClientIp(httpRequest);
        PaymentResponse response = paymentService.createPayment(user, request, ipAddress);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ── Xem trạng thái giao dịch ─────────────────────────────────────────────

    /**
     * Lấy thông tin và trạng thái của một giao dịch thanh toán theo ID.
     * Yêu cầu JWT token.
     */
    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getPayment(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getById(user, id));
    }

    @PostMapping("/{id}/retry")
    public ResponseEntity<PaymentResponse> retryPayment(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.retry(user, id, getClientIp(httpRequest)));
    }

    // ── VNPAY Callback (redirect từ trình duyệt) ─────────────────────────────

    /**
     * VNPAY redirect người dùng về đây sau khi thanh toán.
     * Endpoint public — không cần JWT.
     *
     * <p>Dùng để hiển thị kết quả cho người dùng.
     * KHÔNG nên dùng để cập nhật trạng thái đơn hàng (dùng IPN thay thế).</p>
     */
    @GetMapping("/vnpay/callback")
    public ResponseEntity<Void> vnpayCallback(
            @RequestParam Map<String, String> params) {

        log.info("[VNPAY CALLBACK] Nhận callback: vnp_ResponseCode={}",
                params.get("vnp_ResponseCode"));

        Long paymentId = paymentService.handleVnpayReturn(params);
        return redirectToFrontend(paymentId);
    }

    // ── VNPAY IPN (server-to-server) ─────────────────────────────────────────

    /**
     * VNPAY gọi endpoint này để xác nhận kết quả thanh toán (server-to-server).
     * Endpoint public — không cần JWT vì VNPAY server gọi trực tiếp.
     *
     * <p>Đây là kênh đáng tin cậy nhất để cập nhật trạng thái đơn hàng.</p>
     */
    @RequestMapping(value = "/vnpay/ipn", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<Map<String, String>> vnpayIpn(
            @RequestParam Map<String, String> params) {

        log.info("[VNPAY IPN] Nhận IPN: vnp_TxnRef={}, vnp_ResponseCode={}",
                params.get("vnp_TxnRef"), params.get("vnp_ResponseCode"));

        PaymentService.IpnResult result = paymentService.handleVnpayIpn(params);
        Map<String, String> response = new HashMap<>();
        response.put("RspCode", result.code());
        response.put("Message", result.message());
        return ResponseEntity.ok(response);
    }

    // ── MoMo Callback (redirect từ trình duyệt) ──────────────────────────────

    /**
     * MoMo redirect người dùng về đây sau khi thanh toán.
     * Endpoint public — không cần JWT.
     */
    @GetMapping("/momo/callback")
    public ResponseEntity<Void> momoCallback(
            @RequestParam Map<String, String> params) {

        log.info("[MOMO CALLBACK] Nhận callback: resultCode={}",
                params.get("resultCode"));

        Long paymentId = paymentService.handleMomoReturn(params);
        return redirectToFrontend(paymentId);
    }

    // ── MoMo IPN (server-to-server) ──────────────────────────────────────────

    /**
     * MoMo gọi endpoint này (notify_url) để xác nhận kết quả thanh toán.
     * Endpoint public — không cần JWT vì MoMo server gọi trực tiếp.
     */
    @PostMapping("/momo/ipn")
    public ResponseEntity<Void> momoIpn(
            @RequestBody Map<String, Object> body) {

        Map<String, String> params = body.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().toString()));

        log.info("[MOMO IPN] Nhận IPN: orderId={}, resultCode={}",
                params.get("orderId"), params.get("resultCode"));

        return paymentService.handleMomoIpn(params)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.badRequest().build();
    }

    // ── Utility ──────────────────────────────────────────────────────────────

    /** Lấy địa chỉ IP thực của client, xử lý trường hợp đằng sau proxy/load balancer. */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // X-Forwarded-For có thể chứa nhiều IP (client, proxy1, proxy2, ...)
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip != null ? ip : "127.0.0.1";
    }

    private ResponseEntity<Void> redirectToFrontend(Long paymentId) {
        String base = frontendUrl.endsWith("/")
                ? frontendUrl.substring(0, frontendUrl.length() - 1)
                : frontendUrl;
        String target = paymentId == null
                ? base + "/checkout/success?error=payment_not_found"
                : base + "/checkout/success?paymentId=" + paymentId;
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, URI.create(target).toString())
                .build();
    }
}
