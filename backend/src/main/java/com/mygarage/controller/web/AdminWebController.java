package com.mygarage.controller.web;

import com.mygarage.config.AuthHelper;
import com.mygarage.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminWebController {

    private final UserService userService;
    private final VehicleCategoryService categoryService;
    private final DashboardService dashboardService;
    private final AuthHelper authHelper;

    @GetMapping("/dashboard")
    public String adminDashboard(Model model) {
        model.addAttribute("user", authHelper.getCurrentUser());
        model.addAttribute("totalUsers", dashboardService.getAdminTotalUsers());
        model.addAttribute("totalVehicles", dashboardService.getAdminTotalVehicles());
        model.addAttribute("totalServiceRecords", dashboardService.getAdminTotalServiceRecords());
        model.addAttribute("totalFuelRecords", dashboardService.getAdminTotalFuelRecords());
        model.addAttribute("totalMaintenanceRecords", dashboardService.getAdminTotalMaintenanceRecords());
        model.addAttribute("recentUsers", userService.findAllUsers().stream().limit(5).toList());
        model.addAttribute("categories", categoryService.findAll());
        return "admin/dashboard";
    }

    @GetMapping("/users")
    public String manageUsers(@RequestParam(required = false) String search, Model model) {
        model.addAttribute("user", authHelper.getCurrentUser());
        model.addAttribute("users", userService.findAllUsers());
        return "admin/users";
    }

    @PostMapping("/users/{userId}/toggle")
    public String toggleUser(@PathVariable Long userId, RedirectAttributes ra) {
        try {
            userService.toggleUserStatus(userId);
            ra.addFlashAttribute("successMsg", "User status updated.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/categories")
    public String manageCategories(Model model) {
        model.addAttribute("user", authHelper.getCurrentUser());
        model.addAttribute("categories", categoryService.findAll());
        return "admin/categories";
    }

    @PostMapping("/categories/add")
    public String addCategory(@RequestParam String name,
                              @RequestParam(required = false) String icon,
                              @RequestParam(required = false) String description,
                              RedirectAttributes ra) {
        try {
            categoryService.save(name, icon, description);
            ra.addFlashAttribute("successMsg", "Category '" + name + "' added.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    @PostMapping("/categories/{id}/edit")
    public String editCategory(@PathVariable Long id,
                               @RequestParam String name,
                               @RequestParam(required = false) String icon,
                               @RequestParam(required = false) String description,
                               RedirectAttributes ra) {
        try {
            categoryService.update(id, name, icon, description);
            ra.addFlashAttribute("successMsg", "Category updated.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    @PostMapping("/categories/{id}/delete")
    public String deleteCategory(@PathVariable Long id, RedirectAttributes ra) {
        try {
            categoryService.delete(id);
            ra.addFlashAttribute("successMsg", "Category deleted.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    @GetMapping("/statistics")
    public String statistics(Model model) {
        model.addAttribute("user", authHelper.getCurrentUser());
        model.addAttribute("totalUsers", dashboardService.getAdminTotalUsers());
        model.addAttribute("totalVehicles", dashboardService.getAdminTotalVehicles());
        model.addAttribute("totalServiceRecords", dashboardService.getAdminTotalServiceRecords());
        model.addAttribute("totalFuelRecords", dashboardService.getAdminTotalFuelRecords());
        model.addAttribute("totalMaintenanceRecords", dashboardService.getAdminTotalMaintenanceRecords());
        model.addAttribute("categories", categoryService.findAll());
        return "admin/statistics";
    }
}
