package com.ecommerce.ecommercebackend.repository;

import com.ecommerce.ecommercebackend.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Spring Data JPA repository for {@link Holiday} persistence.
 */
@Repository
public interface HolidayRepository extends JpaRepository<Holiday, Long> {

    boolean existsByDate(LocalDate date);

    List<Holiday> findByDateBetween(LocalDate start, LocalDate end);
}
