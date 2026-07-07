package com.ecommerce.ecommercebackend.repository;

import com.ecommerce.ecommercebackend.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}
