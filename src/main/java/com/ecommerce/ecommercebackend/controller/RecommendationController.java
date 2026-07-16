package com.ecommerce.ecommercebackend.controller;

import com.ecommerce.ecommercebackend.dto.response.ProductResponse;
import com.ecommerce.ecommercebackend.service.ProductService;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.List;

/**
 * Cầu nối giữa frontend và service AI viết bằng Python (FastAPI) để lấy
 * gợi ý sản phẩm.
 *
 * <p>Luồng chạy: Next.js gọi controller này → controller gọi sang FastAPI
 * (nơi tính cosine similarity, xem recommendation.py) → danh sách
 * product_id trả về được "dịch" lại thành {@link ProductResponse} đầy đủ
 * thông qua {@link ProductService}, vì UI cần tên/giá/ảnh chứ không phải
 * chỉ mỗi con số ID.</p>
 *
 * <p>Mọi tình huống lỗi (Python không phản hồi, sản phẩm chưa có trong bảng
 * ratings, sản phẩm đã bị xoá,...) đều rơi về danh sách rỗng thay vì ném
 * lỗi, để {@code SimilarProducts} ở frontend tự ẩn đi thay vì crash — cùng
 * tinh thần phòng thủ như bản đề xuất ban đầu, chỉ khác là có kiểu dữ liệu
 * rõ ràng thay vì trả thẳng String.</p>
 *
 * <p><b>Lưu ý:</b> nếu project có {@code SecurityConfig}, nhớ cho phép GET
 * tới {@code /api/recommendations/**} (giống cách đang permitAll cho
 * {@code /api/products/**}), nếu không request vẫn bị chặn trước khi tới
 * được controller này.</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RecommendationController {

    private final ProductService productService;
    private final RestTemplate restTemplate;

    @Value("${app.chatbot.url:http://localhost:8000}")
    private String chatbotUrl;

    // ── Gợi ý Collaborative Filtering cũ (Giữ nguyên nếu muốn) ──
    @GetMapping("/similar/{productId}")
    public ResponseEntity<List<ProductResponse>> getSimilarProducts(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "5") int topN) {
        List<Long> similarIds = fetchSimilarIdsFromPython(productId, topN,
                chatbotBaseUrl() + "/api/ai/recommend/similar/{productId}?top_n={topN}");
        return ResponseEntity.ok(resolveProducts(similarIds));
    }

    // ── API MỚI: Gợi ý Content-Based Filtering dựa trên Specs ──
    @GetMapping("/content-similar/{productId}")
    public ResponseEntity<List<ProductResponse>> getContentBasedSimilarProducts(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "5") int topN) {
        
        // Gọi sang endpoint CBF của Python
        List<Long> similarIds = fetchSimilarIdsFromPython(productId, topN,
                chatbotBaseUrl() + "/api/ai/recommend/content-based/{productId}?top_n={topN}");
        return ResponseEntity.ok(resolveProducts(similarIds));
    }

    // ── Hàm bóc tách JSON dùng chung (Đã điều chỉnh tham số URL) ────────
    private List<Long> fetchSimilarIdsFromPython(Long productId, int topN, String urlTemplate) {
        try {
            PythonRecommendResponse response = restTemplate.getForObject(
                    urlTemplate, PythonRecommendResponse.class, productId, topN);
            return (response != null && response.productIds != null)
                    ? response.productIds
                    : List.of();
        } catch (RestClientException e) {
            log.warn("Không gọi được Python AI service cho productId={}: {}", productId, e.getMessage());
            return List.of();
        }
    }

    // ── ID -> ProductResponse đầy đủ; ID nào lỗi thì bỏ qua, không crash ────

    private List<ProductResponse> resolveProducts(List<Long> ids) {
        List<ProductResponse> result = new ArrayList<>();
        for (Long id : ids) {
            try {
                result.add(productService.getById(id));
            } catch (Exception e) {
                log.warn("Bỏ qua productId={} khi dựng danh sách gợi ý: {}", id, e.getMessage());
            }
        }
        return result;
    }

    private String chatbotBaseUrl() {
        return chatbotUrl.replaceAll("/+$", "");
    }

    /** Khớp cấu trúc JSON của endpoint FastAPI /api/ai/recommend/similar/{id}. */
    private static class PythonRecommendResponse {
        public String status;
        @JsonProperty("product_ids")
        public List<Long> productIds;
    }
}
