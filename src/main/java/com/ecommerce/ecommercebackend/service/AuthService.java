package com.ecommerce.ecommercebackend.service;

import com.ecommerce.ecommercebackend.dto.request.LoginRequest;
import com.ecommerce.ecommercebackend.dto.request.RegisterRequest;
import com.ecommerce.ecommercebackend.dto.response.AuthResponse;
import com.ecommerce.ecommercebackend.entity.Role;
import com.ecommerce.ecommercebackend.entity.User;
import com.ecommerce.ecommercebackend.exception.UserAlreadyExistsException;
import com.ecommerce.ecommercebackend.repository.UserRepository;
import com.ecommerce.ecommercebackend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service encapsulating authentication business logic.
 *
 * <p>Keeping this logic out of the controller respects the Single Responsibility
 * Principle: the controller handles HTTP concerns, the service handles domain logic.</p>
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository      userRepository;
    private final PasswordEncoder     passwordEncoder;
    private final JwtTokenProvider    jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    // ── Registration ──────────────────────────────────────────────────────────

    /**
     * Registers a new user after validating uniqueness constraints.
     *
     * @param request validated registration payload
     * @throws UserAlreadyExistsException if username or email is already taken
     */
    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException(
                    "Username '" + request.getUsername() + "' is already taken.");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException(
                    "Email '" + request.getEmail() + "' is already registered.");
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))  // BCrypt hash
                .email(request.getEmail())
                .role(Role.ROLE_USER)
                .build();

        userRepository.save(user);
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    /**
     * Authenticates credentials and issues a JWT on success.
     *
     * @param request login payload
     * @return {@link AuthResponse} carrying the signed JWT
     * @throws org.springframework.security.authentication.BadCredentialsException
     *         if username or password is incorrect (thrown by Spring Security internally)
     */
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtTokenProvider.generateToken(userDetails);

        return AuthResponse.builder()
                .accessToken(token)
                .build();
    }
}
