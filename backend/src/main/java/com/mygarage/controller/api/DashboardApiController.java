package com.mygarage.controller.api;

import com.mygarage.config.AuthHelper;
import com.mygarage.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardApiController {

    private final DashboardService dashboardService;
    private final AuthHelper authHelper;

    @GetMapping("/user")
    public ResponseEntity<Map<String, Object>> userDashboard() {
        Long userId = authHelper.getCurrentUserId();
        return ResponseEntity.ok(Map.of(
                "vehicleCount", dashboardService.getUserVehicleCount(userId),
                "serviceCount", dashboardService.getUserServiceCount(userId),
                "fuelCount", dashboardService.getUserFuelCount(userId),
                "alertCount", dashboardService.getUserMaintenanceAlertCount(userId),
                "totalFuelCost", dashboardService.getUserTotalFuelCost(userId)
        ));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> adminDashboard() {
        return ResponseEntity.ok(Map.of(
                "totalUsers", dashboardService.getAdminTotalUsers(),
                "totalVehicles", dashboardService.getAdminTotalVehicles(),
                "totalServiceRecords", dashboardService.getAdminTotalServiceRecords(),
                "totalFuelRecords", dashboardService.getAdminTotalFuelRecords(),
                "totalMaintenanceRecords", dashboardService.getAdminTotalMaintenanceRecords()
        ));
    }
}
