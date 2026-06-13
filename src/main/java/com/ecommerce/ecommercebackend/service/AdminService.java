package com.ecommerce.ecommercebackend.service;

import com.ecommerce.ecommercebackend.dto.response.AdminStatsResponse;
import com.ecommerce.ecommercebackend.dto.response.UserResponse;
import com.ecommerce.ecommercebackend.entity.Role;
import com.ecommerce.ecommercebackend.entity.User;
import com.ecommerce.ecommercebackend.exception.ResourceNotFoundException;
import com.ecommerce.ecommercebackend.repository.OrderRepository;
import com.ecommerce.ecommercebackend.repository.ProductRepository;
import com.ecommerce.ecommercebackend.repository.ReviewRepository;
import com.ecommerce.ecommercebackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Admin-only business logic: user management and dashboard statistics.
 * Order and review administration are delegated to their own services.
 */
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final ReviewRepository reviewRepository;

    // ── User management ───────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toUserResponse)
                .toList();
    }

    @Transactional
    public UserResponse changeRole(Long userId, Role role) {
        User user = findUserOrThrow(userId);
        user.setRole(role);
        return toUserResponse(userRepository.save(user));
    }

    /** Locks ({@code enabled=false}) or unlocks a user account. */
    @Transactional
    public UserResponse setEnabled(Long userId, boolean enabled) {
        User user = findUserOrThrow(userId);
        user.setEnabled(enabled);
        return toUserResponse(userRepository.save(user));
    }

    // ── Statistics ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public AdminStatsResponse getStats() {
        return AdminStatsResponse.builder()
                .totalUsers(userRepository.count())
                .totalProducts(productRepository.count())
                .totalOrders(orderRepository.count())
                .totalReviews(reviewRepository.count())
                .totalRevenue(orderRepository.totalRevenue())
                .build();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + userId));
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .enabled(user.isEnabled())
                .build();
    }
}
