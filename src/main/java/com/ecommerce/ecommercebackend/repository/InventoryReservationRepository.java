package com.ecommerce.ecommercebackend.repository;

import com.ecommerce.ecommercebackend.entity.InventoryReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * B07 – Repository cho {@link InventoryReservation}.
 */
@Repository
public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, Long> {

    /** Tất cả reservation đang PENDING của một user. */
    List<InventoryReservation> findByUserIdAndStatus(
            Long userId, InventoryReservation.ReservationStatus status);

    /** Các reservation đã hết hạn và vẫn PENDING — dùng để cleanup. */
    @Query("""
            SELECT r FROM InventoryReservation r
            WHERE r.status = 'PENDING'
              AND r.expiresAt < :now
            """)
    List<InventoryReservation> findExpiredPending(@Param("now") LocalDateTime now);

    /** Xoá tất cả reservation đã được xử lý (CONFIRMED/RELEASED) quá 24h. */
    @Modifying
    @Query("""
            DELETE FROM InventoryReservation r
            WHERE r.status <> 'PENDING'
              AND r.createdAt < :cutoff
            """)
    int deleteOldProcessed(@Param("cutoff") LocalDateTime cutoff);
}
