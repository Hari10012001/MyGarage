package com.mygarage.controller.api;

import com.mygarage.config.AuthHelper;
import com.mygarage.dto.request.MaintenanceRequest;
import com.mygarage.model.MaintenanceRecord;
import com.mygarage.service.MaintenanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MaintenanceApiController {

    private final MaintenanceService maintenanceService;
    private final AuthHelper authHelper;

    @GetMapping("/vehicles/{vehicleId}/maintenance")
    public ResponseEntity<List<MaintenanceRecord>> getMaintenance(@PathVariable Long vehicleId) {
        Long userId = authHelper.getCurrentUserId();
        return ResponseEntity.ok(maintenanceService.getMaintenanceForVehicle(vehicleId, userId));
    }

    @PostMapping("/vehicles/{vehicleId}/maintenance")
    public ResponseEntity<MaintenanceRecord> addMaintenance(@PathVariable Long vehicleId,
                                                             @Valid @RequestBody MaintenanceRequest request) {
        Long userId = authHelper.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(maintenanceService.addMaintenanceRecord(vehicleId, userId, request));
    }

    @GetMapping("/maintenance/{maintenanceId}")
    public ResponseEntity<MaintenanceRecord> getMaintenanceById(@PathVariable Long maintenanceId) {
        Long userId = authHelper.getCurrentUserId();
        return ResponseEntity.ok(maintenanceService.getMaintenanceRecord(maintenanceId, userId));
    }

    @GetMapping("/vehicles/{vehicleId}/maintenance/{maintenanceId}")
    public ResponseEntity<MaintenanceRecord> getMaintenanceForVehicle(
            @PathVariable Long vehicleId,
            @PathVariable Long maintenanceId) {
        Long userId = authHelper.getCurrentUserId();
        return ResponseEntity.ok(maintenanceService.getMaintenanceRecordForVehicle(maintenanceId, vehicleId, userId));
    }

    @PutMapping("/maintenance/{maintenanceId}")
    public ResponseEntity<MaintenanceRecord> updateMaintenance(@PathVariable Long maintenanceId,
                                                                @Valid @RequestBody MaintenanceRequest request) {
        Long userId = authHelper.getCurrentUserId();
        return ResponseEntity.ok(maintenanceService.updateMaintenanceRecord(maintenanceId, userId, request));
    }

    @PutMapping("/vehicles/{vehicleId}/maintenance/{maintenanceId}")
    public ResponseEntity<MaintenanceRecord> updateMaintenanceForVehicle(
            @PathVariable Long vehicleId,
            @PathVariable Long maintenanceId,
            @Valid @RequestBody MaintenanceRequest request) {
        Long userId = authHelper.getCurrentUserId();
        return ResponseEntity.ok(maintenanceService.updateMaintenanceRecordForVehicle(maintenanceId, vehicleId, userId, request));
    }

    @PatchMapping("/maintenance/{maintenanceId}/complete")
    public ResponseEntity<MaintenanceRecord> patchMarkCompleted(
            @PathVariable Long maintenanceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate completedDate) {
        Long userId = authHelper.getCurrentUserId();
        return ResponseEntity.ok(maintenanceService.markCompleted(maintenanceId, userId, completedDate));
    }

    @PostMapping("/maintenance/{maintenanceId}/complete")
    public ResponseEntity<MaintenanceRecord> postMarkCompleted(
            @PathVariable Long maintenanceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate completedDate) {
        Long userId = authHelper.getCurrentUserId();
        return ResponseEntity.ok(maintenanceService.markCompleted(maintenanceId, userId, completedDate));
    }

    @PatchMapping("/vehicles/{vehicleId}/maintenance/{maintenanceId}/complete")
    public ResponseEntity<MaintenanceRecord> patchMarkCompletedForVehicle(
            @PathVariable Long vehicleId,
            @PathVariable Long maintenanceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate completedDate) {
        Long userId = authHelper.getCurrentUserId();
        return ResponseEntity.ok(maintenanceService.markCompletedForVehicle(maintenanceId, vehicleId, userId, completedDate));
    }

    @PostMapping("/vehicles/{vehicleId}/maintenance/{maintenanceId}/complete")
    public ResponseEntity<MaintenanceRecord> postMarkCompletedForVehicle(
            @PathVariable Long vehicleId,
            @PathVariable Long maintenanceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate completedDate) {
        Long userId = authHelper.getCurrentUserId();
        return ResponseEntity.ok(maintenanceService.markCompletedForVehicle(maintenanceId, vehicleId, userId, completedDate));
    }

    @DeleteMapping("/maintenance/{maintenanceId}")
    public ResponseEntity<Map<String, String>> deleteMaintenance(@PathVariable Long maintenanceId) {
        Long userId = authHelper.getCurrentUserId();
        maintenanceService.deleteMaintenanceRecord(maintenanceId, userId);
        return ResponseEntity.ok(Map.of("message", "Maintenance record deleted."));
    }

    @DeleteMapping("/vehicles/{vehicleId}/maintenance/{maintenanceId}")
    public ResponseEntity<Map<String, String>> deleteMaintenanceForVehicle(
            @PathVariable Long vehicleId,
            @PathVariable Long maintenanceId) {
        Long userId = authHelper.getCurrentUserId();
        maintenanceService.deleteMaintenanceRecordForVehicle(maintenanceId, vehicleId, userId);
        return ResponseEntity.ok(Map.of("message", "Maintenance record deleted."));
    }
}
