package com.ecommerce.ecommercebackend.controller.admin;

import com.ecommerce.ecommercebackend.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * H02 – Admin Dashboard controller nâng cao.
 *
 * <ul>
 *   <li>GET /api/admin/dashboard/overview          – tổng quan: doanh thu hôm nay/7/30 ngày, tăng trưởng</li>
 *   <li>GET /api/admin/dashboard/revenue           – doanh thu tuỳ chỉnh theo ngày</li>
 *   <li>GET /api/admin/dashboard/top-products      – top sản phẩm bán chạy tuỳ chỉnh</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final DashboardService dashboardService;

    /**
     * H02 – Dashboard tổng quan.
     *
     * <p>Response ví dụ:
     * <pre>
     * {
     *   "today": {
     *     "revenue": 55980000,
     *     "prevRevenue": 27990000,
     *     "growthPercent": 100.0,
     *     "newOrders": 3,
     *     "paidOrders": 2,
     *     "cancelledOrders": 0
     *   },
     *   "last7Days": { ... },
     *   "last30Days": { ... },
     *   "totalRevenue": 5000000000,
     *   "totalOrders": 150,
     *   "totalUsers": 80,
     *   "totalProducts": 200,
     *   "topProducts": [
     *     { "productId": 1, "productName": "MacBook Air M2", "totalQuantitySold": 25, "totalRevenue": 699750000 }
     *   ]
     * }
     * </pre>
     * </p>
     */
    @GetMapping("/overview")
    public ResponseEntity<DashboardService.DashboardOverview> getOverview() {
        return ResponseEntity.ok(dashboardService.getOverview());
    }

    /**
     * H02 – Doanh thu theo khoảng thời gian tuỳ chỉnh.
     *
     * <p>GET /api/admin/dashboard/revenue?from=2026-07-01T00:00:00&to=2026-07-14T23:59:59</p>
     */
    @GetMapping("/revenue")
    public ResponseEntity<Long> getRevenue(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(dashboardService.getRevenueByCustomPeriod(from, to));
    }

    /**
     * H02 – Top sản phẩm bán chạy theo khoảng thời gian tuỳ chỉnh.
     *
     * <p>GET /api/admin/dashboard/top-products?from=...&to=...&limit=10</p>
     */
    @GetMapping("/top-products")
    public ResponseEntity<List<DashboardService.TopProductStats>> getTopProducts(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(dashboardService.getTopProducts(from, to, limit));
    }
}
