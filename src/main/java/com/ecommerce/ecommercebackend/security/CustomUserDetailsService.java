package com.ecommerce.ecommercebackend.security;

import com.ecommerce.ecommercebackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Bridges Spring Security's {@link UserDetailsService} contract with the
 * application's {@link UserRepository}.
 *
 * <p>Because {@link com.ecommerce.ecommercebackend.entity.User} already
 * implements {@link UserDetails}, no additional adapter class is required.</p>
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        String identifier = usernameOrEmail == null ? "" : usernameOrEmail.trim();
        String normalizedEmail = identifier.toLowerCase(Locale.ROOT);

        return userRepository.findByUsernameOrEmail(identifier, normalizedEmail)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found with username or email: " + identifier));
    }
}
