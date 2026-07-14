package com.ecommerce.ecommercebackend.repository;

import com.ecommerce.ecommercebackend.entity.ShippingMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link ShippingMethod} persistence.
 */
@Repository
public interface ShippingMethodRepository extends JpaRepository<ShippingMethod, Long> {

    Optional<ShippingMethod> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);
}
