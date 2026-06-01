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
    private String returnUrl;

    /** URL backend nhận IPN (notify_url) từ MoMo. */
    private String notifyUrl;
}
