package com.ecommerce.ecommercebackend.controller;

import com.ecommerce.ecommercebackend.dto.request.GoogleLoginRequest;
import com.ecommerce.ecommercebackend.dto.request.ForgotPasswordRequest;
import com.ecommerce.ecommercebackend.dto.request.LoginRequest;
import com.ecommerce.ecommercebackend.dto.request.RegisterRequest;
import com.ecommerce.ecommercebackend.dto.request.ResetPasswordRequest;
import com.ecommerce.ecommercebackend.dto.response.AuthResponse;
import com.ecommerce.ecommercebackend.dto.response.PasswordResetResponse;
import com.ecommerce.ecommercebackend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller exposing authentication endpoints.
 *
 * <p>All routes are prefixed with {@code /api/auth} and explicitly permitted
 * in {@link com.ecommerce.ecommercebackend.config.SecurityConfig}.</p>
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Registers a new user account.
     *
     * @param request validated registration payload
     * @return 201 Created with a confirmation message
     */
    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body("User registered successfully.");
    }

    /**
     * Authenticates a user and returns a JWT.
     *
     * @param request validated login credentials
     * @return 200 OK with {@link AuthResponse} containing the access token
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<PasswordResetResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(authService.requestPasswordReset(request));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok("Password reset successfully.");
    }

    /**
     * Authenticates a user with a Google Identity Services ID token.
     *
     * @param request Google credential payload from the frontend
     * @return 200 OK with an application JWT
     */
    @PostMapping("/google")
    public ResponseEntity<AuthResponse> googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
        AuthResponse response = authService.loginWithGoogle(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/google/client-id")
    public ResponseEntity<Map<String, String>> googleClientId() {
        return ResponseEntity.ok(Map.of("clientId", authService.getGoogleClientId()));
    }
}
