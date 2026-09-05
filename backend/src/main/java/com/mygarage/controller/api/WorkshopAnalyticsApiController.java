package com.mygarage.controller.api;

import com.mygarage.config.AuthHelper;
import com.mygarage.dto.response.GarageWorkshopEcosystemMatrixDTO;
import com.mygarage.dto.response.WorkshopAnalyticsReportDTO;
import com.mygarage.exception.ResourceNotFoundException;
import com.mygarage.model.User;
import com.mygarage.model.enums.Role;
import com.mygarage.service.WorkshopAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * M21: REST API controller for Service Center Ecosystem,
 * Workshop Benchmarking & Vendor Cost Intelligence Engine.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class WorkshopAnalyticsApiController {

    private final WorkshopAnalyticsService workshopAnalyticsService;
    private final AuthHelper authHelper;

    @GetMapping("/api/analytics/workshops")
    public ResponseEntity<?> getWorkshopEcosystem() {
        User currentUser = authHelper.getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication required");
        }
        if (currentUser.getRole() == Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Admin users cannot access private workshop analytics");
        }

        try {
            GarageWorkshopEcosystemMatrixDTO matrix = workshopAnalyticsService.getGarageWorkshopEcosystem(currentUser);
            return ResponseEntity.ok(matrix);
        } catch (AccessDeniedException e) {
            log.warn("API access denied for user {} on workshop ecosystem", currentUser.getEmail());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            log.error("API error computing workshop ecosystem matrix for user {}", currentUser.getEmail(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unable to compute workshop ecosystem: " + e.getMessage());
        }
    }

    @GetMapping("/api/analytics/workshops/{workshopKey}")
    public ResponseEntity<?> getWorkshopDetail(@PathVariable String workshopKey) {
        User currentUser = authHelper.getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication required");
        }
        if (currentUser.getRole() == Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Admin users cannot access private workshop analytics");
        }

        try {
            WorkshopAnalyticsReportDTO report = workshopAnalyticsService.getWorkshopDetail(currentUser, workshopKey);
            return ResponseEntity.ok(report);
        } catch (ResourceNotFoundException e) {
            log.warn("Workshop not found: {}", workshopKey);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (AccessDeniedException e) {
            log.warn("API access denied for user {} on workshop key {}", currentUser.getEmail(), workshopKey);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            log.error("API error fetching workshop detail for key {}", workshopKey, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unable to fetch workshop detail: " + e.getMessage());
        }
    }
}
