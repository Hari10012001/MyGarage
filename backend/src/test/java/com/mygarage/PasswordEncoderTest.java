package com.mygarage;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordEncoderTest {

    @Test
    void testAdminPasswordHash() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);
        String hash = encoder.encode("Admin@123");
        System.out.println("GENERATED_ADMIN_HASH=" + hash);
        assertTrue(encoder.matches("Admin@123", hash));
    }
}