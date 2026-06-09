package com.ecommerce.ecommercebackend.repository;

import com.ecommerce.ecommercebackend.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link ChatSession} persistence.
 */
@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    Optional<ChatSession> findByUserId(String userId);

    /** Sessions ordered by most-recent activity (for the seller dashboard). */
    List<ChatSession> findAllByOrderByUpdatedAtDesc();
}
