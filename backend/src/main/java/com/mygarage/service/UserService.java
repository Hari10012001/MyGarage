package com.mygarage.service;

import com.mygarage.dto.request.RegisterRequest;
import com.mygarage.dto.request.UpdateProfileRequest;
import com.mygarage.model.User;
import com.mygarage.model.enums.Role;
import com.mygarage.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Register a new NORMAL_USER.
     * Admin cannot be registered via this method.
     */
    public User register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }
        User user = new User();
        user.setFullName(request.getFullName().trim());
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setRole(Role.NORMAL_USER);
        user.setActive(true);
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email.toLowerCase());
    }

    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
    }

    public User updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findById(userId);
        user.setFullName(request.getFullName().trim());
        user.setPhone(request.getPhone());
        return userRepository.save(user);
    }

    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = findById(userId);
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<User> findAllUsers() {
        return userRepository.findByRole(Role.NORMAL_USER);
    }

    @Transactional(readOnly = true)
    public List<User> searchUsers(String query) {
        if (query == null || query.isBlank()) {
            return findAllUsers();
        }
        return userRepository.searchUsers(query.trim());
    }

    public void toggleUserStatus(Long userId) {
        User user = findById(userId);
        if (user.getRole() == Role.ADMIN || "admin@mygarage.com".equalsIgnoreCase(user.getEmail())) {
            throw new IllegalArgumentException("Cannot deactivate or modify the primary administrator account.");
        }
        user.setActive(!user.isActive());
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public long countUsers() {
        return userRepository.countByRole(Role.NORMAL_USER);
    }
}
