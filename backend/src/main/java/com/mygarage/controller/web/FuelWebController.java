package com.mygarage.controller.web;

import com.mygarage.config.AuthHelper;
import com.mygarage.dto.request.FuelRecordRequest;
import com.mygarage.model.Vehicle;
import com.mygarage.model.enums.FuelType;
import com.mygarage.service.FuelRecordService;
import com.mygarage.service.VehicleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/vehicles/{vehicleId}/fuel")
@RequiredArgsConstructor
public class FuelWebController {

    private final FuelRecordService fuelRecordService;
    private final VehicleService vehicleService;
    private final AuthHelper authHelper;

    @GetMapping("/add")
    public String addForm(@PathVariable Long vehicleId, Model model) {
        Long userId = authHelper.getCurrentUserId();
        Vehicle vehicle = vehicleService.getVehicleForUser(vehicleId, userId);
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("fuelRequest", new FuelRecordRequest());
        model.addAttribute("fuelTypes", FuelType.values());
        model.addAttribute("user", authHelper.getCurrentUser());
        return "fuel/form";
    }

    @PostMapping("/add")
    public String addFuel(@PathVariable Long vehicleId,
                          @Valid @ModelAttribute("fuelRequest") FuelRecordRequest request,
                          BindingResult result, RedirectAttributes ra, Model model) {
        Long userId = authHelper.getCurrentUserId();
        if (result.hasErrors()) {
            model.addAttribute("vehicle", vehicleService.getVehicleForUser(vehicleId, userId));
            model.addAttribute("fuelTypes", FuelType.values());
            model.addAttribute("user", authHelper.getCurrentUser());
            return "fuel/form";
        }
        try {
            fuelRecordService.addFuelRecord(vehicleId, userId, request);
            ra.addFlashAttribute("successMsg", "Fuel record saved!");
            return "redirect:/vehicles/" + vehicleId + "?tab=fuel";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMsg", e.getMessage());
            model.addAttribute("vehicle", vehicleService.getVehicleForUser(vehicleId, userId));
            model.addAttribute("fuelTypes", FuelType.values());
            model.addAttribute("user", authHelper.getCurrentUser());
            return "fuel/form";
        }
    }

    @GetMapping("/{fuelId}/edit")
    public String editForm(@PathVariable Long vehicleId, @PathVariable Long fuelId, Model model) {
        Long userId = authHelper.getCurrentUserId();
        var record = fuelRecordService.getFuelRecord(fuelId, userId);
        FuelRecordRequest req = new FuelRecordRequest();
        req.setFuelDate(record.getFuelDate());
        req.setFuelType(record.getFuelType());
        req.setQuantityLitres(record.getQuantityLitres());
        req.setCostPerLitre(record.getCostPerLitre());
        req.setOdometerAtFill(record.getOdometerAtFill());
        req.setNotes(record.getNotes());
        model.addAttribute("fuelRequest", req);
        model.addAttribute("fuelId", fuelId);
        model.addAttribute("vehicle", vehicleService.getVehicleForUser(vehicleId, userId));
        model.addAttribute("fuelTypes", FuelType.values());
        model.addAttribute("user", authHelper.getCurrentUser());
        return "fuel/form";
    }

    @PostMapping("/{fuelId}/edit")
    public String updateFuel(@PathVariable Long vehicleId, @PathVariable Long fuelId,
                             @Valid @ModelAttribute("fuelRequest") FuelRecordRequest request,
                             BindingResult result, RedirectAttributes ra, Model model) {
        Long userId = authHelper.getCurrentUserId();
        if (result.hasErrors()) {
            model.addAttribute("fuelId", fuelId);
            model.addAttribute("vehicle", vehicleService.getVehicleForUser(vehicleId, userId));
            model.addAttribute("fuelTypes", FuelType.values());
            model.addAttribute("user", authHelper.getCurrentUser());
            return "fuel/form";
        }
        fuelRecordService.updateFuelRecord(fuelId, userId, request);
        ra.addFlashAttribute("successMsg", "Fuel record updated.");
        return "redirect:/vehicles/" + vehicleId + "?tab=fuel";
    }

    @PostMapping("/{fuelId}/delete")
    public String deleteFuel(@PathVariable Long vehicleId, @PathVariable Long fuelId,
                             RedirectAttributes ra) {
        Long userId = authHelper.getCurrentUserId();
        fuelRecordService.deleteFuelRecord(fuelId, userId);
        ra.addFlashAttribute("successMsg", "Fuel record deleted.");
        return "redirect:/vehicles/" + vehicleId + "?tab=fuel";
    }
}
