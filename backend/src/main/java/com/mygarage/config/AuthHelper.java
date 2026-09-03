package com.mygarage.config;

import com.mygarage.model.User;
import com.mygarage.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Helper to get the currently authenticated User from the database.
 */
@Component
@RequiredArgsConstructor
public class AuthHelper {

    private final UserRepository userRepository;

    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found in database: " + email));
    }

    public Long getCurrentUserId() {
        return getCurrentUser().getUserId();
    }
}
