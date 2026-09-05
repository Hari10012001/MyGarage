package com.mygarage.controller.api;

import com.mygarage.config.AuthHelper;
import com.mygarage.dto.response.GarageFleetDispatchReportDTO;
import com.mygarage.dto.response.VehicleTripReadinessReportDTO;
import com.mygarage.model.User;
import com.mygarage.service.VehicleReadinessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class VehicleReadinessApiController {

    private final VehicleReadinessService vehicleReadinessService;
    private final AuthHelper authHelper;

    @GetMapping("/api/vehicles/{id}/readiness")
    public ResponseEntity<?> getVehicleReadiness(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "500.0") Double tripDistanceKm,
            @RequestParam(required = false, defaultValue = "2") Integer tripDays,
            @RequestParam(required = false, defaultValue = "HIGHWAY_CRUISE") String drivingRegime
    ) {
        User currentUser = authHelper.getCurrentUser();
        try {
            VehicleTripReadinessReportDTO report = vehicleReadinessService.evaluateVehicleReadiness(
                    id, tripDistanceKm, tripDays, drivingRegime, currentUser.getUserId()
            );
            return ResponseEntity.ok(report);
        } catch (AccessDeniedException | IllegalArgumentException e) {
            log.warn("API Access denied for user {} evaluating readiness on vehicle {}", currentUser.getUserId(), id);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            log.error("API error evaluating readiness for vehicle {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unable to evaluate vehicle readiness: " + e.getMessage());
        }
    }

    @GetMapping("/api/analytics/garage-dispatch")
    public ResponseEntity<?> getGarageFleetDispatch(
            @RequestParam(required = false, defaultValue = "500.0") Double tripDistanceKm,
            @RequestParam(required = false, defaultValue = "2") Integer tripDays,
            @RequestParam(required = false, defaultValue = "HIGHWAY_CRUISE") String drivingRegime
    ) {
        User currentUser = authHelper.getCurrentUser();
        try {
            GarageFleetDispatchReportDTO report = vehicleReadinessService.evaluateFleetDispatch(
                    tripDistanceKm, tripDays, drivingRegime, currentUser.getUserId()
            );
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            log.error("API error evaluating fleet dispatch for user {}", currentUser.getUserId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unable to evaluate fleet dispatch: " + e.getMessage());
        }
    }
}
