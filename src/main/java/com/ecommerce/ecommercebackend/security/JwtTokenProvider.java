package com.ecommerce.ecommercebackend.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

/**
 * Utility component responsible for JWT lifecycle management:
 * token generation, validation, and claim extraction.
 *
 * <p>Uses HMAC-SHA256 with a Base64-encoded secret configured in
 * {@code application.properties} to satisfy JJWT 0.11.5 key-strength
 * requirements (minimum 256-bit secret).</p>
 */
@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    // ── Token generation ──────────────────────────────────────────────────────

    /**
     * Generates a signed JWT for the given {@link UserDetails}.
     *
     * @param userDetails authenticated user
     * @return compact JWT string
     */
    public String generateToken(UserDetails userDetails) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // ── Claim extraction ──────────────────────────────────────────────────────

    /**
     * Extracts the username (subject) embedded in the token.
     *
     * @param token JWT string
     * @return username stored in the subject claim
     */
    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    // ── Validation ────────────────────────────────────────────────────────────

    /**
     * Validates the token against the provided {@link UserDetails}.
     *
     * @param token       JWT string
     * @param userDetails user to validate against
     * @return {@code true} if the token is valid and not expired
     */
    public boolean validateToken(String token, UserDetails userDetails) {
        try {
            String username = extractUsername(token);
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Validates a token without a specific user — used by the WebSocket interceptor
     * when {@link UserDetails} is not yet available at STOMP CONNECT time.
     *
     * @param token JWT string
     * @return {@code true} if the token is well-formed, signed correctly, and not expired
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token); // throws if invalid or expired
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Alias for {@link #extractUsername(String)} — used by the WebSocket interceptor.
     */
    public String getUsernameFromToken(String token) {
        return extractUsername(token);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private boolean isTokenExpired(String token) {
        return parseClaims(token).getExpiration().before(new Date());
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Decodes the Base64-encoded secret and wraps it in a {@link Key}.
     * The secret must be at least 256 bits (32 bytes) for HS256.
     */
    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
