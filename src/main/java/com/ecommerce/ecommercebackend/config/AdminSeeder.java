package com.ecommerce.ecommercebackend.config;

import com.ecommerce.ecommercebackend.entity.Role;
import com.ecommerce.ecommercebackend.entity.User;
import com.ecommerce.ecommercebackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Ensures a default administrator account exists on startup.
 *
 * <p>Credentials are configurable via {@code app.admin.*} properties. The seeder
 * is idempotent: it does nothing if a user with the configured username already
 * exists, so it is safe to run on every boot.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username:admin}")
    private String adminUsername;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Value("${app.admin.email:admin@sope.local}")
    private String adminEmail;

    @Override
    public void run(String... args) {
        if (userRepository.existsByUsername(adminUsername)) {
            return;
        }
        User admin = User.builder()
                .username(adminUsername)
                .password(passwordEncoder.encode(adminPassword))
                .email(adminEmail)
                .role(Role.ROLE_ADMIN)
                .enabled(true)
                .build();
        userRepository.save(admin);
        log.info("Seeded default admin account '{}'", adminUsername);
    }
}
