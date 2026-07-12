package com.ecommerce.ecommercebackend.repository;

import com.ecommerce.ecommercebackend.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * B02 – Repository cho {@link ProductVariant}.
 */
@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    /** Lấy tất cả variant đang active của một sản phẩm. */
    List<ProductVariant> findByProductIdAndActiveTrue(Long productId);

    /** Lấy tất cả variant (kể cả inactive) của một sản phẩm. */
    List<ProductVariant> findByProductId(Long productId);

    /** Tìm variant theo SKU. */
    Optional<ProductVariant> findBySku(String sku);

    /** Lấy các variant còn hàng của sản phẩm. */
    @Query("""
            SELECT v FROM ProductVariant v
            WHERE v.product.id = :productId
              AND v.active = true
              AND (v.stockQuantity - v.reservedQuantity) > 0
            ORDER BY v.storageName, v.colorName
            """)
    List<ProductVariant> findAvailableVariants(@Param("productId") Long productId);

    /** Đếm số variant đang active của sản phẩm. */
    long countByProductIdAndActiveTrue(Long productId);
}
