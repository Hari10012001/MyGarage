package com.mygarage.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FleetComparisonReportDTO {

    private int comparedVehicleCount;
    private List<VehicleComparisonDTO> vehicles;

    // Fleet-wide aggregates across compared set
    private BigDecimal fleetTotalSpend;
    private Double fleetAverageMileage;
    private BigDecimal fleetAverageRunningCostPerKm;

    // Highlight summary awards
    private String mostEfficientVehiclePlate;
    private String mostEconomicalVehiclePlate;
    private String lowestMaintenanceVehiclePlate;
    private String fleetWorkhorsePlate;
}
