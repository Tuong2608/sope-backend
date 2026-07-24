package com.ecommerce.ecommercebackend.repository;

import com.ecommerce.ecommercebackend.entity.Product;
import com.ecommerce.ecommercebackend.entity.ProductStatus;
import com.ecommerce.ecommercebackend.dto.response.ChatbotProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * Spring Data JPA repository for {@link Product} persistence.
 *
 * <p>Extends {@link JpaSpecificationExecutor} to support the dynamic
 * keyword search / attribute filtering built in {@code ProductService}.</p>
 */
@Repository
public interface ProductRepository
        extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    @Query("""
            select new com.ecommerce.ecommercebackend.dto.response.ChatbotProductResponse(
                p.id, p.sku, p.name, p.category, p.brand,
                substring(p.shortDescription, 1, 300),
                p.price, p.oldPrice,
                p.stockQuantity, p.reservedQuantity, p.status)
            from Product p
            where p.status <> :excludedStatus
            """)
    Page<ChatbotProductResponse> findChatbotProducts(
            @Param("excludedStatus") ProductStatus excludedStatus,
            Pageable pageable);

    @Query("""
            select p.id, key(spec), value(spec)
            from Product p
            join p.specs spec
            where p.id in :productIds
            order by p.id
            """)
    List<Object[]> findChatbotProductSpecEntries(
            @Param("productIds") Collection<Long> productIds);
}
