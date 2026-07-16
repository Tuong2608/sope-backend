package com.ecommerce.ecommercebackend.repository;

import com.ecommerce.ecommercebackend.entity.ShippingRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShippingRateRepository extends JpaRepository<ShippingRate, Long> {

    Optional<ShippingRate> findByZoneIdAndMethodIdAndActiveTrue(Long zoneId, Long methodId);

    boolean existsByZoneIdAndMethodId(Long zoneId, Long methodId);

    boolean existsByZoneIdAndMethodIdAndIdNot(Long zoneId, Long methodId, Long id);

    long countByZoneId(Long zoneId);

    long countByMethodId(Long methodId);
}
