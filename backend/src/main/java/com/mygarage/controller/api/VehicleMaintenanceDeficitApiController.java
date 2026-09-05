package com.mygarage.controller.api;

import com.mygarage.config.AuthHelper;
import com.mygarage.dto.response.GarageMaintenanceDeficitMatrixDTO;
import com.mygarage.dto.response.VehicleMaintenanceDeficitReportDTO;
import com.mygarage.model.User;
import com.mygarage.service.VehicleMaintenanceDeficitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class VehicleMaintenanceDeficitApiController {

    private final VehicleMaintenanceDeficitService vehicleMaintenanceDeficitService;
    private final AuthHelper authHelper;

    @GetMapping("/api/vehicles/{id}/maintenance-deficit")
    public ResponseEntity<?> getVehicleMaintenanceDeficit(@PathVariable Long id) {
        User currentUser = authHelper.getCurrentUser();
        try {
            VehicleMaintenanceDeficitReportDTO report = vehicleMaintenanceDeficitService.evaluateVehicleDeficit(
                    id, currentUser.getUserId()
            );
            return ResponseEntity.ok(report);
        } catch (AccessDeniedException | IllegalArgumentException e) {
            log.warn("API Access denied for user {} evaluating maintenance deficit on vehicle {}", currentUser.getUserId(), id);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            log.error("API error evaluating maintenance deficit for vehicle {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unable to evaluate maintenance deficit: " + e.getMessage());
        }
    }

    @GetMapping("/api/analytics/garage-maintenance-deficit")
    public ResponseEntity<?> getGarageMaintenanceDeficit() {
        User currentUser = authHelper.getCurrentUser();
        try {
            GarageMaintenanceDeficitMatrixDTO matrix = vehicleMaintenanceDeficitService.evaluateGarageDeficit(
                    currentUser.getUserId()
            );
            return ResponseEntity.ok(matrix);
        } catch (Exception e) {
            log.error("API error evaluating garage maintenance deficit for user {}", currentUser.getUserId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unable to evaluate garage maintenance deficit: " + e.getMessage());
        }
    }
}
