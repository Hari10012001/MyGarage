package com.mygarage.controller.api;

import com.mygarage.config.AuthHelper;
import com.mygarage.dto.response.GarageFiscalBudgetMatrixDTO;
import com.mygarage.dto.response.VehicleFiscalBudgetReportDTO;
import com.mygarage.model.User;
import com.mygarage.service.VehicleFiscalBudgetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * M20: REST API controller for Vehicle Operational Budgeting,
 * Predictive Cash-Flow Forecast & Maintenance Expense Burn-Rate Engine.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class VehicleFiscalBudgetApiController {

    private final VehicleFiscalBudgetService vehicleFiscalBudgetService;
    private final AuthHelper authHelper;

    @GetMapping("/api/vehicles/{id}/fiscal-budget")
    public ResponseEntity<?> getVehicleFiscalBudget(@PathVariable Long id) {
        User currentUser = authHelper.getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication required");
        }
        try {
            VehicleFiscalBudgetReportDTO report = vehicleFiscalBudgetService.getVehicleFiscalBudgetReport(
                    id, currentUser.getEmail()
            );
            return ResponseEntity.ok(report);
        } catch (AccessDeniedException | IllegalArgumentException e) {
            log.warn("API Access denied for user {} on vehicle fiscal budget ID {}", currentUser.getEmail(), id);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            log.error("API error computing vehicle fiscal budget for vehicle {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unable to compute vehicle fiscal budget: " + e.getMessage());
        }
    }

    @GetMapping("/api/analytics/garage-fiscal-budget")
    public ResponseEntity<?> getGarageFiscalBudget() {
        User currentUser = authHelper.getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication required");
        }
        try {
            GarageFiscalBudgetMatrixDTO matrix = vehicleFiscalBudgetService.getGarageFiscalBudgetMatrix(
                    currentUser.getEmail()
            );
            return ResponseEntity.ok(matrix);
        } catch (AccessDeniedException | IllegalArgumentException e) {
            log.warn("API Access denied for user {} on garage fiscal budget", currentUser.getEmail());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            log.error("API error computing garage fiscal budget for user {}", currentUser.getEmail(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unable to compute garage fiscal budget: " + e.getMessage());
        }
    }
}
