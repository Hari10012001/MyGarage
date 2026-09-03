package com.mygarage.controller.web;

import com.mygarage.config.AuthHelper;
import com.mygarage.model.User;
import com.mygarage.service.DashboardService;
import com.mygarage.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardWebController {

    private final DashboardService dashboardService;
    private final VehicleService vehicleService;
    private final AuthHelper authHelper;

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    @GetMapping
    public String dashboard(Model model) {
        User user = authHelper.getCurrentUser();
        Long userId = user.getUserId();

        model.addAttribute("user", user);
        model.addAttribute("vehicles", vehicleService.getVehiclesForUser(userId));
        model.addAttribute("vehicleCount", dashboardService.getUserVehicleCount(userId));
        model.addAttribute("serviceCount", dashboardService.getUserServiceCount(userId));
        model.addAttribute("fuelCount", dashboardService.getUserFuelCount(userId));
        model.addAttribute("maintenanceCount", dashboardService.getUserMaintenanceCount(userId));
        model.addAttribute("alertCount", dashboardService.getUserMaintenanceAlertCount(userId));
        model.addAttribute("overdueCount", dashboardService.getUserMaintenanceOverdueCount(userId));
        model.addAttribute("dueTodayCount", dashboardService.getUserMaintenanceDueTodayCount(userId));
        model.addAttribute("upcomingCount", dashboardService.getUserMaintenanceUpcomingCount(userId));
        model.addAttribute("completedCount", dashboardService.getUserMaintenanceCompletedCount(userId));
        model.addAttribute("totalFuelCost", dashboardService.getUserTotalFuelCost(userId));
        model.addAttribute("totalServiceCost", dashboardService.getUserTotalServiceCost(userId));
        model.addAttribute("totalMaintenanceCost", dashboardService.getUserTotalMaintenanceCost(userId));
        model.addAttribute("totalGarageCost", dashboardService.getUserTotalGarageCost(userId));
        model.addAttribute("averageMileage", dashboardService.getUserAverageMileage(userId));
        model.addAttribute("recentServices", dashboardService.getRecentServiceRecords(userId));
        model.addAttribute("recentFuelRecords", dashboardService.getRecentFuelRecords(userId));
        model.addAttribute("maintenanceAlerts", dashboardService.getMaintenanceAlerts(userId));

        return "dashboard/index";
    }
}
