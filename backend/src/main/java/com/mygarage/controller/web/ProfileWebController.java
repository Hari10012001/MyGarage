package com.mygarage.controller.web;

import com.mygarage.config.AuthHelper;
import com.mygarage.dto.request.UpdateProfileRequest;
import com.mygarage.model.User;
import com.mygarage.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileWebController {

    private final UserService userService;
    private final AuthHelper authHelper;

    @GetMapping
    public String profilePage(Model model) {
        User user = authHelper.getCurrentUser();
        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setFullName(user.getFullName());
        req.setPhone(user.getPhone());
        model.addAttribute("user", user);
        model.addAttribute("profileRequest", req);
        return "profile/index";
    }

    @PostMapping("/update")
    public String updateProfile(@Valid @ModelAttribute("profileRequest") UpdateProfileRequest request,
                                BindingResult result, RedirectAttributes ra, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("user", authHelper.getCurrentUser());
            return "profile/index";
        }
        Long userId = authHelper.getCurrentUserId();
        userService.updateProfile(userId, request);
        ra.addFlashAttribute("successMsg", "Profile updated successfully.");
        return "redirect:/profile";
    }

    @PostMapping("/change-password")
    public String changePassword(@RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmNewPassword,
                                 RedirectAttributes ra, Model model) {
        if (!newPassword.equals(confirmNewPassword)) {
            ra.addFlashAttribute("passwordError", "New passwords do not match.");
            return "redirect:/profile";
        }
        if (newPassword.length() < 6) {
            ra.addFlashAttribute("passwordError", "New password must be at least 6 characters.");
            return "redirect:/profile";
        }
        try {
            Long userId = authHelper.getCurrentUserId();
            userService.changePassword(userId, currentPassword, newPassword);
            ra.addFlashAttribute("passwordSuccess", "Password changed successfully.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("passwordError", e.getMessage());
        }
        return "redirect:/profile";
    }
}
