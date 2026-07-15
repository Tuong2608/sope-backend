package com.ecommerce.ecommercebackend.repository;

import com.ecommerce.ecommercebackend.entity.Coupon;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Coupon} persistence.
 */
@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {

    Optional<Coupon> findByCode(String code);

    boolean existsByCode(String code);

    /**
     * Task D06 — locks the coupon row ({@code SELECT ... FOR UPDATE}) for the
     * duration of the caller's transaction. Used only when actually placing an
     * order (holding a redemption slot); concurrent checkouts against the same
     * coupon serialize here instead of racing past its usage limit.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Coupon c WHERE c.code = :code")
    Optional<Coupon> findByCodeForUpdate(String code);
}
