package com.mygarage.controller.api;

import com.mygarage.config.AuthHelper;
import com.mygarage.dto.request.ScheduleForecastRequestDTO;
import com.mygarage.dto.response.VehiclePredictiveReportDTO;
import com.mygarage.model.MaintenanceRecord;
import com.mygarage.service.PredictiveMaintenanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PredictiveMaintenanceApiController {

    private final PredictiveMaintenanceService predictiveMaintenanceService;
    private final AuthHelper authHelper;

    /**
     * REST endpoint: Get comprehensive predictive maintenance report for a vehicle.
     */
    @GetMapping("/vehicles/{id}/forecast")
    public ResponseEntity<VehiclePredictiveReportDTO> getVehicleForecast(@PathVariable Long id) {
        Long userId = authHelper.getCurrentUserId();
        return ResponseEntity.ok(predictiveMaintenanceService.getVehicleForecast(id, userId));
    }

    /**
     * REST endpoint: Get predictive maintenance reports for all user-owned vehicles.
     */
    @GetMapping("/analytics/garage-forecast")
    public ResponseEntity<List<VehiclePredictiveReportDTO>> getGarageFleetForecast() {
        Long userId = authHelper.getCurrentUserId();
        return ResponseEntity.ok(predictiveMaintenanceService.getGarageFleetForecast(userId));
    }

    /**
     * REST endpoint: Convert a predicted maintenance milestone into an active scheduled maintenance record.
     * Rejects duplicates with 409 Conflict or 400 Bad Request.
     */
    @PostMapping("/vehicles/{id}/forecast/schedule")
    public ResponseEntity<MaintenanceRecord> scheduleForecastedMilestone(
            @PathVariable Long id,
            @Valid @RequestBody ScheduleForecastRequestDTO request) {
        Long userId = authHelper.getCurrentUserId();
        MaintenanceRecord created = predictiveMaintenanceService.scheduleForecastedMilestone(id, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
