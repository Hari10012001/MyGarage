package com.mygarage.controller.web;

import com.mygarage.config.AuthHelper;
import com.mygarage.dto.response.GarageMaintenanceDeficitMatrixDTO;
import com.mygarage.dto.response.VehicleMaintenanceDeficitReportDTO;
import com.mygarage.model.User;
import com.mygarage.model.Vehicle;
import com.mygarage.service.VehicleMaintenanceDeficitService;
import com.mygarage.service.VehicleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequiredArgsConstructor
public class VehicleMaintenanceDeficitWebController {

    private final VehicleMaintenanceDeficitService vehicleMaintenanceDeficitService;
    private final VehicleService vehicleService;
    private final AuthHelper authHelper;

    @GetMapping("/vehicles/{id}/maintenance-deficit")
    public String viewVehicleDeficit(
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        User currentUser = authHelper.getCurrentUser();
        try {
            // Verify ownership first (throws if unauthorized or vehicle not found)
            Vehicle vehicle = vehicleService.getVehicleForUser(id, currentUser.getUserId());
            VehicleMaintenanceDeficitReportDTO report = vehicleMaintenanceDeficitService.evaluateVehicleDeficit(
                    id, currentUser.getUserId()
            );

            model.addAttribute("vehicle", vehicle);
            model.addAttribute("report", report);
            model.addAttribute("user", currentUser);

            return "vehicle/maintenance-deficit";
        } catch (AccessDeniedException | IllegalArgumentException e) {
            log.warn("Access denied for user {} viewing maintenance deficit of vehicle {}", currentUser.getUserId(), id);
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/vehicles";
        } catch (Exception e) {
            log.error("Error loading maintenance deficit report for vehicle {}", id, e);
            redirectAttributes.addFlashAttribute("errorMsg", "Unable to evaluate maintenance deficit: " + e.getMessage());
            return "redirect:/vehicles/" + id;
        }
    }

    @GetMapping("/vehicles/maintenance-deficit")
    public String viewGarageMaintenanceDeficit(
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        User currentUser = authHelper.getCurrentUser();
        try {
            GarageMaintenanceDeficitMatrixDTO matrix = vehicleMaintenanceDeficitService.evaluateGarageDeficit(
                    currentUser.getUserId()
            );

            model.addAttribute("matrix", matrix);
            model.addAttribute("user", currentUser);

            return "vehicle/garage-maintenance-deficit";
        } catch (Exception e) {
            log.error("Error loading garage maintenance deficit matrix for user {}", currentUser.getUserId(), e);
            redirectAttributes.addFlashAttribute("errorMsg", "Unable to evaluate garage maintenance deficit: " + e.getMessage());
            return "redirect:/vehicles";
        }
    }
}
