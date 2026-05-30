package com.ecommerce.ecommercebackend.repository;

import com.ecommerce.ecommercebackend.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link Product} persistence.
 *
 * <p>Extends {@link JpaSpecificationExecutor} to support the dynamic
 * keyword search / attribute filtering built in {@code ProductService}.</p>
 */
@Repository
public interface ProductRepository
        extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
}
