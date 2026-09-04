package com.mygarage.controller.web;

import com.mygarage.config.AuthHelper;
import com.mygarage.dto.response.GarageReportSummaryDTO;
import com.mygarage.dto.response.VehicleDossierDTO;
import com.mygarage.model.User;
import com.mygarage.service.ReportService;
import com.mygarage.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.nio.charset.StandardCharsets;

@Controller
@RequiredArgsConstructor
public class ReportWebController {

    private final ReportService reportService;
    private final VehicleService vehicleService;
    private final AuthHelper authHelper;

    @GetMapping("/reports")
    public String reportsHub(Model model) {
        User user = authHelper.getCurrentUser();
        GarageReportSummaryDTO summary = reportService.getGarageReportSummary(user.getUserId());
        model.addAttribute("summary", summary);
        model.addAttribute("vehicles", vehicleService.getVehiclesForUser(user.getUserId()));
        model.addAttribute("user", user);
        return "report/index";
    }

    @GetMapping("/vehicles/{vehicleId}/report")
    public String vehicleDossier(@PathVariable Long vehicleId, Model model) {
        User user = authHelper.getCurrentUser();
        VehicleDossierDTO dossier = reportService.getVehicleDossier(vehicleId, user.getUserId());
        model.addAttribute("dossier", dossier);
        model.addAttribute("user", user);
        return "report/dossier";
    }

    @GetMapping({"/vehicles/{vehicleId}/export/services", "/vehicles/{vehicleId}/export/services/csv"})
    public ResponseEntity<byte[]> exportServicesCsv(@PathVariable Long vehicleId) {
        User user = authHelper.getCurrentUser();
        String csv = reportService.exportVehicleServicesCsv(vehicleId, user.getUserId());
        return createCsvResponse(csv, "vehicle-" + vehicleId + "-services.csv");
    }

    @GetMapping({"/vehicles/{vehicleId}/export/fuel", "/vehicles/{vehicleId}/export/fuel/csv"})
    public ResponseEntity<byte[]> exportFuelCsv(@PathVariable Long vehicleId) {
        User user = authHelper.getCurrentUser();
        String csv = reportService.exportVehicleFuelCsv(vehicleId, user.getUserId());
        return createCsvResponse(csv, "vehicle-" + vehicleId + "-fuel.csv");
    }

    @GetMapping({"/vehicles/{vehicleId}/export/maintenance", "/vehicles/{vehicleId}/export/maintenance/csv"})
    public ResponseEntity<byte[]> exportMaintenanceCsv(@PathVariable Long vehicleId) {
        User user = authHelper.getCurrentUser();
        String csv = reportService.exportVehicleMaintenanceCsv(vehicleId, user.getUserId());
        return createCsvResponse(csv, "vehicle-" + vehicleId + "-maintenance.csv");
    }

    @GetMapping({"/vehicles/{vehicleId}/export/all", "/vehicles/{vehicleId}/export/all/csv"})
    public ResponseEntity<byte[]> exportMasterHistoryCsv(@PathVariable Long vehicleId) {
        User user = authHelper.getCurrentUser();
        String csv = reportService.exportVehicleAllCsv(vehicleId, user.getUserId());
        return createCsvResponse(csv, "vehicle-" + vehicleId + "-master-history.csv");
    }

    @GetMapping({"/export/garage/csv", "/export/garage"})
    public ResponseEntity<byte[]> exportGarageCsv() {
        User user = authHelper.getCurrentUser();
        String csv = reportService.exportUserGarageCsv(user.getUserId());
        return createCsvResponse(csv, "garage-portfolio-summary.csv");
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
