package com.ecommerce.ecommercebackend.controller.admin;

import com.ecommerce.ecommercebackend.service.ProductDataValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * A04 – Admin endpoint để kích hoạt kiểm tra dữ liệu sản phẩm.
 *
 * <p>GET /api/admin/products/validate — chạy toàn bộ kiểm tra và trả về báo cáo lỗi.</p>
 */
@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminProductValidationController {

    private final ProductDataValidationService validationService;

    /**
     * Chạy kiểm tra dữ liệu sản phẩm và trả về báo cáo.
     *
     * <p>Response ví dụ:
     * <pre>
     * {
     *   "totalProducts": 150,
     *   "duplicateSkus": ["363417", "363418"],
     *   "missingPriceIds": [5, 12],
     *   "missingImageIds": [7],
     *   "missingBrandIds": [],
     *   "invalidCategoryIds": [99],
     *   "hasErrors": true,
     *   "errorCount": 6
     * }
     * </pre>
     * </p>
     */
    @GetMapping("/validate")
    public ResponseEntity<ProductDataValidationService.ValidationReport> validate() {
        return ResponseEntity.ok(validationService.validate());
    }
}
