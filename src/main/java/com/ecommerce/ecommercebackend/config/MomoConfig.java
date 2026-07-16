package com.ecommerce.ecommercebackend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình cho MoMo Sandbox.
 * Đọc các giá trị từ application.properties với prefix "momo".
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "momo")
public class MomoConfig {

    /** Partner code do MoMo cấp. */
    private String partnerCode;

    /** Access key do MoMo cấp (dùng trong chữ ký). */
    private String accessKey;

    /** Secret key để tạo chữ ký HMAC-SHA256. */
    private String secretKey;

    /** URL API tạo giao dịch của MoMo (sandbox hoặc production). */
    private String endpoint;

    /** URL backend nhận redirect từ MoMo sau khi thanh toán. */
    private String redirectUrl;

    /** URL backend nhận IPN từ MoMo. */
    private String ipnUrl;

    /** Loại request đúng với merchant contract, mặc định captureWallet. */
    private String requestType = "captureWallet";

    private String queryEndpoint;
    private String refundEndpoint;
}
