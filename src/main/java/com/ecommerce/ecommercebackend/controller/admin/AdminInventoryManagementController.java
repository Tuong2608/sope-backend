package com.ecommerce.ecommercebackend.controller.admin;

import com.ecommerce.ecommercebackend.entity.ProductStatus;
import com.ecommerce.ecommercebackend.service.AdminInventoryManagementService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * H09 – Admin controller quản lý tồn kho thủ công.
 *
 * <ul>
 *   <li>POST /api/admin/inventory/restock/{productId}          – nhập hàng cho sản phẩm</li>
 *   <li>POST /api/admin/inventory/restock/variant/{variantId}  – nhập hàng cho variant</li>
 *   <li>POST /api/admin/inventory/bulk-restock                 – nhập hàng hàng loạt</li>
 *   <li>PUT  /api/admin/inventory/set-stock/{productId}        – đặt số lượng tuyệt đối</li>
 *   <li>PUT  /api/admin/inventory/status/{productId}           – đổi trạng thái SP</li>
 *   <li>PUT  /api/admin/inventory/min-stock/{productId}        – đặt mức tồn tối thiểu</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/admin/inventory")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminInventoryManagementController {

    private final AdminInventoryManagementService inventoryManagementService;

    // ── Request bodies ────────────────────────────────────────────────────────

    @Data
    public static class RestockBody {
        @NotNull @Min(1)
        private Integer quantity;
    }

    @Data
    public static class SetStockBody {
        @NotNull @Min(0)
        private Integer quantity;
    }

    @Data
    public static class StatusBody {
        @NotNull
        private ProductStatus status;
    }

    @Data
    public static class MinStockBody {
        @NotNull @Min(0)
        private Integer minStockLevel;
    }

    @Data
    public static class BulkRestockItem {
        @NotNull private Long productId;
        private Long variantId;
        @NotNull @Min(1) private Integer quantity;
    }

    // ── Endpoints ─────────────────────────────────────────────────────────────

    /**
     * H09 – Nhập thêm hàng cho một sản phẩm.
     *
     * <p>POST /api/admin/inventory/restock/{productId}<br>
     * Body: {@code { "quantity": 50 }}</p>
     */
    @PostMapping("/restock/{productId}")
    public ResponseEntity<AdminInventoryManagementService.StockUpdateResult> restock(
            @PathVariable Long productId,
            @Valid @RequestBody RestockBody body) {
        return ResponseEntity.ok(inventoryManagementService.restockProduct(productId, body.getQuantity()));
    }

    /**
     * H09 – Nhập thêm hàng cho một variant.
     */
    @PostMapping("/restock/variant/{variantId}")
    public ResponseEntity<AdminInventoryManagementService.StockUpdateResult> restockVariant(
            @PathVariable Long variantId,
            @Valid @RequestBody RestockBody body) {
        return ResponseEntity.ok(inventoryManagementService.restockVariant(variantId, body.getQuantity()));
    }

    /**
     * H09 – Bulk restock nhiều sản phẩm cùng lúc.
     *
     * <p>POST /api/admin/inventory/bulk-restock<br>
     * Body: {@code [{ "productId": 1, "quantity": 50 }, { "productId": 2, "variantId": 5, "quantity": 20 }]}</p>
     */
    @PostMapping("/bulk-restock")
    public ResponseEntity<List<AdminInventoryManagementService.StockUpdateResult>> bulkRestock(
            @Valid @RequestBody List<BulkRestockItem> items) {

        List<AdminInventoryManagementService.RestockRequest> requests = items.stream()
                .map(i -> new AdminInventoryManagementService.RestockRequest(
                        i.getProductId(), i.getVariantId(), i.getQuantity()))
                .toList();

        return ResponseEntity.ok(inventoryManagementService.bulkRestock(requests));
    }

    /**
     * H09 – Đặt số lượng tồn kho tuyệt đối (ghi đè).
     *
     * <p>PUT /api/admin/inventory/set-stock/{productId}<br>
     * Body: {@code { "quantity": 100 }}</p>
     */
    @PutMapping("/set-stock/{productId}")
    public ResponseEntity<AdminInventoryManagementService.StockUpdateResult> setStock(
            @PathVariable Long productId,
            @Valid @RequestBody SetStockBody body) {
        return ResponseEntity.ok(inventoryManagementService.setStock(productId, body.getQuantity()));
    }

    /**
     * H09 – Thay đổi trạng thái sản phẩm.
     *
     * <p>PUT /api/admin/inventory/status/{productId}<br>
     * Body: {@code { "status": "INACTIVE" }}</p>
     */
    @PutMapping("/status/{productId}")
    public ResponseEntity<AdminInventoryManagementService.StockUpdateResult> updateStatus(
            @PathVariable Long productId,
            @Valid @RequestBody StatusBody body) {
        return ResponseEntity.ok(inventoryManagementService.updateStatus(productId, body.getStatus()));
    }

    /**
     * H09 – Cập nhật mức tồn tối thiểu (cảnh báo low stock).
     *
     * <p>PUT /api/admin/inventory/min-stock/{productId}<br>
     * Body: {@code { "minStockLevel": 5 }}</p>
     */
    @PutMapping("/min-stock/{productId}")
    public ResponseEntity<AdminInventoryManagementService.StockUpdateResult> updateMinStock(
            @PathVariable Long productId,
            @Valid @RequestBody MinStockBody body) {
        return ResponseEntity.ok(inventoryManagementService.updateMinStockLevel(productId, body.getMinStockLevel()));
    }
}
