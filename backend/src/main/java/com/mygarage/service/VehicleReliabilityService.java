package com.mygarage.service;

import com.mygarage.dto.response.GarageReliabilityMatrixDTO;
import com.mygarage.dto.response.VehicleReliabilityReportDTO;

public interface VehicleReliabilityService {
    VehicleReliabilityReportDTO getVehicleReliability(Long vehicleId, Long userId);
    GarageReliabilityMatrixDTO getGarageReliabilityMatrix(Long userId);
}
