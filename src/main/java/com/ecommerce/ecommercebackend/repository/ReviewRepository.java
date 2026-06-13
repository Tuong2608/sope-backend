package com.ecommerce.ecommercebackend.repository;

import com.ecommerce.ecommercebackend.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Review} (table {@code ratings}).
 */
@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByProductIdOrderByCreatedAtDesc(Long productId);

    Optional<Review> findByUserIdAndProductId(Long userId, Long productId);

    List<Review> findByUserIdOrderByCreatedAtDesc(Long userId);

    /** Average star rating of a product, or {@code null} when it has no reviews. */
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId")
    Double averageRatingByProductId(Long productId);
}
