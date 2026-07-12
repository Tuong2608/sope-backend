package com.ecommerce.ecommercebackend.repository;

import com.ecommerce.ecommercebackend.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Address} (a user's address book).
 */
@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {

    /** All addresses of a user, default first, then most recently created. */
    List<Address> findByUserIdOrderByIsDefaultDescCreatedAtDesc(Long userId);

    /** A single address scoped to its owner (prevents cross-user access). */
    Optional<Address> findByIdAndUserId(Long id, Long userId);

    Optional<Address> findByUserIdAndIsDefaultTrue(Long userId);

    long countByUserId(Long userId);
}
