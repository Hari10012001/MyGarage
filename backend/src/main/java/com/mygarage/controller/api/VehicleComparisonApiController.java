package com.mygarage.controller.api;

import com.mygarage.config.AuthHelper;
import com.mygarage.dto.response.FleetComparisonReportDTO;
import com.mygarage.dto.response.FleetExpenseBreakdownDTO;
import com.mygarage.dto.response.VehicleComparisonDTO;
import com.mygarage.service.VehicleComparisonService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class VehicleComparisonApiController {

    private final VehicleComparisonService vehicleComparisonService;
    private final AuthHelper authHelper;

    @GetMapping("/analytics/compare")
    public ResponseEntity<FleetComparisonReportDTO> compareVehicles(@RequestParam("vehicleIds") List<Long> vehicleIds) {
        Long userId = authHelper.getCurrentUserId();
        return ResponseEntity.ok(vehicleComparisonService.compareVehicles(vehicleIds, userId));
    }

    @GetMapping("/analytics/fleet-breakdown")
    public ResponseEntity<FleetExpenseBreakdownDTO> getFleetBreakdown() {
        Long userId = authHelper.getCurrentUserId();
        return ResponseEntity.ok(vehicleComparisonService.getFleetExpenseBreakdown(userId));
    }

    @GetMapping("/vehicles/{id1}/compare/{id2}")
    public ResponseEntity<FleetComparisonReportDTO> compareTwoVehicles(@PathVariable Long id1, @PathVariable Long id2) {
        Long userId = authHelper.getCurrentUserId();
        return ResponseEntity.ok(vehicleComparisonService.compareVehicles(Arrays.asList(id1, id2), userId));
    }

    @GetMapping("/vehicles/{vehicleId}/analytics")
    public ResponseEntity<VehicleComparisonDTO> getVehicleAnalytics(@PathVariable Long vehicleId) {
        Long userId = authHelper.getCurrentUserId();
        return ResponseEntity.ok(vehicleComparisonService.getVehicleComparisonMetrics(vehicleId, userId));
    }
}
