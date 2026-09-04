package com.mygarage.service;

import com.mygarage.dto.request.ScheduleForecastRequestDTO;
import com.mygarage.dto.response.VehiclePredictiveReportDTO;
import com.mygarage.model.MaintenanceRecord;

import java.util.List;

public interface PredictiveMaintenanceService {

    /**
     * Generates a comprehensive predictive maintenance and forward budget report for an owned vehicle.
     * Enforces two-tier ownership isolation.
     */
    VehiclePredictiveReportDTO getVehicleForecast(Long vehicleId, Long userId);

    /**
     * Generates predictive summaries for all vehicles owned by the authenticated user.
     */
    List<VehiclePredictiveReportDTO> getGarageFleetForecast(Long userId);

    /**
     * Converts a predicted periodic maintenance milestone into a scheduled maintenance task.
     * Prevents duplicate scheduling of existing pending tasks.
     */
    MaintenanceRecord scheduleForecastedMilestone(Long vehicleId, ScheduleForecastRequestDTO request, Long userId);
}
