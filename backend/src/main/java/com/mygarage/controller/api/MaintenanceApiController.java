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

    @PutMapping("/maintenance/{maintenanceId}")
    public ResponseEntity<MaintenanceRecord> updateMaintenance(@PathVariable Long maintenanceId,
                                                                @Valid @RequestBody MaintenanceRequest request) {
        Long userId = authHelper.getCurrentUserId();
        return ResponseEntity.ok(maintenanceService.updateMaintenanceRecord(maintenanceId, userId, request));
    }

    @PatchMapping("/maintenance/{maintenanceId}/complete")
    public ResponseEntity<MaintenanceRecord> markCompleted(
            @PathVariable Long maintenanceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate completedDate) {
        Long userId = authHelper.getCurrentUserId();
        return ResponseEntity.ok(maintenanceService.markCompleted(maintenanceId, userId, completedDate));
    }

    @DeleteMapping("/maintenance/{maintenanceId}")
    public ResponseEntity<Map<String, String>> deleteMaintenance(@PathVariable Long maintenanceId) {
        Long userId = authHelper.getCurrentUserId();
        maintenanceService.deleteMaintenanceRecord(maintenanceId, userId);
        return ResponseEntity.ok(Map.of("message", "Maintenance record deleted."));
    }
}
