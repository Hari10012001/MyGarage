package com.mygarage.service;

import com.mygarage.dto.response.FleetComparisonReportDTO;
import com.mygarage.dto.response.FleetExpenseBreakdownDTO;
import com.mygarage.dto.response.VehicleComparisonDTO;

import java.util.List;

public interface VehicleComparisonService {

    FleetComparisonReportDTO compareVehicles(List<Long> vehicleIds, Long userId);

    VehicleComparisonDTO getVehicleComparisonMetrics(Long vehicleId, Long userId);

    FleetExpenseBreakdownDTO getFleetExpenseBreakdown(Long userId);
}
