package com.mygarage.controller.web;

import com.mygarage.config.AuthHelper;
import com.mygarage.dto.response.GarageFiscalBudgetMatrixDTO;
import com.mygarage.dto.response.VehicleFiscalBudgetReportDTO;
import com.mygarage.model.User;
import com.mygarage.model.Vehicle;
import com.mygarage.service.VehicleFiscalBudgetService;
import com.mygarage.service.VehicleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * M20: Web MVC controller for Vehicle Operational Budgeting,
 * Predictive Cash-Flow Forecast & Maintenance Expense Burn-Rate Engine.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class VehicleFiscalBudgetWebController {

    private final VehicleFiscalBudgetService vehicleFiscalBudgetService;
    private final VehicleService vehicleService;
    private final AuthHelper authHelper;

    @GetMapping("/vehicles/{id}/fiscal-budget")
    public String viewVehicleFiscalBudget(
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        User currentUser = authHelper.getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }
        try {
            // Verify ownership first (throws AccessDeniedException if not owned or admin)
            Vehicle vehicle = vehicleService.getVehicleForUser(id, currentUser.getUserId());
            VehicleFiscalBudgetReportDTO report = vehicleFiscalBudgetService.getVehicleFiscalBudgetReport(
                    id, currentUser.getEmail()
            );

            model.addAttribute("vehicle", vehicle);
            model.addAttribute("userVehicles", vehicleService.getVehiclesForUser(currentUser.getUserId()));
            model.addAttribute("report", report);
            model.addAttribute("user", currentUser);

            return "vehicle/fiscal-budget";
        } catch (AccessDeniedException | IllegalArgumentException e) {
            log.warn("Access denied for user {} viewing fiscal budget of vehicle {}", currentUser.getUserId(), id);
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/vehicles";
        } catch (Exception e) {
            log.error("Error loading vehicle fiscal budget report for vehicle {}", id, e);
            redirectAttributes.addFlashAttribute("errorMsg", "Unable to compute vehicle fiscal budget: " + e.getMessage());
            return "redirect:/vehicles/" + id;
        }
    }

    @GetMapping("/vehicles/fiscal-budget")
    public String viewGarageFiscalBudget(
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        User currentUser = authHelper.getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }
        try {
            GarageFiscalBudgetMatrixDTO matrix = vehicleFiscalBudgetService.getGarageFiscalBudgetMatrix(
                    currentUser.getEmail()
            );

            model.addAttribute("matrix", matrix);
            model.addAttribute("user", currentUser);

            return "vehicle/garage-fiscal-budget";
        } catch (AccessDeniedException e) {
            log.warn("Access denied for user {} on garage fiscal budget", currentUser.getEmail());
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/vehicles";
        } catch (Exception e) {
            log.error("Error loading garage fiscal budget matrix for user {}", currentUser.getUserId(), e);
            redirectAttributes.addFlashAttribute("errorMsg", "Unable to compute garage fiscal budget: " + e.getMessage());
            return "redirect:/vehicles";
        }
    }
}
