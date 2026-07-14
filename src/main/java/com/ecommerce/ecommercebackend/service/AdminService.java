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
    public UserResponse changeRole(User currentUser, Long userId, Role role) {
        User user = findUserOrThrow(userId);

        if (currentUser.getId().equals(userId) && role != Role.ROLE_ADMIN) {
            throw new com.ecommerce.ecommercebackend.exception.BadRequestException("Bạn không thể tự hạ quyền của chính mình.");
        }

        if (user.getRole() == Role.ROLE_ADMIN && role != Role.ROLE_ADMIN) {
            long adminCount = userRepository.findAll().stream()
                    .filter(u -> u.getRole() == Role.ROLE_ADMIN)
                    .count();
            if (adminCount <= 1) {
                throw new com.ecommerce.ecommercebackend.exception.BadRequestException("Không thể hạ quyền Admin duy nhất còn lại của hệ thống.");
            }
        }

        user.setRole(role);
        return toUserResponse(userRepository.save(user));
    }

    /** Locks ({@code enabled=false}) or unlocks a user account. */
    @Transactional
    public UserResponse setEnabled(User currentUser, Long userId, boolean enabled) {
        User user = findUserOrThrow(userId);
        
        if (!enabled && currentUser.getId().equals(userId)) {
            throw new com.ecommerce.ecommercebackend.exception.BadRequestException("Bạn không thể tự khóa tài khoản của chính mình.");
        }

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
