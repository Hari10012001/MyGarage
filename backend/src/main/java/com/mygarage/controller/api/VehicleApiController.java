package com.mygarage.controller.api;

import com.mygarage.config.AuthHelper;
import com.mygarage.dto.request.VehicleRequest;
import com.mygarage.model.Vehicle;
import com.mygarage.service.VehicleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
public class VehicleApiController {

    private final VehicleService vehicleService;
    private final AuthHelper authHelper;

    @GetMapping
    public ResponseEntity<List<Vehicle>> getVehicles(@RequestParam(required = false) String search) {
        Long userId = authHelper.getCurrentUserId();
        return ResponseEntity.ok(vehicleService.searchVehicles(userId, search));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Vehicle> getVehicle(@PathVariable Long id) {
        Long userId = authHelper.getCurrentUserId();
        return ResponseEntity.ok(vehicleService.getVehicleForUser(id, userId));
    }

    @PostMapping
    public ResponseEntity<Vehicle> addVehicle(@Valid @RequestBody VehicleRequest request) {
        Long userId = authHelper.getCurrentUserId();
        Vehicle vehicle = vehicleService.addVehicle(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(vehicle);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Vehicle> updateVehicle(@PathVariable Long id,
                                                  @Valid @RequestBody VehicleRequest request) {
        Long userId = authHelper.getCurrentUserId();
        return ResponseEntity.ok(vehicleService.updateVehicle(id, userId, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteVehicle(@PathVariable Long id) {
        Long userId = authHelper.getCurrentUserId();
        vehicleService.deleteVehicle(id, userId);
        return ResponseEntity.ok(Map.of("message", "Vehicle deleted successfully."));
    }
}
