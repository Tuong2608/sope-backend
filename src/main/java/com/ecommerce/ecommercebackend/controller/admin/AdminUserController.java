package com.ecommerce.ecommercebackend.controller.admin;

import com.ecommerce.ecommercebackend.dto.request.UpdateUserRoleRequest;
import com.ecommerce.ecommercebackend.dto.response.UserResponse;
import com.ecommerce.ecommercebackend.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin user management. Secured to ROLE_ADMIN via {@code /api/admin/**}.
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminService adminService;

    @GetMapping
    public ResponseEntity<List<UserResponse>> all() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<UserResponse> changeRole(
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.ecommerce.ecommercebackend.entity.User currentUser,
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRoleRequest request) {
        return ResponseEntity.ok(adminService.changeRole(currentUser, id, request.getRole()));
    }

    @PutMapping("/{id}/lock")
    public ResponseEntity<UserResponse> lock(
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.ecommerce.ecommercebackend.entity.User currentUser,
            @PathVariable Long id) {
        return ResponseEntity.ok(adminService.setEnabled(currentUser, id, false));
    }

    @PutMapping("/{id}/unlock")
    public ResponseEntity<UserResponse> unlock(
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.ecommerce.ecommercebackend.entity.User currentUser,
            @PathVariable Long id) {
        return ResponseEntity.ok(adminService.setEnabled(currentUser, id, true));
    }
}
