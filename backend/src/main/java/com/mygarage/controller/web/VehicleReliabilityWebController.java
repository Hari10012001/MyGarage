package com.mygarage.controller.web;

import com.mygarage.config.AuthHelper;
import com.mygarage.dto.response.GarageReliabilityMatrixDTO;
import com.mygarage.dto.response.VehicleReliabilityReportDTO;
import com.mygarage.model.User;
import com.mygarage.model.Vehicle;
import com.mygarage.service.VehicleReliabilityService;
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
public class VehicleReliabilityWebController {

    private final VehicleReliabilityService vehicleReliabilityService;
    private final VehicleService vehicleService;
    private final AuthHelper authHelper;

    @GetMapping("/vehicles/{id}/reliability")
    public String viewVehicleReliability(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        User currentUser = authHelper.getCurrentUser();
        try {
            // Verify ownership first (throws if unauthorized or vehicle not found)
            Vehicle vehicle = vehicleService.getVehicleForUser(id, currentUser.getUserId());
            VehicleReliabilityReportDTO report = vehicleReliabilityService.getVehicleReliability(id, currentUser.getUserId());

            model.addAttribute("vehicle", vehicle);
            model.addAttribute("report", report);
            model.addAttribute("user", currentUser);

            return "vehicle/reliability";
        } catch (AccessDeniedException | IllegalArgumentException e) {
            log.warn("Access denied for user {} viewing reliability of vehicle {}", currentUser.getUserId(), id);
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/vehicles";
        } catch (Exception e) {
            log.error("Error loading reliability report for vehicle {}", id, e);
            redirectAttributes.addFlashAttribute("errorMsg", "Unable to load reliability report: " + e.getMessage());
            return "redirect:/vehicles/" + id;
        }
    }

    @GetMapping("/vehicles/reliability")
    public String viewGarageReliability(Model model, RedirectAttributes redirectAttributes) {
        User currentUser = authHelper.getCurrentUser();
        try {
            GarageReliabilityMatrixDTO matrix = vehicleReliabilityService.getGarageReliabilityMatrix(currentUser.getUserId());

            model.addAttribute("matrix", matrix);
            model.addAttribute("user", currentUser);

            return "vehicle/garage-reliability";
        } catch (Exception e) {
            log.error("Error loading garage reliability matrix for user {}", currentUser.getUserId(), e);
            redirectAttributes.addFlashAttribute("errorMsg", "Unable to load garage reliability matrix: " + e.getMessage());
            return "redirect:/vehicles";
        }
    }
}
