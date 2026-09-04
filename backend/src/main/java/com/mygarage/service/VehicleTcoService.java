package com.mygarage.service;

import com.mygarage.dto.response.GarageTcoSummaryDTO;
import com.mygarage.dto.response.VehicleTcoReportDTO;

public interface VehicleTcoService {
    VehicleTcoReportDTO calculateVehicleTco(Long vehicleId, Long userId);
    GarageTcoSummaryDTO calculateGarageTcoSummary(Long userId);
}
