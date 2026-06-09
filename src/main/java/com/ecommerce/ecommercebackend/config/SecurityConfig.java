package com.ecommerce.ecommercebackend.config;

import com.ecommerce.ecommercebackend.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Modern, component-based Spring Security configuration.
 *
 * <p>Key decisions:
 * <ul>
 *   <li>CSRF disabled – API is stateless; CSRF tokens are meaningless without sessions.</li>
 *   <li>Session policy STATELESS – no {@code HttpSession} is ever created or used.</li>
 *   <li>{@link JwtAuthenticationFilter} runs before
 *       {@link UsernamePasswordAuthenticationFilter} so every request is pre-authenticated.</li>
 * </ul>
 * </p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsService      userDetailsService;

    // ── Security filter chain ─────────────────────────────────────────────────

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        // Public catalog browsing: anyone can read products.
                        .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                        // Payment callbacks/IPN — called by VNPAY/MoMo servers, no JWT available.
                        .requestMatchers("/api/payment/vnpay/callback", "/api/payment/vnpay/ipn",
                                         "/api/payment/momo/callback",  "/api/payment/momo/ipn").permitAll()
                        // Chatbot (FastAPI) pushes messages server-to-server without a JWT.
                        // NOTE: phase-1 simplicity — secure with a shared API key in a later phase.
                        .requestMatchers(HttpMethod.POST, "/api/chat/save").permitAll()
                        // Catalog management (create/update/delete) requires a logged-in user.
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    // ── Authentication beans ──────────────────────────────────────────────────

    /**
     * {@link DaoAuthenticationProvider} wires together the password encoder
     * and {@link UserDetailsService} for username/password authentication.
     *
     * <p>Spring Security 6.x (Spring Boot 4) changed the constructor to require
     * {@link UserDetailsService} as a mandatory argument; the
     * {@code setUserDetailsService()} setter was removed.</p>
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * Exposes the {@link AuthenticationManager} bean so that the
     * {@code AuthController} can inject and use it directly.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
