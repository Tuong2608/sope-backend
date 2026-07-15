package com.ecommerce.ecommercebackend.repository;

import com.ecommerce.ecommercebackend.entity.Order;
import com.ecommerce.ecommercebackend.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Order} persistence.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /** A user's orders, newest first (order history). */
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    /** A single order scoped to its owner (prevents cross-user access). */
    Optional<Order> findByIdAndUserId(Long id, Long userId);

    /** Lookup by business key scoped to its owner. */
    Optional<Order> findByOrderCodeAndUserId(String orderCode, Long userId);

    /** Lookup by the business key shared with the payment module. */
    Optional<Order> findByOrderCode(String orderCode);

    boolean existsByOrderCode(String orderCode);

    // ── Admin / review support ──────────────────────────────────────────────────

    List<Order> findAllByOrderByCreatedAtDesc();

    List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status);

    /** True if the user has a non-cancelled order containing the product (= "đã mua"). */
    @Query("""
            SELECT COUNT(o) > 0 FROM Order o JOIN o.items i
            WHERE o.user.id = :userId AND i.productId = :productId
              AND o.status <> com.ecommerce.ecommercebackend.entity.OrderStatus.CANCELLED
            """)
    boolean hasPurchased(@Param("userId") Long userId, @Param("productId") Long productId);

    /** Total revenue from paid/completed orders (0 when none). */
    @Query("""
            SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o
            WHERE o.status IN (com.ecommerce.ecommercebackend.entity.OrderStatus.PAID,
                               com.ecommerce.ecommercebackend.entity.OrderStatus.COMPLETED)
            """)
    long totalRevenue();

    // ── H02: Dashboard ─────────────────────────────────────────────────────

    /** Doanh thu PAID/COMPLETED trong khoảng thời gian. */
    @Query("""
            SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o
            WHERE o.status IN (com.ecommerce.ecommercebackend.entity.OrderStatus.PAID,
                               com.ecommerce.ecommercebackend.entity.OrderStatus.COMPLETED)
              AND o.createdAt >= :from AND o.createdAt <= :to
            """)
    long revenueByPeriod(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** Số đơn hàng theo trạng thái trong khoảng thời gian. */
    @Query("""
            SELECT COUNT(o) FROM Order o
            WHERE o.status = :status
              AND o.createdAt >= :from AND o.createdAt <= :to
            """)
    long countByStatusAndPeriod(
            @Param("status") OrderStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    /** Số đơn hàng mới trong khoảng thời gian (mọi trạng thái). */
    @Query("""
            SELECT COUNT(o) FROM Order o
            WHERE o.createdAt >= :from AND o.createdAt <= :to
            """)
    long countByPeriod(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** Top sản phẩm bán chạy: [productId, productName, totalQty, totalRevenue]. */
    @Query("""
            SELECT i.productId, i.productName,
                   SUM(i.quantity)  AS totalQty,
                   SUM(i.lineTotal) AS totalRevenue
            FROM Order o JOIN o.items i
            WHERE o.status IN (com.ecommerce.ecommercebackend.entity.OrderStatus.PAID,
                               com.ecommerce.ecommercebackend.entity.OrderStatus.COMPLETED)
              AND o.createdAt >= :from AND o.createdAt <= :to
            GROUP BY i.productId, i.productName
            ORDER BY totalQty DESC
            LIMIT :limit
            """)
    List<Object[]> findTopSellingProducts(
            @Param("from") LocalDateTime from,
            @Param("to")   LocalDateTime to,
            @Param("limit") int limit);
}
