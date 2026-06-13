package com.ecommerce.ecommercebackend.dto.response;

import com.ecommerce.ecommercebackend.entity.Role;
import lombok.Builder;
import lombok.Data;

/**
 * User representation for the admin user-management endpoints
 * (never exposes the password hash).
 */
@Data
@Builder
public class UserResponse {

    private Long id;
    private String username;
    private String email;
    private Role role;
    private boolean enabled;
}
