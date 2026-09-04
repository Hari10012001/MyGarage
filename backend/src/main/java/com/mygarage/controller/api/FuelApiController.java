package com.mygarage.controller.api;

import com.mygarage.config.AuthHelper;
import com.mygarage.dto.request.FuelRecordRequest;
import com.mygarage.model.FuelRecord;
import com.mygarage.service.FuelRecordService;
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
public class FuelApiController {

    private final FuelRecordService fuelRecordService;
    private final AuthHelper authHelper;

    @GetMapping("/fuel")
    public ResponseEntity<List<FuelRecord>> getAllFuelRecords() {
        Long userId = authHelper.getCurrentUserId();
        return ResponseEntity.ok(fuelRecordService.getAllFuelForUser(userId));
    }

    @GetMapping("/vehicles/{vehicleId}/fuel")
    public ResponseEntity<List<FuelRecord>> getFuelRecords(
            @PathVariable Long vehicleId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        Long userId = authHelper.getCurrentUserId();
        return ResponseEntity.ok(fuelRecordService.getFuelRecordsFiltered(vehicleId, userId, from, to));
    }

    @PostMapping("/vehicles/{vehicleId}/fuel")
    public ResponseEntity<FuelRecord> addFuel(@PathVariable Long vehicleId,
                                               @Valid @RequestBody FuelRecordRequest request) {
        Long userId = authHelper.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(fuelRecordService.addFuelRecord(vehicleId, userId, request));
    }

    @GetMapping("/fuel/{fuelId}")
    public ResponseEntity<FuelRecord> getFuelById(@PathVariable Long fuelId) {
        Long userId = authHelper.getCurrentUserId();
        return ResponseEntity.ok(fuelRecordService.getFuelRecord(fuelId, userId));
    }

    @GetMapping("/vehicles/{vehicleId}/fuel/{fuelId}")
    public ResponseEntity<FuelRecord> getFuelForVehicle(
            @PathVariable Long vehicleId,
            @PathVariable Long fuelId) {
        Long userId = authHelper.getCurrentUserId();
        return ResponseEntity.ok(fuelRecordService.getFuelRecordForVehicle(fuelId, vehicleId, userId));
    }

    @PutMapping("/fuel/{fuelId}")
    public ResponseEntity<FuelRecord> updateFuel(@PathVariable Long fuelId,
                                                  @Valid @RequestBody FuelRecordRequest request) {
        Long userId = authHelper.getCurrentUserId();
        return ResponseEntity.ok(fuelRecordService.updateFuelRecord(fuelId, userId, request));
    }

    @PutMapping("/vehicles/{vehicleId}/fuel/{fuelId}")
    public ResponseEntity<FuelRecord> updateFuelForVehicle(
            @PathVariable Long vehicleId,
            @PathVariable Long fuelId,
            @Valid @RequestBody FuelRecordRequest request) {
        Long userId = authHelper.getCurrentUserId();
        return ResponseEntity.ok(fuelRecordService.updateFuelRecordForVehicle(fuelId, vehicleId, userId, request));
    }

    @DeleteMapping("/fuel/{fuelId}")
    public ResponseEntity<Map<String, String>> deleteFuel(@PathVariable Long fuelId) {
        Long userId = authHelper.getCurrentUserId();
        fuelRecordService.deleteFuelRecord(fuelId, userId);
        return ResponseEntity.ok(Map.of("message", "Fuel record deleted."));
    }

    @DeleteMapping("/vehicles/{vehicleId}/fuel/{fuelId}")
    public ResponseEntity<Map<String, String>> deleteFuelForVehicle(
            @PathVariable Long vehicleId,
            @PathVariable Long fuelId) {
        Long userId = authHelper.getCurrentUserId();
        fuelRecordService.deleteFuelRecordForVehicle(fuelId, vehicleId, userId);
        return ResponseEntity.ok(Map.of("message", "Fuel record deleted."));
    }
}
