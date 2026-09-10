package com.mygarage.controller.web;

import com.mygarage.config.AuthHelper;
import com.mygarage.dto.request.VehicleRequest;
import com.mygarage.model.User;
import com.mygarage.model.Vehicle;
import com.mygarage.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/vehicles")
@RequiredArgsConstructor
public class VehicleWebController {

    private final VehicleService vehicleService;
    private final VehicleCategoryService categoryService;
    private final ServiceRecordService serviceRecordService;
    private final FuelRecordService fuelRecordService;
    private final MaintenanceService maintenanceService;
    private final AuthHelper authHelper;

    @GetMapping
    public String listVehicles(@RequestParam(required = false) String search, Model model) {
        Long userId = authHelper.getCurrentUserId();
        model.addAttribute("vehicles", vehicleService.searchVehicles(userId, search));
        model.addAttribute("search", search);
        model.addAttribute("user", authHelper.getCurrentUser());
        return "vehicle/list";
    }

    @GetMapping("/add")
    public String addForm(Model model) {
        model.addAttribute("vehicleRequest", new VehicleRequest());
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("user", authHelper.getCurrentUser());
        return "vehicle/form";
    }

    @PostMapping("/add")
    public String addVehicle(@Valid @ModelAttribute("vehicleRequest") VehicleRequest request,
                             BindingResult result, RedirectAttributes ra, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("categories", categoryService.findAll());
            model.addAttribute("user", authHelper.getCurrentUser());
            return "vehicle/form";
        }
        try {
            Long userId = authHelper.getCurrentUserId();
            vehicleService.addVehicle(userId, request);
            ra.addFlashAttribute("successMsg", "Vehicle added to your garage!");
            return "redirect:/vehicles";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMsg", e.getMessage());
            model.addAttribute("categories", categoryService.findAll());
            model.addAttribute("user", authHelper.getCurrentUser());
            return "vehicle/form";
        }
    }

    @GetMapping("/{id}")
    public String viewVehicle(@PathVariable Long id, Model model) {
        Long userId = authHelper.getCurrentUserId();
        Vehicle vehicle = vehicleService.getVehicleForUser(id, userId);
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("userVehicles", vehicleService.getVehiclesForUser(userId));
        model.addAttribute("serviceRecords", serviceRecordService.getServiceRecordsForVehicle(id, userId));
        model.addAttribute("fuelRecords", fuelRecordService.getFuelRecordsForVehicle(id, userId));
        model.addAttribute("maintenanceRecords", maintenanceService.getMaintenanceForVehicle(id, userId));
        model.addAttribute("user", authHelper.getCurrentUser());
        return "vehicle/detail";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Long userId = authHelper.getCurrentUserId();
        Vehicle vehicle = vehicleService.getVehicleForUser(id, userId);
        VehicleRequest req = new VehicleRequest();
        req.setCategoryId(vehicle.getCategory().getCategoryId());
        req.setPlateNumber(vehicle.getPlateNumber());
        req.setMake(vehicle.getMake());
        req.setModel(vehicle.getModel());
        req.setYear(vehicle.getYear());
        req.setColor(vehicle.getColor());
        req.setFuelType(vehicle.getFuelType());
        req.setCurrentOdometer(vehicle.getCurrentOdometer());
        req.setNotes(vehicle.getNotes());
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("userVehicles", vehicleService.getVehiclesForUser(userId));
        model.addAttribute("vehicleRequest", req);
        model.addAttribute("vehicleId", id);
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("user", authHelper.getCurrentUser());
        return "vehicle/form";
    }

    @PostMapping("/{id}/edit")
    public String updateVehicle(@PathVariable Long id,
                                @Valid @ModelAttribute("vehicleRequest") VehicleRequest request,
                                BindingResult result, RedirectAttributes ra, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("vehicleId", id);
            model.addAttribute("categories", categoryService.findAll());
            model.addAttribute("user", authHelper.getCurrentUser());
            return "vehicle/form";
        }
        try {
            Long userId = authHelper.getCurrentUserId();
            vehicleService.updateVehicle(id, userId, request);
            ra.addFlashAttribute("successMsg", "Vehicle updated successfully.");
            return "redirect:/vehicles/" + id;
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMsg", e.getMessage());
            model.addAttribute("vehicleId", id);
            model.addAttribute("categories", categoryService.findAll());
            model.addAttribute("user", authHelper.getCurrentUser());
            return "vehicle/form";
        }
    }

    @PostMapping("/{id}/delete")
    public String deleteVehicle(@PathVariable Long id, RedirectAttributes ra) {
        try {
            Long userId = authHelper.getCurrentUserId();
            vehicleService.deleteVehicle(id, userId);
            ra.addFlashAttribute("successMsg", "Vehicle removed from your garage.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/vehicles";
    }

    @GetMapping("/{id}/timeline")
    public String timeline(@PathVariable Long id, Model model) {
        Long userId = authHelper.getCurrentUserId();
        Vehicle vehicle = vehicleService.getVehicleForUser(id, userId);
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("userVehicles", vehicleService.getVehiclesForUser(userId));
        model.addAttribute("serviceRecords", serviceRecordService.getServiceRecordsForVehicle(id, userId));
        model.addAttribute("fuelRecords", fuelRecordService.getFuelRecordsForVehicle(id, userId));
        model.addAttribute("maintenanceRecords", maintenanceService.getMaintenanceForVehicle(id, userId));
        model.addAttribute("user", authHelper.getCurrentUser());
        return "vehicle/timeline";
    }
}
