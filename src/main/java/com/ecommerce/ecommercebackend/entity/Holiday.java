package com.ecommerce.ecommercebackend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * A non-working day used to push back the estimated delivery date (task C03).
 * Dates are stored in Vietnam local time (the whole app runs on VN business hours).
 */
@Entity
@Table(
        name = "holidays",
        uniqueConstraints = @UniqueConstraint(name = "uk_holidays_date", columnNames = "date")
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Holiday {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate date;

    @Column(length = 255)
    private String description;
}
