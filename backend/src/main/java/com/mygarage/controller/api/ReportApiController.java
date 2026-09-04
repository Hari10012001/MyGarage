package com.mygarage.controller.api;

import com.mygarage.config.AuthHelper;
import com.mygarage.dto.response.GarageReportSummaryDTO;
import com.mygarage.dto.response.VehicleDossierDTO;
import com.mygarage.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReportApiController {

    private final ReportService reportService;
    private final AuthHelper authHelper;

    @GetMapping("/reports/garage-summary")
    public ResponseEntity<GarageReportSummaryDTO> getGarageSummary() {
        Long userId = authHelper.getCurrentUserId();
        return ResponseEntity.ok(reportService.getGarageReportSummary(userId));
    }

    @GetMapping("/vehicles/{vehicleId}/export/summary")
    public ResponseEntity<VehicleDossierDTO> getVehicleDossierSummary(@PathVariable Long vehicleId) {
        Long userId = authHelper.getCurrentUserId();
        return ResponseEntity.ok(reportService.getVehicleDossier(vehicleId, userId));
    }

    @GetMapping("/vehicles/{vehicleId}/export/services")
    public ResponseEntity<byte[]> exportServices(@PathVariable Long vehicleId) {
        Long userId = authHelper.getCurrentUserId();
        String csv = reportService.exportVehicleServicesCsv(vehicleId, userId);
        return createCsvResponse(csv, "vehicle-" + vehicleId + "-services.csv");
    }

    @GetMapping("/vehicles/{vehicleId}/export/fuel")
    public ResponseEntity<byte[]> exportFuel(@PathVariable Long vehicleId) {
        Long userId = authHelper.getCurrentUserId();
        String csv = reportService.exportVehicleFuelCsv(vehicleId, userId);
        return createCsvResponse(csv, "vehicle-" + vehicleId + "-fuel.csv");
    }

    @GetMapping("/vehicles/{vehicleId}/export/maintenance")
    public ResponseEntity<byte[]> exportMaintenance(@PathVariable Long vehicleId) {
        Long userId = authHelper.getCurrentUserId();
        String csv = reportService.exportVehicleMaintenanceCsv(vehicleId, userId);
        return createCsvResponse(csv, "vehicle-" + vehicleId + "-maintenance.csv");
    }

    @GetMapping("/vehicles/{vehicleId}/export/all")
    public ResponseEntity<byte[]> exportAll(@PathVariable Long vehicleId) {
        Long userId = authHelper.getCurrentUserId();
        String csv = reportService.exportVehicleAllCsv(vehicleId, userId);
        return createCsvResponse(csv, "vehicle-" + vehicleId + "-all.csv");
    }

    private ResponseEntity<byte[]> createCsvResponse(String csvData, String filename) {
        byte[] bytes = csvData.getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .contentLength(bytes.length)
                .body(bytes);
    }
}
