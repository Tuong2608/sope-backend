package com.ecommerce.ecommercebackend.repository;

import com.ecommerce.ecommercebackend.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link CartItem} persistence.
 */
@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
}
