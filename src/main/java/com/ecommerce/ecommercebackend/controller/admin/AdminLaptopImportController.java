package com.ecommerce.ecommercebackend.controller.admin;

import com.ecommerce.ecommercebackend.service.LaptopImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * A03 – Admin endpoint nhập dữ liệu laptop thủ công.
 *
 * <ul>
 *   <li>POST /api/admin/products/import/laptop — nhận JSON array và import</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminLaptopImportController {

    private final LaptopImportService laptopImportService;

    /**
     * Import danh sách laptop từ JSON array.
     *
     * <p>Body: JSON array của laptop objects theo schema TGDD crawl.
     * Ví dụ một phần tử:
     * <pre>
     * {
     *   "sku": "363417",
     *   "product_name": "MacBook Air M2",
     *   "category": "laptop",
     *   "brand": "Apple",
     *   "current_price": 27990000,
     *   "original_price": 29990000,
     *   "img_url": "https://..."
     * }
     * </pre>
     * </p>
     */
    @PostMapping("/import/laptop")
    public ResponseEntity<LaptopImportService.ImportResult> importLaptops(
            @RequestBody List<Map<String, Object>> laptopData) {
        LaptopImportService.ImportResult result = laptopImportService.importLaptops(laptopData);
        return ResponseEntity.ok(result);
    }
}
