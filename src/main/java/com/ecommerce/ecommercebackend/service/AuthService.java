package com.ecommerce.ecommercebackend.service;

import com.ecommerce.ecommercebackend.dto.request.GoogleLoginRequest;
import com.ecommerce.ecommercebackend.dto.request.ForgotPasswordRequest;
import com.ecommerce.ecommercebackend.dto.request.LoginRequest;
import com.ecommerce.ecommercebackend.dto.request.RegisterRequest;
import com.ecommerce.ecommercebackend.dto.request.ResetPasswordRequest;
import com.ecommerce.ecommercebackend.dto.response.AuthResponse;
import com.ecommerce.ecommercebackend.dto.response.PasswordResetResponse;
import lombok.extern.slf4j.Slf4j;
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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final GoogleIdTokenVerifier googleIdTokenVerifier;
    private final MailService mailService;
    private final SecureRandom secureRandom = new SecureRandom();

    private static final String RESET_MESSAGE =
            "If the email exists, a password reset link has been created.";

    @Value("${app.google.client-id:}")
    private String googleClientId;

    @Value("${app.frontend.base-url:http://localhost:3000}")
    private String frontendBaseUrl;

    @Value("${app.password-reset.expiration-minutes:30}")
    private long passwordResetExpirationMinutes;

    @Value("${app.email-verification.expiration-minutes:1440}")
    private long emailVerificationExpirationMinutes;

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

        String verificationToken = generateResetToken();
        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(request.getPassword()))
                .email(email)
                .role(Role.ROLE_USER)
                .provider(AuthProvider.LOCAL)
                .emailVerified(false)
                .emailVerificationTokenHash(hashToken(verificationToken))
                .emailVerificationTokenExpiresAt(LocalDateTime.now().plusMinutes(emailVerificationExpirationMinutes))
                .build();

        userRepository.save(user);

        String verifyLink = buildVerificationLink(verificationToken);
        mailService.sendVerificationEmail(email, verifyLink);
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
    public PasswordResetResponse requestPasswordReset(ForgotPasswordRequest request) {
        String email = normalizeEmail(request.getEmail());
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isEmpty()) {
            return PasswordResetResponse.builder()
                    .message(RESET_MESSAGE)
                    .build();
        }

        String token = generateResetToken();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(passwordResetExpirationMinutes);
        User account = user.get();
        account.setPasswordResetTokenHash(hashToken(token));
        account.setPasswordResetTokenExpiresAt(expiresAt);
        userRepository.save(account);

        String resetLink = buildResetLink(token);
        mailService.sendPasswordResetEmail(email, resetLink);

        return PasswordResetResponse.builder()
                .message(RESET_MESSAGE)
                .build();
    }

    @Transactional
    public void verifyEmail(String token) {
        User user = userRepository.findByEmailVerificationTokenHash(hashToken(token.trim()))
                .orElseThrow(() -> new BadRequestException("Verification token is invalid or expired."));
        LocalDateTime expiresAt = user.getEmailVerificationTokenExpiresAt();
        if (expiresAt == null || expiresAt.isBefore(LocalDateTime.now())) {
            clearEmailVerificationToken(user);
            userRepository.save(user);
            throw new BadRequestException("Verification token is invalid or expired.");
        }

        user.setEmailVerified(true);
        clearEmailVerificationToken(user);
        userRepository.save(user);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Password confirmation does not match.");
        }

        User user = userRepository.findByPasswordResetTokenHash(hashToken(request.getToken().trim()))
                .orElseThrow(() -> new BadRequestException("Reset token is invalid or expired."));
        LocalDateTime expiresAt = user.getPasswordResetTokenExpiresAt();
        if (expiresAt == null || expiresAt.isBefore(LocalDateTime.now())) {
            clearPasswordResetToken(user);
            userRepository.save(user);
            throw new BadRequestException("Reset token is invalid or expired.");
        }

        user.setPassword(passwordEncoder.encode(request.getPassword()));
        clearPasswordResetToken(user);
        userRepository.save(user);
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

    private String generateResetToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available.", ex);
        }
    }

    private String buildResetLink(String token) {
        String baseUrl = frontendBaseUrl == null ? "" : frontendBaseUrl.trim();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        String encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8);
        return baseUrl + "/reset-password?token=" + encodedToken;
    }

    private void clearPasswordResetToken(User user) {
        user.setPasswordResetTokenHash(null);
        user.setPasswordResetTokenExpiresAt(null);
    }

    private String buildVerificationLink(String token) {
        String baseUrl = frontendBaseUrl == null ? "" : frontendBaseUrl.trim();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        String encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8);
        return baseUrl + "/verify-email?token=" + encodedToken;
    }

    private void clearEmailVerificationToken(User user) {
        user.setEmailVerificationTokenHash(null);
        user.setEmailVerificationTokenExpiresAt(null);
    }

    private String generateOAuthPasswordHash() {
        return passwordEncoder.encode(UUID.randomUUID().toString());
    }

    private String preferNewValue(String currentValue, String newValue) {
        return StringUtils.hasText(newValue) ? newValue : currentValue;
    }
}
