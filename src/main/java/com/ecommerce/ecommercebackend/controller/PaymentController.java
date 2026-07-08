package com.ecommerce.ecommercebackend.controller;

import com.ecommerce.ecommercebackend.dto.request.PaymentRequest;
import com.ecommerce.ecommercebackend.dto.response.PaymentResponse;
import com.ecommerce.ecommercebackend.entity.User;
import com.ecommerce.ecommercebackend.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

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

    /**
     * Xac nhan chuyen khoan gia lap cho payment VNPAY/MoMo trong moi truong demo.
     */
    @PostMapping("/{id}/simulate-bank-transfer")
    public ResponseEntity<PaymentResponse> simulateBankTransfer(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        return ResponseEntity.ok(paymentService.simulateBankTransfer(user, id));
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
    public ResponseEntity<Map<String, Object>> vnpayCallback(
            @RequestParam Map<String, String> params) {

        log.info("[VNPAY CALLBACK] Nhận callback: vnp_ResponseCode={}",
                params.get("vnp_ResponseCode"));

        String responseCode = params.getOrDefault("vnp_ResponseCode", "");
        boolean success = "00".equals(responseCode);

        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("message", success ? "Thanh toán thành công" : "Thanh toán thất bại hoặc bị huỷ");
        result.put("orderId", params.get("vnp_TxnRef"));
        result.put("amount",  params.get("vnp_Amount"));

        return ResponseEntity.ok(result);
    }

    // ── VNPAY IPN (server-to-server) ─────────────────────────────────────────

    /**
     * VNPAY gọi endpoint này để xác nhận kết quả thanh toán (server-to-server).
     * Endpoint public — không cần JWT vì VNPAY server gọi trực tiếp.
     *
     * <p>Đây là kênh đáng tin cậy nhất để cập nhật trạng thái đơn hàng.</p>
     */
    @PostMapping("/vnpay/ipn")
    public ResponseEntity<Map<String, String>> vnpayIpn(
            @RequestParam Map<String, String> params) {

        log.info("[VNPAY IPN] Nhận IPN: vnp_TxnRef={}, vnp_ResponseCode={}",
                params.get("vnp_TxnRef"), params.get("vnp_ResponseCode"));

        paymentService.handleVnpayIpn(params);

        // VNPAY yêu cầu backend trả về đúng format này
        Map<String, String> response = new HashMap<>();
        response.put("RspCode", "00");
        response.put("Message", "Confirm Success");
        return ResponseEntity.ok(response);
    }

    // ── MoMo Callback (redirect từ trình duyệt) ──────────────────────────────

    /**
     * MoMo redirect người dùng về đây sau khi thanh toán.
     * Endpoint public — không cần JWT.
     */
    @GetMapping("/momo/callback")
    public ResponseEntity<Map<String, Object>> momoCallback(
            @RequestParam Map<String, String> params) {

        log.info("[MOMO CALLBACK] Nhận callback: resultCode={}",
                params.get("resultCode"));

        String resultCode = params.getOrDefault("resultCode", "-1");
        boolean success = "0".equals(resultCode);

        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("message", success ? "Thanh toán thành công" : "Thanh toán thất bại hoặc bị huỷ");
        result.put("orderId", params.get("orderId"));
        result.put("amount",  params.get("amount"));

        return ResponseEntity.ok(result);
    }

    // ── MoMo IPN (server-to-server) ──────────────────────────────────────────

    /**
     * MoMo gọi endpoint này (notify_url) để xác nhận kết quả thanh toán.
     * Endpoint public — không cần JWT vì MoMo server gọi trực tiếp.
     */
    @PostMapping("/momo/ipn")
    public ResponseEntity<Void> momoIpn(
            @RequestBody Map<String, String> params) {

        log.info("[MOMO IPN] Nhận IPN: orderId={}, resultCode={}",
                params.get("orderId"), params.get("resultCode"));

        paymentService.handleMomoIpn(params);
        return ResponseEntity.noContent().build();
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
}
