package com.ecommerce.ecommercebackend.repository;

import com.ecommerce.ecommercebackend.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Cart} persistence.
 */
@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    @EntityGraph(attributePaths = {"items", "items.product", "items.variant"})
    Optional<Cart> findByUserId(Long userId);
}
