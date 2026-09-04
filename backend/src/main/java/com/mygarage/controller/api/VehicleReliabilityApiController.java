package com.mygarage.controller.api;

import com.mygarage.config.AuthHelper;
import com.mygarage.dto.response.GarageReliabilityMatrixDTO;
import com.mygarage.dto.response.VehicleReliabilityReportDTO;
import com.mygarage.model.User;
import com.mygarage.service.VehicleReliabilityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
public class VehicleReliabilityApiController {

    private final VehicleReliabilityService vehicleReliabilityService;
    private final AuthHelper authHelper;

    @GetMapping("/api/vehicles/{id}/reliability")
    public ResponseEntity<?> getVehicleReliability(@PathVariable Long id) {
        User currentUser = authHelper.getCurrentUser();
        try {
            VehicleReliabilityReportDTO report = vehicleReliabilityService.getVehicleReliability(id, currentUser.getUserId());
            return ResponseEntity.ok(report);
        } catch (AccessDeniedException | IllegalArgumentException e) {
            log.warn("API Access denied for user {} viewing reliability on vehicle {}", currentUser.getUserId(), id);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            log.error("API error loading reliability for vehicle {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unable to process reliability analytics: " + e.getMessage());
        }
    }

    @GetMapping("/api/analytics/garage-reliability")
    public ResponseEntity<?> getGarageReliability() {
        User currentUser = authHelper.getCurrentUser();
        try {
            GarageReliabilityMatrixDTO matrix = vehicleReliabilityService.getGarageReliabilityMatrix(currentUser.getUserId());
            return ResponseEntity.ok(matrix);
        } catch (Exception e) {
            log.error("API error loading garage reliability for user {}", currentUser.getUserId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unable to load garage reliability: " + e.getMessage());
        }
    }
}
