package com.ecommerce.ecommercebackend.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * B10 – Utility xử lý ảnh sản phẩm, chuẩn hóa URL và fallback về placeholder.
 *
 * <p>Đảm bảo mọi sản phẩm/variant đều có ảnh hiển thị được — kể cả khi
 * dữ liệu crawl thiếu hoặc URL bị hỏng.</p>
 */
@Slf4j
@Component
public class ProductImageUtil {

    /** Ảnh placeholder mặc định khi không có ảnh. */
    public static final String DEFAULT_PLACEHOLDER   = "/images/placeholder.png";
    public static final String LAPTOP_PLACEHOLDER    = "/images/placeholder-laptop.png";
    public static final String PHONE_PLACEHOLDER     = "/images/placeholder-phone.png";
    public static final String TABLET_PLACEHOLDER    = "/images/placeholder-tablet.png";

    /**
     * B10 – Trả về URL ảnh hợp lệ, fallback về placeholder theo category nếu thiếu.
     *
     * @param imgUrl   URL ảnh gốc (có thể null/blank/sai)
     * @param category Category sản phẩm để chọn placeholder phù hợp
     * @return URL ảnh đã được kiểm tra, không bao giờ null/blank
     */
    public String resolveImageUrl(String imgUrl, String category) {
        if (isValidUrl(imgUrl)) {
            return imgUrl.trim();
        }
        String placeholder = resolvePlaceholder(category);
        log.debug("[B10] Thiếu ảnh category='{}' → dùng placeholder: {}", category, placeholder);
        return placeholder;
    }

    /**
     * B10 – Lấy placeholder phù hợp theo category.
     */
    public String resolvePlaceholder(String category) {
        if (category == null) return DEFAULT_PLACEHOLDER;
        return switch (category.toLowerCase()) {
            case "laptop"                   -> LAPTOP_PLACEHOLDER;
            case "dien-thoai", "smartphone" -> PHONE_PLACEHOLDER;
            case "may-tinh-bang", "tablet"  -> TABLET_PLACEHOLDER;
            default                         -> DEFAULT_PLACEHOLDER;
        };
    }

    /**
     * Kiểm tra URL ảnh có hợp lệ hay không.
     * Hợp lệ: không null, không rỗng, bắt đầu bằng http(s) hoặc /
     */
    public boolean isValidUrl(String url) {
        if (url == null || url.isBlank()) return false;
        String trimmed = url.trim();
        return trimmed.startsWith("http://")
                || trimmed.startsWith("https://")
                || trimmed.startsWith("/");
    }

    /**
     * B10 – Chuẩn hóa danh sách ảnh: loại bỏ URL rỗng/null, deduplicate.
     *
     * @param images     Danh sách URL gốc
     * @param primaryUrl URL ảnh chính (để đặt lên đầu nếu chưa có)
     * @return Danh sách ảnh đã chuẩn hóa
     */
    public java.util.List<String> normalizeImages(
            java.util.List<String> images, String primaryUrl) {

        java.util.LinkedHashSet<String> seen = new java.util.LinkedHashSet<>();

        // Ưu tiên ảnh chính
        if (isValidUrl(primaryUrl)) seen.add(primaryUrl.trim());

        if (images != null) {
            images.stream()
                    .filter(this::isValidUrl)
                    .map(String::trim)
                    .forEach(seen::add);
        }

        return new java.util.ArrayList<>(seen);
    }
}
