package com.mygarage.controller.api;

import com.mygarage.config.AuthHelper;
import com.mygarage.dto.request.UpdateProfileRequest;
import com.mygarage.model.User;
import com.mygarage.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST API for Authenticated User Profile Management.
 * Accessible to any authenticated user (NORMAL_USER or ADMIN).
 */
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileApiController {

    private final UserService userService;
    private final AuthHelper authHelper;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getProfile() {
        User user = authHelper.getCurrentUser();
        return ResponseEntity.ok(Map.of(
                "userId", user.getUserId(),
                "fullName", user.getFullName(),
                "email", user.getEmail(),
                "phone", user.getPhone() != null ? user.getPhone() : "",
                "role", user.getRole().name(),
                "createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : ""
        ));
    }

    @PutMapping
    public ResponseEntity<Map<String, Object>> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        Long userId = authHelper.getCurrentUserId();
        User updated = userService.updateProfile(userId, request);
        return ResponseEntity.ok(Map.of(
                "userId", updated.getUserId(),
                "fullName", updated.getFullName(),
                "phone", updated.getPhone() != null ? updated.getPhone() : "",
                "message", "Profile updated successfully."
        ));
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(@RequestBody Map<String, String> body) {
        String currentPassword = body.get("currentPassword");
        String newPassword = body.get("newPassword");
        if (currentPassword == null || newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("New password must be at least 6 characters.");
        }
        Long userId = authHelper.getCurrentUserId();
        userService.changePassword(userId, currentPassword, newPassword);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully."));
    }
}