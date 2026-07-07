package com.ecommerce.ecommercebackend.service;

import com.ecommerce.ecommercebackend.dto.request.GoogleLoginRequest;
import com.ecommerce.ecommercebackend.dto.request.LoginRequest;
import com.ecommerce.ecommercebackend.dto.request.RegisterRequest;
import com.ecommerce.ecommercebackend.dto.response.AuthResponse;
import com.ecommerce.ecommercebackend.entity.AuthProvider;
import com.ecommerce.ecommercebackend.entity.Role;
import com.ecommerce.ecommercebackend.entity.User;
import com.ecommerce.ecommercebackend.exception.BadRequestException;
import com.ecommerce.ecommercebackend.exception.UserAlreadyExistsException;
import com.ecommerce.ecommercebackend.repository.UserRepository;
import com.ecommerce.ecommercebackend.security.JwtTokenProvider;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final GoogleIdTokenVerifier googleIdTokenVerifier;

    @Value("${app.google.client-id:}")
    private String googleClientId;

    @Transactional
    public void register(RegisterRequest request) {
        String username = request.getUsername().trim();
        String email = normalizeEmail(request.getEmail());

        if (userRepository.existsByUsername(username)) {
            throw new UserAlreadyExistsException(
                    "Username '" + username + "' is already taken.");
        }
        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException(
                    "Email '" + email + "' is already registered.");
        }

        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(request.getPassword()))
                .email(email)
                .role(Role.ROLE_USER)
                .provider(AuthProvider.LOCAL)
                .emailVerified(false)
                .build();

        userRepository.save(user);
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername().trim(),
                        request.getPassword()
                )
        );

        User user = (User) authentication.getPrincipal();
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse loginWithGoogle(GoogleLoginRequest request) {
        if (!StringUtils.hasText(googleClientId)) {
            throw new BadRequestException("Google login is not configured on the server.");
        }

        GoogleIdToken.Payload payload = verifyGoogleCredential(request.getCredential());
        String providerId = payload.getSubject();
        String email = normalizeEmail(payload.getEmail());

        if (!StringUtils.hasText(providerId) || !StringUtils.hasText(email)) {
            throw new BadCredentialsException("Invalid Google account information.");
        }
        if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
            throw new BadCredentialsException("Google email is not verified.");
        }

        String fullName = (String) payload.get("name");
        String avatarUrl = (String) payload.get("picture");

        User user = userRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, providerId)
                .map(existing -> updateGoogleProfile(existing, email, fullName, avatarUrl))
                .orElseGet(() -> findOrCreateGoogleUser(providerId, email, fullName, avatarUrl));

        return buildAuthResponse(user);
    }

    public String getGoogleClientId() {
        return googleClientId;
    }

    private GoogleIdToken.Payload verifyGoogleCredential(String credential) {
        try {
            GoogleIdToken idToken = googleIdTokenVerifier.verify(credential);
            if (idToken == null) {
                throw new BadCredentialsException("Invalid Google credential.");
            }
            return idToken.getPayload();
        } catch (GeneralSecurityException | IOException e) {
            throw new BadCredentialsException("Could not verify Google credential.", e);
        }
    }

    private User findOrCreateGoogleUser(
            String providerId,
            String email,
            String fullName,
            String avatarUrl
    ) {
        Optional<User> existingByEmail = userRepository.findByEmail(email);
        if (existingByEmail.isPresent()) {
            User existing = existingByEmail.get();
            if (existing.getProvider() == AuthProvider.GOOGLE
                    && StringUtils.hasText(existing.getProviderId())
                    && !providerId.equals(existing.getProviderId())) {
                throw new BadRequestException("This email is linked to another Google account.");
            }

            existing.setProvider(AuthProvider.GOOGLE);
            existing.setProviderId(providerId);
            existing.setEmailVerified(true);
            existing.setFullName(preferNewValue(existing.getFullName(), fullName));
            existing.setAvatarUrl(preferNewValue(existing.getAvatarUrl(), avatarUrl));
            return userRepository.save(existing);
        }

        User user = User.builder()
                .username(generateUniqueUsername(email))
                .password(generateOAuthPasswordHash())
                .email(email)
                .role(Role.ROLE_USER)
                .provider(AuthProvider.GOOGLE)
                .providerId(providerId)
                .fullName(fullName)
                .avatarUrl(avatarUrl)
                .emailVerified(true)
                .build();

        return userRepository.save(user);
    }

    private User updateGoogleProfile(User user, String email, String fullName, String avatarUrl) {
        user.setEmail(email);
        user.setEmailVerified(true);
        user.setFullName(preferNewValue(user.getFullName(), fullName));
        user.setAvatarUrl(preferNewValue(user.getAvatarUrl(), avatarUrl));
        return userRepository.save(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        String token = jwtTokenProvider.generateToken(user);

        return AuthResponse.builder()
                .accessToken(token)
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole().name())
                .build();
    }

    private String generateUniqueUsername(String email) {
        String localPart = email.split("@", 2)[0].toLowerCase(Locale.ROOT);
        String base = localPart.replaceAll("[^a-z0-9._-]", "");
        if (!StringUtils.hasText(base) || base.length() < 3) {
            base = "google_user";
        }
        if (base.length() > 42) {
            base = base.substring(0, 42);
        }

        String candidate = base;
        int suffix = 1;
        while (userRepository.existsByUsername(candidate)) {
            candidate = base + suffix;
            suffix++;
        }
        return candidate;
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private String generateOAuthPasswordHash() {
        return passwordEncoder.encode(UUID.randomUUID().toString());
    }

    private String preferNewValue(String currentValue, String newValue) {
        return StringUtils.hasText(newValue) ? newValue : currentValue;
    }
}
