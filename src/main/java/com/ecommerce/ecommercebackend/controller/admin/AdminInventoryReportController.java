package com.ecommerce.ecommercebackend.controller.admin;

import com.ecommerce.ecommercebackend.service.InventoryReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * E07 – Admin controller báo cáo tồn kho.
 *
 * <ul>
 *   <li>GET /api/admin/inventory/overview    – tổng quan tồn kho</li>
 *   <li>GET /api/admin/inventory/low-stock   – sản phẩm sắp hết hàng</li>
 *   <li>GET /api/admin/inventory/out-of-stock – sản phẩm đã hết hàng</li>
 *   <li>GET /api/admin/inventory/inactive    – sản phẩm ngừng bán</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/admin/inventory")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminInventoryReportController {

    private final InventoryReportService inventoryReportService;

    /**
     * E07 – Tổng quan tồn kho.
     *
     * <p>Response:
     * <pre>
     * {
     *   "totalProducts": 150,
     *   "activeProducts": 130,
     *   "inStockProducts": 110,
     *   "outOfStockProducts": 20,
     *   "lowStockProducts": 15,
     *   "inactiveProducts": 20
     * }
     * </pre>
     * </p>
     */
    @GetMapping("/overview")
    public ResponseEntity<InventoryReportService.InventoryOverview> getOverview() {
        return ResponseEntity.ok(inventoryReportService.getOverview());
    }

    /**
     * E07 – Danh sách sản phẩm sắp hết hàng (availableQty &lt;= minStockLevel).
     * Sắp xếp từ ít hàng nhất.
     */
    @GetMapping("/low-stock")
    public ResponseEntity<List<InventoryReportService.InventoryItemReport>> getLowStock() {
        return ResponseEntity.ok(inventoryReportService.getLowStockProducts());
    }

    /**
     * E07 – Danh sách sản phẩm đã hết hàng hoàn toàn.
     */
    @GetMapping("/out-of-stock")
    public ResponseEntity<List<InventoryReportService.InventoryItemReport>> getOutOfStock() {
        return ResponseEntity.ok(inventoryReportService.getOutOfStockProducts());
    }

    /**
     * E07 – Danh sách sản phẩm đang ngừng bán.
     */
    @GetMapping("/inactive")
    public ResponseEntity<List<InventoryReportService.InventoryItemReport>> getInactive() {
        return ResponseEntity.ok(inventoryReportService.getInactiveProducts());
    }
}
