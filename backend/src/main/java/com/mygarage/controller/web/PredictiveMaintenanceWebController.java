package com.mygarage.controller.web;

import com.mygarage.config.AuthHelper;
import com.mygarage.dto.request.ScheduleForecastRequestDTO;
import com.mygarage.dto.response.VehiclePredictiveReportDTO;
import com.mygarage.model.Vehicle;
import com.mygarage.service.PredictiveMaintenanceService;
import com.mygarage.service.VehicleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Slf4j
@Controller
@RequestMapping("/vehicles")
@RequiredArgsConstructor
public class PredictiveMaintenanceWebController {

    private final PredictiveMaintenanceService predictiveMaintenanceService;
    private final VehicleService vehicleService;
    private final AuthHelper authHelper;

    /**
     * Web view for single vehicle predictive maintenance & service planner.
     */
    @GetMapping("/{id}/forecast")
    public String viewVehicleForecast(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Long userId = authHelper.getCurrentUserId();
        try {
            VehiclePredictiveReportDTO report = predictiveMaintenanceService.getVehicleForecast(id, userId);
            List<Vehicle> userVehicles = vehicleService.getVehiclesForUser(userId);

            model.addAttribute("report", report);
            model.addAttribute("vehicle", vehicleService.getVehicleForUser(id, userId));
            model.addAttribute("userVehicles", userVehicles);
            model.addAttribute("scheduleRequest", new ScheduleForecastRequestDTO());
            return "vehicle/forecast";
        } catch (AccessDeniedException ex) {
            log.warn("Access denied for user {} viewing forecast of vehicle {}: {}", userId, id, ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Access Denied: You do not have permission to view that vehicle.");
            return "redirect:/vehicles";
        } catch (Exception ex) {
            log.error("Error generating forecast for vehicle {}: {}", id, ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/vehicles";
        }
    }

    /**
     * Web view for garage-wide fleet predictive overview.
     */
    @GetMapping("/planner")
    public String viewGaragePlanner(Model model, RedirectAttributes redirectAttributes) {
        Long userId = authHelper.getCurrentUserId();
        List<Vehicle> userVehicles = vehicleService.getVehiclesForUser(userId);

        if (userVehicles.isEmpty()) {
            redirectAttributes.addFlashAttribute("infoMessage", "Please add a vehicle to view the predictive maintenance planner.");
            return "redirect:/vehicles";
        }

        // Direct to the first vehicle's forecast page for detailed visual display
        Long firstVehicleId = userVehicles.get(0).getVehicleId();
        return "redirect:/vehicles/" + firstVehicleId + "/forecast";
    }

    /**
     * State-changing POST to schedule a predicted milestone into maintenance tasks.
     * Enforces CSRF and two-tier ownership validation.
     */
    @PostMapping("/{id}/forecast/schedule")
    public String scheduleForecastMilestone(@PathVariable Long id,
                                            @Valid @ModelAttribute("scheduleRequest") ScheduleForecastRequestDTO request,
                                            BindingResult bindingResult,
                                            RedirectAttributes redirectAttributes) {
        Long userId = authHelper.getCurrentUserId();

        if (bindingResult.hasErrors()) {
            String errorMsg = bindingResult.getAllErrors().get(0).getDefaultMessage();
            redirectAttributes.addFlashAttribute("errorMessage", errorMsg);
            return "redirect:/vehicles/" + id + "/forecast";
        }

        try {
            predictiveMaintenanceService.scheduleForecastedMilestone(id, request, userId);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Service milestone '" + request.getTitle() + "' successfully scheduled for " + request.getScheduledDate() + "!");
        } catch (AccessDeniedException ex) {
            log.warn("Access denied for user {} scheduling milestone on vehicle {}: {}", userId, id, ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Access Denied: Unauthorized vehicle operation.");
            return "redirect:/vehicles";
        } catch (IllegalStateException ex) {
            // Duplicate task detected
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            log.error("Error scheduling forecast milestone for vehicle {}: {}", id, ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to schedule task: " + ex.getMessage());
        }

        return "redirect:/vehicles/" + id + "/forecast";
    }
}
