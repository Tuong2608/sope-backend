package com.ecommerce.ecommercebackend.repository;

import com.ecommerce.ecommercebackend.entity.ShippingRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link ShippingRate} persistence.
 */
@Repository
public interface ShippingRateRepository extends JpaRepository<ShippingRate, Long> {

    Optional<ShippingRate> findByZoneIdAndMethodIdAndActiveTrue(Long zoneId, Long methodId);
}
