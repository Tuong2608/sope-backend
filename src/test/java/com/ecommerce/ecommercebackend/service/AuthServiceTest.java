package com.ecommerce.ecommercebackend.service;

import com.ecommerce.ecommercebackend.dto.request.ForgotPasswordRequest;
import com.ecommerce.ecommercebackend.dto.request.RegisterRequest;
import com.ecommerce.ecommercebackend.dto.response.PasswordResetResponse;
import com.ecommerce.ecommercebackend.entity.AuthProvider;
import com.ecommerce.ecommercebackend.entity.Role;
import com.ecommerce.ecommercebackend.entity.User;
import com.ecommerce.ecommercebackend.exception.BadRequestException;
import com.ecommerce.ecommercebackend.exception.UserAlreadyExistsException;
import com.ecommerce.ecommercebackend.repository.UserRepository;
import com.ecommerce.ecommercebackend.security.JwtTokenProvider;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private GoogleIdTokenVerifier googleIdTokenVerifier;

    @Mock
    private MailService mailService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository, passwordEncoder, jwtTokenProvider,
                authenticationManager, googleIdTokenVerifier, mailService);
        ReflectionTestUtils.setField(authService, "frontendBaseUrl", "http://localhost:3000");
        ReflectionTestUtils.setField(authService, "passwordResetExpirationMinutes", 30L);
        ReflectionTestUtils.setField(authService, "emailVerificationExpirationMinutes", 1440L);
    }

    // ── register() ────────────────────────────────────────────────────────────

    @Test
    void registerSavesUserAndSendsVerificationEmail() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("toan");
        request.setPassword("password123");
        request.setEmail("toan@example.com");

        when(userRepository.existsByUsername("toan")).thenReturn(false);
        when(userRepository.existsByEmail("toan@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed-password");

        authService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertThat(saved.isEmailVerified()).isFalse();
        assertThat(saved.getEmailVerificationTokenHash()).isNotBlank();
        assertThat(saved.getEmailVerificationTokenExpiresAt()).isAfter(LocalDateTime.now());

        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);
        verify(mailService).sendVerificationEmail(eq("toan@example.com"), linkCaptor.capture());
        assertThat(linkCaptor.getValue()).startsWith("http://localhost:3000/verify-email?token=");
    }

    @Test
    void registerThrowsWhenUsernameTakenAndDoesNotSendEmail() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("toan");
        request.setPassword("password123");
        request.setEmail("toan@example.com");

        when(userRepository.existsByUsername("toan")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(UserAlreadyExistsException.class);

        verify(mailService, never()).sendVerificationEmail(anyString(), anyString());
        verify(userRepository, never()).save(any());
    }

    // ── requestPasswordReset() ───────────────────────────────────────────────

    @Test
    void requestPasswordResetSendsResetEmailForExistingUser() {
        User user = User.builder().id(1L).username("toan").email("toan@example.com").role(Role.ROLE_USER).build();
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("toan@example.com");

        when(userRepository.findByEmail("toan@example.com")).thenReturn(Optional.of(user));

        PasswordResetResponse response = authService.requestPasswordReset(request);

        assertThat(response.getMessage()).isEqualTo("If the email exists, a password reset link has been created.");
        assertThat(user.getPasswordResetTokenHash()).isNotBlank();
        verify(userRepository).save(user);

        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);
        verify(mailService).sendPasswordResetEmail(eq("toan@example.com"), linkCaptor.capture());
        assertThat(linkCaptor.getValue()).startsWith("http://localhost:3000/reset-password?token=");
    }

    @Test
    void requestPasswordResetDoesNotSendEmailForUnknownUser() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("ghost@example.com");

        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        PasswordResetResponse response = authService.requestPasswordReset(request);

        assertThat(response.getMessage()).isEqualTo("If the email exists, a password reset link has been created.");
        verify(mailService, never()).sendPasswordResetEmail(anyString(), anyString());
        verify(userRepository, never()).save(any());
    }

    // ── verifyEmail() ────────────────────────────────────────────────────────

    @Test
    void verifyEmailMarksUserVerifiedForValidToken() {
        String rawToken = "raw-verification-token";
        User user = User.builder()
                .id(1L)
                .email("toan@example.com")
                .role(Role.ROLE_USER)
                .provider(AuthProvider.LOCAL)
                .emailVerified(false)
                .emailVerificationTokenHash(sha256Hex(rawToken))
                .emailVerificationTokenExpiresAt(LocalDateTime.now().plusMinutes(10))
                .build();

        when(userRepository.findByEmailVerificationTokenHash(sha256Hex(rawToken))).thenReturn(Optional.of(user));

        authService.verifyEmail(rawToken);

        assertThat(user.isEmailVerified()).isTrue();
        assertThat(user.getEmailVerificationTokenHash()).isNull();
        assertThat(user.getEmailVerificationTokenExpiresAt()).isNull();
        verify(userRepository).save(user);
    }

    @Test
    void verifyEmailThrowsForExpiredToken() {
        String rawToken = "expired-token";
        User user = User.builder()
                .id(1L)
                .email("toan@example.com")
                .role(Role.ROLE_USER)
                .emailVerified(false)
                .emailVerificationTokenHash(sha256Hex(rawToken))
                .emailVerificationTokenExpiresAt(LocalDateTime.now().minusMinutes(1))
                .build();

        when(userRepository.findByEmailVerificationTokenHash(sha256Hex(rawToken))).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.verifyEmail(rawToken))
                .isInstanceOf(BadRequestException.class);

        assertThat(user.isEmailVerified()).isFalse();
        assertThat(user.getEmailVerificationTokenHash()).isNull();
        verify(userRepository).save(user);
    }

    @Test
    void verifyEmailThrowsForUnknownToken() {
        when(userRepository.findByEmailVerificationTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.verifyEmail("unknown-token"))
                .isInstanceOf(BadRequestException.class);
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
