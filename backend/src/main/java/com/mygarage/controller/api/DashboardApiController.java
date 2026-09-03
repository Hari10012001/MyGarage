package com.mygarage.controller.api;

import com.mygarage.config.AuthHelper;
import com.mygarage.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardApiController {

    private final DashboardService dashboardService;
    private final AuthHelper authHelper;

    @GetMapping({"", "/summary", "/user"})
    public ResponseEntity<Map<String, Object>> userDashboard() {
        Long userId = authHelper.getCurrentUserId();
        return ResponseEntity.ok(Map.ofEntries(
                Map.entry("vehicleCount", dashboardService.getUserVehicleCount(userId)),
                Map.entry("serviceCount", dashboardService.getUserServiceCount(userId)),
                Map.entry("fuelCount", dashboardService.getUserFuelCount(userId)),
                Map.entry("maintenanceCount", dashboardService.getUserMaintenanceCount(userId)),
                Map.entry("alertCount", dashboardService.getUserMaintenanceAlertCount(userId)),
                Map.entry("overdueCount", dashboardService.getUserMaintenanceOverdueCount(userId)),
                Map.entry("dueTodayCount", dashboardService.getUserMaintenanceDueTodayCount(userId)),
                Map.entry("upcomingCount", dashboardService.getUserMaintenanceUpcomingCount(userId)),
                Map.entry("completedCount", dashboardService.getUserMaintenanceCompletedCount(userId)),
                Map.entry("totalFuelCost", dashboardService.getUserTotalFuelCost(userId)),
                Map.entry("totalServiceCost", dashboardService.getUserTotalServiceCost(userId)),
                Map.entry("totalMaintenanceCost", dashboardService.getUserTotalMaintenanceCost(userId)),
                Map.entry("totalGarageCost", dashboardService.getUserTotalGarageCost(userId)),
                Map.entry("averageMileage", dashboardService.getUserAverageMileage(userId) != null ? dashboardService.getUserAverageMileage(userId) : 0.0)
        ));
    }

    @GetMapping("/alerts")
    public ResponseEntity<List<com.mygarage.model.MaintenanceRecord>> getAlerts() {
        Long userId = authHelper.getCurrentUserId();
        return ResponseEntity.ok(dashboardService.getMaintenanceAlerts(userId));
    }

    @GetMapping("/recent-services")
    public ResponseEntity<List<com.mygarage.model.ServiceRecord>> getRecentServices() {
        Long userId = authHelper.getCurrentUserId();
        return ResponseEntity.ok(dashboardService.getRecentServiceRecords(userId));
    }

    @GetMapping("/recent-fuel")
    public ResponseEntity<List<com.mygarage.model.FuelRecord>> getRecentFuel() {
        Long userId = authHelper.getCurrentUserId();
        return ResponseEntity.ok(dashboardService.getRecentFuelRecords(userId));
    }
}
