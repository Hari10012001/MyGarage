package com.mygarage.controller.web;

import com.mygarage.config.AuthHelper;
import com.mygarage.dto.response.GarageWorkshopEcosystemMatrixDTO;
import com.mygarage.dto.response.WorkshopAnalyticsReportDTO;
import com.mygarage.exception.ResourceNotFoundException;
import com.mygarage.model.User;
import com.mygarage.model.enums.Role;
import com.mygarage.service.WorkshopAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * M21: Web MVC controller for Service Center Ecosystem,
 * Workshop Benchmarking & Vendor Cost Intelligence Engine.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class WorkshopAnalyticsWebController {

    private final WorkshopAnalyticsService workshopAnalyticsService;
    private final AuthHelper authHelper;

    @GetMapping("/workshops")
    public String viewWorkshopEcosystem(Model model, RedirectAttributes redirectAttributes) {
        User currentUser = authHelper.getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }
        if (currentUser.getRole() == Role.ADMIN) {
            log.warn("Admin user {} redirected away from workshop ecosystem", currentUser.getEmail());
            redirectAttributes.addFlashAttribute("errorMsg", "Admin users cannot access private workshop analytics.");
            return "redirect:/vehicles";
        }

        try {
            GarageWorkshopEcosystemMatrixDTO ecosystem = workshopAnalyticsService.getGarageWorkshopEcosystem(currentUser);
            model.addAttribute("ecosystem", ecosystem);
            model.addAttribute("user", currentUser);
            return "workshop/ecosystem";
        } catch (AccessDeniedException e) {
            log.warn("Access denied for user {} on workshop ecosystem: {}", currentUser.getEmail(), e.getMessage());
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/vehicles";
        } catch (Exception e) {
            log.error("Error loading workshop ecosystem for user {}", currentUser.getEmail(), e);
            redirectAttributes.addFlashAttribute("errorMsg", "Unable to load workshop ecosystem: " + e.getMessage());
            return "redirect:/vehicles";
        }
    }

    @GetMapping("/workshops/{workshopKey}")
    public String viewWorkshopDetail(
            @PathVariable String workshopKey,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        User currentUser = authHelper.getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }
        if (currentUser.getRole() == Role.ADMIN) {
            log.warn("Admin user {} redirected away from workshop detail", currentUser.getEmail());
            redirectAttributes.addFlashAttribute("errorMsg", "Admin users cannot access private workshop analytics.");
            return "redirect:/vehicles";
        }

        try {
            WorkshopAnalyticsReportDTO report = workshopAnalyticsService.getWorkshopDetail(currentUser, workshopKey);
            model.addAttribute("report", report);
            model.addAttribute("user", currentUser);
            return "workshop/detail";
        } catch (ResourceNotFoundException | AccessDeniedException e) {
            log.warn("Access denied or not found for user {} on workshop {}: {}", currentUser.getEmail(), workshopKey, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/workshops";
        } catch (Exception e) {
            log.error("Error loading workshop detail for key {} user {}", workshopKey, currentUser.getEmail(), e);
            redirectAttributes.addFlashAttribute("errorMsg", "Unable to load workshop intelligence: " + e.getMessage());
            return "redirect:/workshops";
        }
    }
}
