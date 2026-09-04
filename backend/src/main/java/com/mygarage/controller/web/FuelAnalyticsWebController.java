package com.mygarage.controller.web;

import com.mygarage.dto.response.GarageFuelIntelligenceDTO;
import com.mygarage.dto.response.VehicleFuelAnalyticsDTO;
import com.mygarage.model.User;
import com.mygarage.model.Vehicle;
import com.mygarage.service.FuelAnalyticsService;
import com.mygarage.config.AuthHelper;
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
public class FuelAnalyticsWebController {

    private final FuelAnalyticsService fuelAnalyticsService;
    private final AuthHelper authHelper;
    private final VehicleService vehicleService;

    @GetMapping("/vehicles/{id}/fuel-analytics")
    public String viewVehicleFuelAnalytics(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        User currentUser = authHelper.getCurrentUser();
        try {
            // Verify ownership first (will throw if invalid)
            Vehicle vehicle = vehicleService.getVehicleForUser(id, currentUser.getUserId());
            
            VehicleFuelAnalyticsDTO report = fuelAnalyticsService.getVehicleFuelAnalytics(id, currentUser.getUserId());
            
            model.addAttribute("report", report);
            model.addAttribute("vehicle", vehicle);
            model.addAttribute("user", currentUser);
            
            return "fuel/analytics";
        } catch (AccessDeniedException | IllegalArgumentException e) {
            log.warn("Access denied for user {} viewing fuel analytics of vehicle {}", currentUser.getUserId(), id);
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/vehicles";
        } catch (Exception e) {
            log.error("Error loading fuel analytics for vehicle {}", id, e);
            redirectAttributes.addFlashAttribute("errorMsg", "Unable to load fuel analytics: " + e.getMessage());
            return "redirect:/vehicles/" + id;
        }
    }

    @GetMapping("/vehicles/fuel-analytics")
    public String viewGarageFuelSummary(Model model, RedirectAttributes redirectAttributes) {
        User currentUser = authHelper.getCurrentUser();
        try {
            GarageFuelIntelligenceDTO summary = fuelAnalyticsService.getGarageFuelIntelligence(currentUser.getUserId());
            
            model.addAttribute("summary", summary);
            model.addAttribute("user", currentUser);
            
            return "fuel/garage-fuel";
        } catch (Exception e) {
            log.error("Error loading garage fuel summary", e);
            redirectAttributes.addFlashAttribute("errorMsg", "Unable to load garage fuel summary.");
            return "redirect:/dashboard";
        }
    }
}
