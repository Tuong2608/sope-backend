package com.ecommerce.ecommercebackend.repository;

import com.ecommerce.ecommercebackend.entity.Payment;
import com.ecommerce.ecommercebackend.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /** Tìm giao dịch thanh toán theo mã đơn hàng. */
    Optional<Payment> findByOrderId(String orderId);

    Optional<Payment> findByProviderOrderId(String providerOrderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Payment> findLockedByProviderOrderId(String providerOrderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Payment> findLockedById(Long id);

    Optional<Payment> findFirstByOrderIdOrderByCreatedAtDesc(String orderId);

    Optional<Payment> findFirstByOrderIdAndStatusOrderByCreatedAtDesc(
            String orderId,
            PaymentStatus status
    );

    Optional<Payment> findFirstByOrderIdAndProviderAndStatusInOrderByCreatedAtDesc(
            String orderId,
            com.ecommerce.ecommercebackend.entity.PaymentProvider provider,
            java.util.Collection<PaymentStatus> statuses
    );

    List<Payment> findAllByOrderByCreatedAtDesc();

    List<Payment> findByStatusOrderByCreatedAtDesc(PaymentStatus status);
}
