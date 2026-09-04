package com.mygarage.controller.web;

import com.mygarage.config.AuthHelper;
import com.mygarage.dto.response.FleetComparisonReportDTO;
import com.mygarage.dto.response.FleetExpenseBreakdownDTO;
import com.mygarage.model.User;
import com.mygarage.model.Vehicle;
import com.mygarage.service.VehicleComparisonService;
import com.mygarage.service.VehicleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Slf4j
@Controller
@RequestMapping("/vehicles/compare")
@RequiredArgsConstructor
public class VehicleComparisonWebController {

    private final VehicleComparisonService vehicleComparisonService;
    private final VehicleService vehicleService;
    private final AuthHelper authHelper;

    @GetMapping
    public String showComparePage(@RequestParam(name = "vehicleIds", required = false) List<Long> vehicleIds,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        User user = authHelper.getCurrentUser();
        List<Vehicle> ownedVehicles = vehicleService.getVehiclesForUser(user.getUserId());
        model.addAttribute("ownedVehicles", ownedVehicles);
        model.addAttribute("user", user);

        FleetExpenseBreakdownDTO breakdown = vehicleComparisonService.getFleetExpenseBreakdown(user.getUserId());
        model.addAttribute("fleetBreakdown", breakdown);

        if (vehicleIds != null && !vehicleIds.isEmpty()) {
            try {
                FleetComparisonReportDTO report = vehicleComparisonService.compareVehicles(vehicleIds, user.getUserId());
                model.addAttribute("report", report);
                model.addAttribute("selectedVehicleIds", vehicleIds);
            } catch (AccessDeniedException ade) {
                log.warn("AccessDenied during vehicle comparison web request: {}", ade.getMessage());
                redirectAttributes.addFlashAttribute("errorMessage", ade.getMessage());
                return "redirect:/vehicles/compare";
            } catch (IllegalArgumentException ex) {
                model.addAttribute("errorMessage", ex.getMessage());
                model.addAttribute("selectedVehicleIds", vehicleIds);
            }
        }

        return "vehicle/compare";
    }
}
