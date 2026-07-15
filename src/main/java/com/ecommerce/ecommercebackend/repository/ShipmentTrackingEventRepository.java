package com.ecommerce.ecommercebackend.repository;

import com.ecommerce.ecommercebackend.entity.ShipmentTrackingEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link ShipmentTrackingEvent} persistence.
 */
@Repository
public interface ShipmentTrackingEventRepository extends JpaRepository<ShipmentTrackingEvent, Long> {

    List<ShipmentTrackingEvent> findByTrackingIdOrderByCreatedAtAsc(Long trackingId);
}
