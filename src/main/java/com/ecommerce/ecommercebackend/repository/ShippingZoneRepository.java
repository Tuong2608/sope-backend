package com.ecommerce.ecommercebackend.repository;

import com.ecommerce.ecommercebackend.entity.ShippingZone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link ShippingZone} persistence.
 */
@Repository
public interface ShippingZoneRepository extends JpaRepository<ShippingZone, Long> {

    List<ShippingZone> findByActiveTrueOrderByPriorityAsc();
}
