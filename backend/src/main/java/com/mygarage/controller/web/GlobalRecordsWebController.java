package com.mygarage.controller.web;

import com.mygarage.config.AuthHelper;
import com.mygarage.model.FuelRecord;
import com.mygarage.model.MaintenanceRecord;
import com.mygarage.model.ServiceRecord;
import com.mygarage.model.enums.MaintenanceStatus;
import com.mygarage.service.FuelRecordService;
import com.mygarage.service.MaintenanceService;
import com.mygarage.service.ServiceRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Controller for centralized cross-vehicle records:
 * 1. /services — All Service Records
 * 2. /fuel — All Fuel Logs
 * 3. /maintenance — All Maintenance Tasks & Centralized Planner
 */
@Controller
@PreAuthorize("hasRole('NORMAL_USER')")
@RequiredArgsConstructor
public class GlobalRecordsWebController {

    private final ServiceRecordService serviceRecordService;
    private final FuelRecordService fuelRecordService;
    private final MaintenanceService maintenanceService;
    private final AuthHelper authHelper;

    @GetMapping("/services")
    public String allServices(@RequestParam(required = false) String search, Model model) {
        Long userId = authHelper.getCurrentUserId();
        List<ServiceRecord> services = serviceRecordService.getAllServicesForUser(userId, search);
        BigDecimal totalCost = serviceRecordService.getTotalServiceCostForUser(userId);

        model.addAttribute("user", authHelper.getCurrentUser());
        model.addAttribute("services", services);
        model.addAttribute("totalCost", totalCost != null ? totalCost : BigDecimal.ZERO);
        model.addAttribute("search", search);
        model.addAttribute("activePage", "services");
        return "service/index";
    }

    @GetMapping("/fuel")
    public String allFuel(Model model) {
        Long userId = authHelper.getCurrentUserId();
        List<FuelRecord> fuelRecords = fuelRecordService.getAllFuelForUser(userId);
        BigDecimal totalCost = fuelRecordService.totalFuelCostForUser(userId);
        BigDecimal totalLitres = fuelRecordService.totalFuelQuantityForUser(userId);
        Double avgMileage = fuelRecordService.averageEstimatedMileageForUser(userId);

        model.addAttribute("user", authHelper.getCurrentUser());
        model.addAttribute("fuelRecords", fuelRecords);
        model.addAttribute("totalCost", totalCost != null ? totalCost : BigDecimal.ZERO);
        model.addAttribute("totalLitres", totalLitres != null ? totalLitres : BigDecimal.ZERO);
        model.addAttribute("avgMileage", avgMileage != null ? avgMileage : 0.0);
        model.addAttribute("activePage", "fuel");
        return "fuel/index";
    }

    @GetMapping("/maintenance")
    public String allMaintenance(@RequestParam(required = false) String status, Model model) {
        Long userId = authHelper.getCurrentUserId();
        MaintenanceStatus filterStatus = null;
        if (status != null && !status.isBlank() && !status.equalsIgnoreCase("ALL")) {
            try {
                filterStatus = MaintenanceStatus.valueOf(status.toUpperCase().trim());
            } catch (IllegalArgumentException ignored) {}
        }

        List<MaintenanceRecord> tasks = maintenanceService.getAllMaintenanceForUser(userId, filterStatus);

        model.addAttribute("user", authHelper.getCurrentUser());
        model.addAttribute("tasks", tasks);
        model.addAttribute("selectedStatus", status != null ? status.toUpperCase() : "ALL");
        model.addAttribute("overdueCount", maintenanceService.countMaintenanceForUserAndStatus(userId, MaintenanceStatus.OVERDUE));
        model.addAttribute("dueTodayCount", maintenanceService.countMaintenanceForUserAndStatus(userId, MaintenanceStatus.DUE_TODAY));
        model.addAttribute("upcomingCount", maintenanceService.countMaintenanceForUserAndStatus(userId, MaintenanceStatus.UPCOMING));
        model.addAttribute("completedCount", maintenanceService.countMaintenanceForUserAndStatus(userId, MaintenanceStatus.COMPLETED));
        model.addAttribute("activePage", "maintenance");
        return "maintenance/index";
    }

    @PostMapping("/maintenance/{id}/complete")
    public String completeMaintenance(@PathVariable Long id,
                                      @RequestParam(required = false) String completedDate,
                                      RedirectAttributes ra) {
        try {
            Long userId = authHelper.getCurrentUserId();
            LocalDate date = (completedDate != null && !completedDate.isBlank())
                    ? LocalDate.parse(completedDate)
                    : LocalDate.now();
            maintenanceService.markCompleted(id, userId, date);
            ra.addFlashAttribute("successMsg", "Maintenance task marked as completed.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Could not complete maintenance task: " + e.getMessage());
        }
        return "redirect:/maintenance";
    }
}