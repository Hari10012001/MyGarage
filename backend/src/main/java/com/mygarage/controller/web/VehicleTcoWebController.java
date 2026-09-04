package com.mygarage.controller.web;

import com.mygarage.config.AuthHelper;
import com.mygarage.dto.response.GarageTcoSummaryDTO;
import com.mygarage.dto.response.VehicleTcoReportDTO;
import com.mygarage.model.Vehicle;
import com.mygarage.service.VehicleService;
import com.mygarage.service.VehicleTcoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Slf4j
@Controller
@RequestMapping("/vehicles")
@RequiredArgsConstructor
public class VehicleTcoWebController {

    private final VehicleTcoService vehicleTcoService;
    private final VehicleService vehicleService;
    private final AuthHelper authHelper;

    /**
     * Web view for vehicle Total Cost of Ownership (TCO), residual valuation & replacement advisory.
     */
    @GetMapping("/{id}/tco")
    public String viewVehicleTco(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Long userId = authHelper.getCurrentUserId();
        try {
            VehicleTcoReportDTO report = vehicleTcoService.calculateVehicleTco(id, userId);
            List<Vehicle> userVehicles = vehicleService.getVehiclesForUser(userId);

            model.addAttribute("report", report);
            model.addAttribute("vehicle", vehicleService.getVehicleForUser(id, userId));
            model.addAttribute("userVehicles", userVehicles);
            return "vehicle/tco";
        } catch (AccessDeniedException ex) {
            log.warn("Access denied for user {} viewing TCO of vehicle {}: {}", userId, id, ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Access Denied: You do not have permission to view that vehicle.");
            return "redirect:/vehicles";
        } catch (Exception ex) {
            log.error("Error generating TCO report for vehicle {}: {}", id, ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/vehicles";
        }
    }

    /**
     * Web dispatcher for TCO & Asset Advisory. Redirects to user's first vehicle.
     */
    @GetMapping("/tco")
    public String redirectToFirstVehicleTco(RedirectAttributes redirectAttributes) {
        Long userId = authHelper.getCurrentUserId();
        List<Vehicle> vehicles = vehicleService.getVehiclesForUser(userId);
        if (vehicles.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "No vehicles found in your garage. Please register a vehicle first.");
            return "redirect:/vehicles";
        }
        return "redirect:/vehicles/" + vehicles.get(0).getVehicleId() + "/tco";
    }
}
