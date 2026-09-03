package com.mygarage.controller.api;

import com.mygarage.model.User;
import com.mygarage.model.VehicleCategory;
import com.mygarage.service.DashboardService;
import com.mygarage.service.UserService;
import com.mygarage.service.VehicleCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API for Administrative Operations.
 * Strictly gated to ROLE_ADMIN.
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminApiController {

    private final DashboardService dashboardService;
    private final UserService userService;
    private final VehicleCategoryService categoryService;

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        return ResponseEntity.ok(Map.of(
                "totalUsers", dashboardService.getAdminTotalUsers(),
                "totalVehicles", dashboardService.getAdminTotalVehicles(),
                "totalServiceRecords", dashboardService.getAdminTotalServiceRecords(),
                "totalFuelRecords", dashboardService.getAdminTotalFuelRecords(),
                "totalMaintenanceRecords", dashboardService.getAdminTotalMaintenanceRecords()
        ));
    }

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.findAllUsers());
    }

    @PostMapping("/users/{userId}/toggle")
    public ResponseEntity<Map<String, Object>> toggleUserStatus(@PathVariable Long userId) {
        userService.toggleUserStatus(userId);
        User user = userService.findById(userId);
        return ResponseEntity.ok(Map.of(
                "userId", user.getUserId(),
                "isActive", user.isActive(),
                "message", "User status updated successfully."
        ));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<VehicleCategory>> getCategories() {
        return ResponseEntity.ok(categoryService.findAll());
    }
}