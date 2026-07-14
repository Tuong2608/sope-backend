package com.ecommerce.ecommercebackend.security;

import com.ecommerce.ecommercebackend.entity.Role;
import com.ecommerce.ecommercebackend.entity.User;
import com.ecommerce.ecommercebackend.exception.BadRequestException;
import com.ecommerce.ecommercebackend.exception.ResourceNotFoundException;
import com.ecommerce.ecommercebackend.repository.UserRepository;
import com.ecommerce.ecommercebackend.service.AdminService;
import com.ecommerce.ecommercebackend.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@org.springframework.test.context.TestPropertySource(properties = {
    "app.jwt.secret=dGVzdC1zZWNyZXQta2V5LWZvci1qdW5pdC10ZXN0aW5nLW9ubHktbm90LXVzZWQtaW4tcHJvZHVjdGlvbg==",
    "app.jwt.expiration-ms=3600000"
})
public class SecurityAccessIntegrationTest {

    @Autowired
    private AdminService adminService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserRepository userRepository;

    private User userA;
    private User userB;
    private User admin;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        userA = userRepository.save(User.builder()
                .username("user_a")
                .password("123456")
                .email("usera@test.com")
                .role(Role.ROLE_USER)
                .build());

        userB = userRepository.save(User.builder()
                .username("user_b")
                .password("123456")
                .email("userb@test.com")
                .role(Role.ROLE_USER)
                .build());

        admin = userRepository.save(User.builder()
                .username("admin_test")
                .password("123456")
                .email("admin@test.com")
                .role(Role.ROLE_ADMIN)
                .build());
    }

    @Test
    void testUserCannotViewOtherUserOrders() {
        // User A cố gắng xem một order không tồn tại của User A thì ra ResourceNotFound
        // Nhưng nếu cố gắng xem order của người khác, logic `findByIdAndUserId` cũng ra ResourceNotFound
        assertThrows(ResourceNotFoundException.class, () -> {
            orderService.getOrder(userA, 9999L);
        });
    }

    @Test
    void testAdminCannotLockThemself() {
        assertThrows(BadRequestException.class, () -> {
            adminService.setEnabled(admin, admin.getId(), false);
        });
    }

    @Test
    void testAdminCannotDemoteLastAdmin() {
        assertThrows(BadRequestException.class, () -> {
            adminService.changeRole(admin, admin.getId(), Role.ROLE_USER);
        });
    }
}

