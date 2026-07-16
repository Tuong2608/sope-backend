package com.ecommerce.ecommercebackend.repository;

import com.ecommerce.ecommercebackend.entity.CouponUsage;
import com.ecommerce.ecommercebackend.entity.CouponUsageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link CouponUsage} — the hold/use/release
 * history that {@code D06} will use to enforce coupon usage limits safely
 * under concurrent checkouts.
 */
@Repository
public interface CouponUsageRepository extends JpaRepository<CouponUsage, Long> {

    Optional<CouponUsage> findByOrderId(Long orderId);

    boolean existsByCouponId(Long couponId);

    List<CouponUsage> findByCouponIdAndStatus(Long couponId, CouponUsageStatus status);

    List<CouponUsage> findByCouponIdAndUserIdAndStatus(
            Long couponId, Long userId, CouponUsageStatus status);

    long countByCouponIdAndUserIdAndStatus(Long couponId, Long userId, CouponUsageStatus status);
}
