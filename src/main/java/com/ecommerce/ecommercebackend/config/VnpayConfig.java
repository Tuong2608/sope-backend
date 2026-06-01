package com.ecommerce.ecommercebackend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình cho VNPAY Sandbox.
 * Đọc các giá trị từ application.properties với prefix "vnpay".
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "vnpay")
public class VnpayConfig {

    /** Terminal code (TMN Code) do VNPAY cấp. */
    private String tmnCode;

    /** Secret key để tạo chữ ký HMAC-SHA512. */
    private String hashSecret;

    /** URL trang thanh toán VNPAY (sandbox hoặc production). */
    private String url;

    /** URL backend nhận redirect từ VNPAY sau khi thanh toán. */
    private String returnUrl;

    /** URL backend nhận IPN (Instant Payment Notification) từ VNPAY. */
    private String ipnUrl;
}
