package com.ecommerce.ecommercebackend.config;

import com.ecommerce.ecommercebackend.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

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
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        // Public catalog browsing: anyone can read products and their reviews.
                        .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                        // Product recommendations are shown on public product detail pages.
                        .requestMatchers(HttpMethod.GET, "/api/recommendations/**").permitAll()
                        // Interaction dataset for the AI recommendation engine (service-to-service).
                        .requestMatchers(HttpMethod.GET, "/api/ratings").permitAll()
                        // Payment callbacks/IPN — called by VNPAY/MoMo servers, no JWT available.
                        .requestMatchers("/api/payment/vnpay/callback", "/api/payment/vnpay/ipn",
                                         "/api/payment/momo/callback",  "/api/payment/momo/ipn").permitAll()
                        // Chatbot (FastAPI) pushes messages server-to-server without a JWT.
                        .requestMatchers(HttpMethod.POST, "/api/chat/save").permitAll()
                        // WebSocket handshake endpoints — JWT auth happens inside STOMP CONNECT.
                        .requestMatchers("/ws/**", "/ws-sockjs/**").permitAll()
                        // REST helper to get room info (public — no sensitive data).
                        .requestMatchers(HttpMethod.GET, "/api/ws/room/**").permitAll()
                        // Admin area: only ROLE_ADMIN.
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // Catalog management (create/update/delete) is admin-only; GET stays public.
                        .requestMatchers(HttpMethod.POST,   "/api/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT,    "/api/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasRole("ADMIN")
                        // Everything else requires a logged-in user.
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

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:3001",
                "http://127.0.0.1:3000",
                "http://127.0.0.1:3001"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Origin"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
