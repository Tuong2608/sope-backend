package com.ecommerce.ecommercebackend.repository;

import com.ecommerce.ecommercebackend.entity.ShipmentTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link ShipmentTracking} persistence.
 */
@Repository
public interface ShipmentTrackingRepository extends JpaRepository<ShipmentTracking, Long> {

    Optional<ShipmentTracking> findByOrderId(Long orderId);

    boolean existsByTrackingNumber(String trackingNumber);

    Optional<ShipmentTracking> findByTrackingNumber(String trackingNumber);
}
