package com.ecommerce.ecommercebackend.repository;

import com.ecommerce.ecommercebackend.entity.Payment;
import com.ecommerce.ecommercebackend.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /** Tìm giao dịch thanh toán theo mã đơn hàng. */
    Optional<Payment> findByOrderId(String orderId);

    Optional<Payment> findFirstByOrderIdOrderByCreatedAtDesc(String orderId);

    Optional<Payment> findFirstByOrderIdAndStatusOrderByCreatedAtDesc(
            String orderId,
            PaymentStatus status
    );
}
