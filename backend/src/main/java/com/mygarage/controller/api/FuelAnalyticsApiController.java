package com.mygarage.controller.api;

import com.mygarage.dto.response.GarageFuelIntelligenceDTO;
import com.mygarage.dto.response.VehicleFuelAnalyticsDTO;
import com.mygarage.model.User;
import com.mygarage.service.FuelAnalyticsService;
import com.mygarage.config.AuthHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FuelAnalyticsApiController {

    private final FuelAnalyticsService fuelAnalyticsService;
    private final AuthHelper authHelper;

    @GetMapping("/vehicles/{id}/fuel-analytics")
    public ResponseEntity<?> getVehicleFuelAnalytics(@PathVariable Long id) {
        try {
            User currentUser = authHelper.getCurrentUser();
            VehicleFuelAnalyticsDTO report = fuelAnalyticsService.getVehicleFuelAnalytics(id, currentUser.getUserId());
            return ResponseEntity.ok(report);
        } catch (AccessDeniedException e) {
            log.warn("API Access denied for fuel analytics on vehicle {}", id);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("API error fetching fuel analytics for vehicle {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving fuel analytics");
        }
    }

    @GetMapping("/analytics/garage-fuel")
    public ResponseEntity<?> getGarageFuelIntelligence() {
        try {
            User currentUser = authHelper.getCurrentUser();
            GarageFuelIntelligenceDTO summary = fuelAnalyticsService.getGarageFuelIntelligence(currentUser.getUserId());
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            log.error("API error fetching garage fuel intelligence", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving garage fuel intelligence");
        }
    }
}
