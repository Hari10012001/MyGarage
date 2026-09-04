package com.mygarage.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FleetExpenseBreakdownDTO {

    private long totalVehicles;
    private BigDecimal totalFleetSpend;

    // Overall Category Distribution
    private BigDecimal totalServiceCost;
    private BigDecimal totalFuelCost;
    private BigDecimal totalMaintenanceCost;

    private Double serviceCostPercentage;
    private Double fuelCostPercentage;
    private Double maintenanceCostPercentage;

    // Per-vehicle shares
    private List<VehicleShareItemDTO> vehicleShares;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VehicleShareItemDTO {
        private Long vehicleId;
        private String plateNumber;
        private String make;
        private String model;
        private BigDecimal totalCost;
        private Double percentageOfFleetSpend;
    }
}
