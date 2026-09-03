package com.mygarage.controller.web;

import com.mygarage.config.AuthHelper;
import com.mygarage.dto.request.MaintenanceRequest;
import com.mygarage.model.Vehicle;
import com.mygarage.model.enums.MaintenanceStatus;
import com.mygarage.service.MaintenanceService;
import com.mygarage.service.VehicleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/vehicles/{vehicleId}/maintenance")
@RequiredArgsConstructor
public class MaintenanceWebController {

    private final MaintenanceService maintenanceService;
    private final VehicleService vehicleService;
    private final AuthHelper authHelper;

    @GetMapping("/add")
    public String addForm(@PathVariable Long vehicleId, Model model) {
        Long userId = authHelper.getCurrentUserId();
        Vehicle vehicle = vehicleService.getVehicleForUser(vehicleId, userId);
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("maintenanceRequest", new MaintenanceRequest());
        model.addAttribute("user", authHelper.getCurrentUser());
        return "maintenance/form";
    }

    @PostMapping("/add")
    public String addMaintenance(@PathVariable Long vehicleId,
                                 @Valid @ModelAttribute("maintenanceRequest") MaintenanceRequest request,
                                 BindingResult result, RedirectAttributes ra, Model model) {
        Long userId = authHelper.getCurrentUserId();
        if (result.hasErrors()) {
            model.addAttribute("vehicle", vehicleService.getVehicleForUser(vehicleId, userId));
            model.addAttribute("user", authHelper.getCurrentUser());
            return "maintenance/form";
        }
        try {
            maintenanceService.addMaintenanceRecord(vehicleId, userId, request);
            ra.addFlashAttribute("successMsg", "Maintenance task added!");
            return "redirect:/vehicles/" + vehicleId + "?tab=maintenance";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMsg", e.getMessage());
            model.addAttribute("vehicle", vehicleService.getVehicleForUser(vehicleId, userId));
            model.addAttribute("user", authHelper.getCurrentUser());
            return "maintenance/form";
        }
    }

    @GetMapping
    public String listMaintenance(@PathVariable Long vehicleId) {
        return "redirect:/vehicles/" + vehicleId + "?tab=maintenance";
    }

    @GetMapping("/{maintenanceId}")
    public String viewMaintenance(@PathVariable Long vehicleId, @PathVariable Long maintenanceId, Model model) {
        Long userId = authHelper.getCurrentUserId();
        var record = maintenanceService.getMaintenanceRecordForVehicle(maintenanceId, vehicleId, userId);
        model.addAttribute("record", record);
        model.addAttribute("vehicle", record.getVehicle());
        model.addAttribute("user", authHelper.getCurrentUser());
        return "redirect:/vehicles/" + vehicleId + "?tab=maintenance";
    }

    @GetMapping("/{maintenanceId}/edit")
    public String editForm(@PathVariable Long vehicleId, @PathVariable Long maintenanceId, Model model) {
        Long userId = authHelper.getCurrentUserId();
        var record = maintenanceService.getMaintenanceRecordForVehicle(maintenanceId, vehicleId, userId);
        MaintenanceRequest req = new MaintenanceRequest();
        req.setTitle(record.getTitle());
        req.setDescription(record.getDescription());
        req.setScheduledDate(record.getScheduledDate());
        req.setCompletedDate(record.getCompletedDate());
        req.setCost(record.getCost());
        req.setNotes(record.getNotes());
        model.addAttribute("maintenanceRequest", req);
        model.addAttribute("maintenanceId", maintenanceId);
        model.addAttribute("vehicle", record.getVehicle());
        model.addAttribute("user", authHelper.getCurrentUser());
        return "maintenance/form";
    }

    @PostMapping("/{maintenanceId}/edit")
    public String updateMaintenance(@PathVariable Long vehicleId, @PathVariable Long maintenanceId,
                                    @Valid @ModelAttribute("maintenanceRequest") MaintenanceRequest request,
                                    BindingResult result, RedirectAttributes ra, Model model) {
        Long userId = authHelper.getCurrentUserId();
        if (result.hasErrors()) {
            model.addAttribute("maintenanceId", maintenanceId);
            model.addAttribute("vehicle", vehicleService.getVehicleForUser(vehicleId, userId));
            model.addAttribute("user", authHelper.getCurrentUser());
            return "maintenance/form";
        }
        maintenanceService.updateMaintenanceRecordForVehicle(maintenanceId, vehicleId, userId, request);
        ra.addFlashAttribute("successMsg", "Maintenance task updated.");
        return "redirect:/vehicles/" + vehicleId + "?tab=maintenance";
    }

    @PostMapping("/{maintenanceId}/complete")
    public String markCompleted(@PathVariable Long vehicleId, @PathVariable Long maintenanceId,
                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate completedDate,
                                RedirectAttributes ra) {
        Long userId = authHelper.getCurrentUserId();
        maintenanceService.markCompletedForVehicle(maintenanceId, vehicleId, userId, completedDate);
        ra.addFlashAttribute("successMsg", "Maintenance marked as completed.");
        return "redirect:/vehicles/" + vehicleId + "?tab=maintenance";
    }

    @PostMapping("/{maintenanceId}/delete")
    public String deleteMaintenance(@PathVariable Long vehicleId, @PathVariable Long maintenanceId,
                                    RedirectAttributes ra) {
        Long userId = authHelper.getCurrentUserId();
        maintenanceService.deleteMaintenanceRecordForVehicle(maintenanceId, vehicleId, userId);
        ra.addFlashAttribute("successMsg", "Maintenance task deleted.");
        return "redirect:/vehicles/" + vehicleId + "?tab=maintenance";
    }
}
