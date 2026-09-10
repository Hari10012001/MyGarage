package com.mygarage.controller.web;

import com.mygarage.config.AuthHelper;
import com.mygarage.dto.response.GarageFleetDispatchReportDTO;
import com.mygarage.dto.response.VehicleTripReadinessReportDTO;
import com.mygarage.model.User;
import com.mygarage.model.Vehicle;
import com.mygarage.service.VehicleReadinessService;
import com.mygarage.service.VehicleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequiredArgsConstructor
public class VehicleReadinessWebController {

    private final VehicleReadinessService vehicleReadinessService;
    private final VehicleService vehicleService;
    private final AuthHelper authHelper;

    @GetMapping("/vehicles/{id}/readiness")
    public String viewVehicleReadiness(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "500.0") Double tripDistanceKm,
            @RequestParam(required = false, defaultValue = "2") Integer tripDays,
            @RequestParam(required = false, defaultValue = "HIGHWAY_CRUISE") String drivingRegime,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        User currentUser = authHelper.getCurrentUser();
        try {
            // Verify ownership first (throws if unauthorized or vehicle not found)
            Vehicle vehicle = vehicleService.getVehicleForUser(id, currentUser.getUserId());
            VehicleTripReadinessReportDTO report = vehicleReadinessService.evaluateVehicleReadiness(
                    id, tripDistanceKm, tripDays, drivingRegime, currentUser.getUserId()
            );

            model.addAttribute("vehicle", vehicle);
            model.addAttribute("userVehicles", vehicleService.getVehiclesForUser(currentUser.getUserId()));
            model.addAttribute("report", report);
            model.addAttribute("tripDistanceKm", tripDistanceKm);
            model.addAttribute("tripDays", tripDays);
            model.addAttribute("drivingRegime", drivingRegime);
            model.addAttribute("user", currentUser);

            return "vehicle/readiness";
        } catch (AccessDeniedException | IllegalArgumentException e) {
            log.warn("Access denied for user {} viewing readiness of vehicle {}", currentUser.getUserId(), id);
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/vehicles";
        } catch (Exception e) {
            log.error("Error loading trip readiness report for vehicle {}", id, e);
            redirectAttributes.addFlashAttribute("errorMsg", "Unable to evaluate trip readiness: " + e.getMessage());
            return "redirect:/vehicles/" + id;
        }
    }

    @GetMapping("/vehicles/dispatch")
    public String viewGarageFleetDispatch(
            @RequestParam(required = false, defaultValue = "500.0") Double tripDistanceKm,
            @RequestParam(required = false, defaultValue = "2") Integer tripDays,
            @RequestParam(required = false, defaultValue = "HIGHWAY_CRUISE") String drivingRegime,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        User currentUser = authHelper.getCurrentUser();
        try {
            GarageFleetDispatchReportDTO report = vehicleReadinessService.evaluateFleetDispatch(
                    tripDistanceKm, tripDays, drivingRegime, currentUser.getUserId()
            );

            model.addAttribute("report", report);
            model.addAttribute("tripDistanceKm", tripDistanceKm);
            model.addAttribute("tripDays", tripDays);
            model.addAttribute("drivingRegime", drivingRegime);
            model.addAttribute("user", currentUser);

            return "vehicle/garage-dispatch";
        } catch (Exception e) {
            log.error("Error loading fleet dispatch report for user {}", currentUser.getUserId(), e);
            redirectAttributes.addFlashAttribute("errorMsg", "Unable to evaluate garage fleet dispatch: " + e.getMessage());
            return "redirect:/vehicles";
        }
    }
}
