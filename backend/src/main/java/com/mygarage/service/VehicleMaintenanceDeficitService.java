package com.mygarage.service;

import com.mygarage.dto.response.GarageMaintenanceDeficitMatrixDTO;
import com.mygarage.dto.response.VehicleMaintenanceDeficitReportDTO;

public interface VehicleMaintenanceDeficitService {

    /**
     * Evaluates the Maintenance Deficit Index (MDI), deferred backlog debt,
     * compound neglect cost exposure, and prioritized recovery roadmap for a single vehicle.
     *
     * @param vehicleId vehicle ID
     * @param userId authenticated user ID
     * @return VehicleMaintenanceDeficitReportDTO
     */
    VehicleMaintenanceDeficitReportDTO evaluateVehicleDeficit(Long vehicleId, Long userId);

    /**
     * Evaluates the garage-wide fleet Maintenance Deficit Index (MDI), aggregate deferred debt,
     * compound neglect liability, vehicle rankings, and consolidated priority triage queue.
     *
     * @param userId authenticated user ID
     * @return GarageMaintenanceDeficitMatrixDTO
     */
    GarageMaintenanceDeficitMatrixDTO evaluateGarageDeficit(Long userId);
}
