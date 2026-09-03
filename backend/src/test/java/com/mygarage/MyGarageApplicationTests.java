package com.mygarage;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * MyGarage Application Context Load Test
 * Verifies that the Spring application context loads successfully.
 */
@SpringBootTest
@ActiveProfiles("test")
class MyGarageApplicationTests {

    @Test
    void contextLoads() {
        // If this test passes, the Spring context loaded without errors
    }
}
