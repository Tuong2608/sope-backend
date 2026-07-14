package com.ecommerce.ecommercebackend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@org.springframework.test.context.TestPropertySource(properties = {
    "app.jwt.secret=dGVzdC1zZWNyZXQta2V5LWZvci1qdW5pdC10ZXN0aW5nLW9ubHktbm90LXVzZWQtaW4tcHJvZHVjdGlvbg==",
    "app.jwt.expiration-ms=3600000"
})
class EcommerceBackendApplicationTests {

    @Test
    void contextLoads() {
    }

}
