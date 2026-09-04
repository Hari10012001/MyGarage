package com.mygarage.controller.api;

import com.mygarage.config.AuthHelper;
import com.mygarage.dto.response.GarageTcoSummaryDTO;
import com.mygarage.dto.response.VehicleTcoReportDTO;
import com.mygarage.service.VehicleTcoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class VehicleTcoApiController {

    private final VehicleTcoService vehicleTcoService;
    private final AuthHelper authHelper;

    /**
     * REST endpoint: Get comprehensive TCO, valuation & replacement advisory report for a vehicle.
     */
    @GetMapping("/vehicles/{id}/tco")
    public ResponseEntity<VehicleTcoReportDTO> getVehicleTco(@PathVariable Long id) {
        Long userId = authHelper.getCurrentUserId();
        return ResponseEntity.ok(vehicleTcoService.calculateVehicleTco(id, userId));
    }

    /**
     * REST endpoint: Get garage-wide fleet TCO & asset equity summary across all owned vehicles.
     */
    @GetMapping("/analytics/garage-tco")
    public ResponseEntity<GarageTcoSummaryDTO> getGarageTcoSummary() {
        Long userId = authHelper.getCurrentUserId();
        return ResponseEntity.ok(vehicleTcoService.calculateGarageTcoSummary(userId));
    }
}
