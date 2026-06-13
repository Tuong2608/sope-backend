package com.ecommerce.ecommercebackend.dto.request;

import com.ecommerce.ecommercebackend.entity.Role;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request payload for an admin changing a user's role.
 */
@Data
public class UpdateUserRoleRequest {

    @NotNull(message = "role is required (ROLE_USER or ROLE_ADMIN)")
    private Role role;
}
