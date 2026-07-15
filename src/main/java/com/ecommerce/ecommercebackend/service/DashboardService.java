package com.ecommerce.ecommercebackend.service;

import com.ecommerce.ecommercebackend.entity.OrderStatus;
import com.ecommerce.ecommercebackend.repository.OrderRepository;
import com.ecommerce.ecommercebackend.repository.ProductRepository;
import com.ecommerce.ecommercebackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * H02 – Service thống kê dashboard nâng cao cho admin.
 *
 * <p>Cung cấp dữ liệu thống kê theo khoảng thời gian:
 * <ul>
 *   <li>Doanh thu hôm nay / 7 ngày / 30 ngày</li>
 *   <li>Số đơn hàng mới / đã thanh toán / đã hủy theo kỳ</li>
 *   <li>Số người dùng mới</li>
 *   <li>Top sản phẩm bán chạy</li>
 *   <li>So sánh doanh thu kỳ này vs kỳ trước (%)</li>
 * </ul>
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final OrderRepository   orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository    userRepository;

    // ── DTOs ─────────────────────────────────────────────────────────────────

    /** Thống kê doanh thu theo một kỳ. */
    public record PeriodRevenueStats(
            long   revenue,
            long   prevRevenue,
            double growthPercent,    // % so với kỳ trước (âm = giảm)
            long   newOrders,
            long   paidOrders,
            long   cancelledOrders
    ) {}

    /** Thống kê tổng quan dashboard. */
    public record DashboardOverview(
            // Hôm nay
            PeriodRevenueStats today,
            // 7 ngày gần nhất
            PeriodRevenueStats last7Days,
            // 30 ngày gần nhất
            PeriodRevenueStats last30Days,
            // Tổng all-time
            long totalRevenue,
            long totalOrders,
            long totalUsers,
            long totalProducts,
            // Top sản phẩm bán chạy trong 30 ngày
            List<TopProductStats> topProducts
    ) {}

    /** Một sản phẩm trong top bán chạy. */
    public record TopProductStats(
            Long   productId,
            String productName,
            long   totalQuantitySold,
            long   totalRevenue
    ) {}

    // ── Main API ──────────────────────────────────────────────────────────────

    /**
     * H02 – Dashboard tổng quan nâng cao.
     */
    @Transactional(readOnly = true)
    public DashboardOverview getOverview() {
        LocalDateTime now = LocalDateTime.now();

        // Hôm nay
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd   = LocalDate.now().atTime(LocalTime.MAX);

        // 7 ngày
        LocalDateTime last7Start = now.minusDays(7);
        LocalDateTime prev7Start = now.minusDays(14);
        LocalDateTime prev7End   = now.minusDays(7);

        // 30 ngày
        LocalDateTime last30Start = now.minusDays(30);
        LocalDateTime prev30Start = now.minusDays(60);
        LocalDateTime prev30End   = now.minusDays(30);

        // Tổng all-time
        long totalRevenue  = orderRepository.totalRevenue();
        long totalOrders   = orderRepository.count();
        long totalUsers    = userRepository.count();
        long totalProducts = productRepository.count();

        // Top 10 sản phẩm bán chạy 30 ngày
        List<TopProductStats> topProducts = buildTopProducts(last30Start, now, 10);

        DashboardOverview overview = new DashboardOverview(
                buildPeriodStats(todayStart, todayEnd, now.minusDays(1).with(LocalTime.MIN), todayStart),
                buildPeriodStats(last7Start, now, prev7Start, prev7End),
                buildPeriodStats(last30Start, now, prev30Start, prev30End),
                totalRevenue, totalOrders, totalUsers, totalProducts,
                topProducts
        );

        log.info("[H02] Dashboard loaded: revenue={}, orders={}, users={}, products={}",
                totalRevenue, totalOrders, totalUsers, totalProducts);
        return overview;
    }

    /**
     * H02 – Doanh thu theo khoảng tuỳ chỉnh.
     *
     * @param from Bắt đầu (ISO LocalDateTime)
     * @param to   Kết thúc (ISO LocalDateTime)
     * @return Tổng doanh thu PAID/COMPLETED trong khoảng đó
     */
    @Transactional(readOnly = true)
    public long getRevenueByCustomPeriod(LocalDateTime from, LocalDateTime to) {
        return orderRepository.revenueByPeriod(from, to);
    }

    /**
     * H02 – Top sản phẩm bán chạy theo khoảng tuỳ chỉnh.
     */
    @Transactional(readOnly = true)
    public List<TopProductStats> getTopProducts(LocalDateTime from, LocalDateTime to, int limit) {
        return buildTopProducts(from, to, Math.min(limit, 50));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private PeriodRevenueStats buildPeriodStats(
            LocalDateTime from, LocalDateTime to,
            LocalDateTime prevFrom, LocalDateTime prevTo) {

        long revenue      = orderRepository.revenueByPeriod(from, to);
        long prevRevenue  = orderRepository.revenueByPeriod(prevFrom, prevTo);
        long newOrders    = orderRepository.countByPeriod(from, to);
        long paidOrders   = orderRepository.countByStatusAndPeriod(OrderStatus.PAID, from, to);
        long cancelled    = orderRepository.countByStatusAndPeriod(OrderStatus.CANCELLED, from, to);

        double growth = prevRevenue == 0 ? 0.0
                : Math.round(((revenue - prevRevenue) * 100.0 / prevRevenue) * 10) / 10.0;

        return new PeriodRevenueStats(revenue, prevRevenue, growth, newOrders, paidOrders, cancelled);
    }

    private List<TopProductStats> buildTopProducts(LocalDateTime from, LocalDateTime to, int limit) {
        return orderRepository.findTopSellingProducts(from, to, limit).stream()
                .map(row -> new TopProductStats(
                        row[0] instanceof Long   l ? l : ((Number) row[0]).longValue(),
                        (String) row[1],
                        ((Number) row[2]).longValue(),
                        ((Number) row[3]).longValue()
                ))
                .toList();
    }
}
