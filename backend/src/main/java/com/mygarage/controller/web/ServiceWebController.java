package com.mygarage.controller.web;

import com.mygarage.config.AuthHelper;
import com.mygarage.dto.request.ServiceRecordRequest;
import com.mygarage.model.Vehicle;
import com.mygarage.service.ServiceRecordService;
import com.mygarage.service.VehicleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/vehicles/{vehicleId}/services")
@RequiredArgsConstructor
public class ServiceWebController {

    private final ServiceRecordService serviceRecordService;
    private final VehicleService vehicleService;
    private final AuthHelper authHelper;

    @GetMapping("/add")
    public String addForm(@PathVariable Long vehicleId, Model model) {
        Long userId = authHelper.getCurrentUserId();
        Vehicle vehicle = vehicleService.getVehicleForUser(vehicleId, userId);
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("serviceRequest", new ServiceRecordRequest());
        model.addAttribute("user", authHelper.getCurrentUser());
        return "service/form";
    }

    @PostMapping("/add")
    public String addService(@PathVariable Long vehicleId,
                             @Valid @ModelAttribute("serviceRequest") ServiceRecordRequest request,
                             BindingResult result, RedirectAttributes ra, Model model) {
        Long userId = authHelper.getCurrentUserId();
        if (result.hasErrors()) {
            model.addAttribute("vehicle", vehicleService.getVehicleForUser(vehicleId, userId));
            model.addAttribute("user", authHelper.getCurrentUser());
            return "service/form";
        }
        try {
            serviceRecordService.addServiceRecord(vehicleId, userId, request);
            ra.addFlashAttribute("successMsg", "Service record saved!");
            return "redirect:/vehicles/" + vehicleId + "?tab=service";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMsg", e.getMessage());
            model.addAttribute("vehicle", vehicleService.getVehicleForUser(vehicleId, userId));
            model.addAttribute("user", authHelper.getCurrentUser());
            return "service/form";
        }
    }

    @GetMapping("/{serviceId}/edit")
    public String editForm(@PathVariable Long vehicleId, @PathVariable Long serviceId, Model model) {
        Long userId = authHelper.getCurrentUserId();
        var record = serviceRecordService.getServiceRecord(serviceId, userId);
        ServiceRecordRequest req = new ServiceRecordRequest();
        req.setServiceDate(record.getServiceDate());
        req.setServiceType(record.getServiceType());
        req.setDescription(record.getDescription());
        req.setGarageName(record.getGarageName());
        req.setCost(record.getCost());
        req.setOdometerAtService(record.getOdometerAtService());
        req.setNextServiceDueDate(record.getNextServiceDueDate());
        req.setNotes(record.getNotes());
        model.addAttribute("serviceRequest", req);
        model.addAttribute("serviceId", serviceId);
        model.addAttribute("vehicle", vehicleService.getVehicleForUser(vehicleId, userId));
        model.addAttribute("user", authHelper.getCurrentUser());
        return "service/form";
    }

    @PostMapping("/{serviceId}/edit")
    public String updateService(@PathVariable Long vehicleId, @PathVariable Long serviceId,
                                @Valid @ModelAttribute("serviceRequest") ServiceRecordRequest request,
                                BindingResult result, RedirectAttributes ra, Model model) {
        Long userId = authHelper.getCurrentUserId();
        if (result.hasErrors()) {
            model.addAttribute("serviceId", serviceId);
            model.addAttribute("vehicle", vehicleService.getVehicleForUser(vehicleId, userId));
            model.addAttribute("user", authHelper.getCurrentUser());
            return "service/form";
        }
        serviceRecordService.updateServiceRecord(serviceId, userId, request);
        ra.addFlashAttribute("successMsg", "Service record updated.");
        return "redirect:/vehicles/" + vehicleId + "?tab=service";
    }

    @PostMapping("/{serviceId}/delete")
    public String deleteService(@PathVariable Long vehicleId, @PathVariable Long serviceId,
                                RedirectAttributes ra) {
        Long userId = authHelper.getCurrentUserId();
        serviceRecordService.deleteServiceRecord(serviceId, userId);
        ra.addFlashAttribute("successMsg", "Service record deleted.");
        return "redirect:/vehicles/" + vehicleId + "?tab=service";
    }
}
