package com.mygarage.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Global exception handler for Thymeleaf (web) controllers.
 * Shows user-friendly error pages. Never exposes stack traces.
 */
@ControllerAdvice(basePackages = "com.mygarage.controller.web")
public class GlobalWebExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleBadRequest(IllegalArgumentException ex, Model model) {
        model.addAttribute("errorTitle", "Invalid Request");
        model.addAttribute("errorMessage", ex.getMessage());
        return "error/error";
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleAccessDenied(AccessDeniedException ex, Model model) {
        model.addAttribute("errorTitle", "Access Denied");
        model.addAttribute("errorMessage", "You do not have permission to access this page.");
        return "error/error";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGeneral(Exception ex, Model model, HttpServletRequest request) {
        // Log internally
        System.err.println("Web error at " + request.getRequestURI() + ": " + ex.getMessage());
        model.addAttribute("errorTitle", "Something went wrong");
        model.addAttribute("errorMessage", "An unexpected error occurred. Please go back and try again.");
        return "error/error";
    }
}
