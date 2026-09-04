package com.mygarage.service;

import com.mygarage.dto.response.GarageReportSummaryDTO;
import com.mygarage.dto.response.VehicleDossierDTO;

public interface ReportService {

    VehicleDossierDTO getVehicleDossier(Long vehicleId, Long userId);

    GarageReportSummaryDTO getGarageReportSummary(Long userId);

    String exportVehicleServicesCsv(Long vehicleId, Long userId);

    String exportVehicleFuelCsv(Long vehicleId, Long userId);

    String exportVehicleMaintenanceCsv(Long vehicleId, Long userId);

    String exportVehicleAllCsv(Long vehicleId, Long userId);

    String exportUserGarageCsv(Long userId);
}
