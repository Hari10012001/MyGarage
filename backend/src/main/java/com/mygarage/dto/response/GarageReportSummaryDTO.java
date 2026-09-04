package com.mygarage.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GarageReportSummaryDTO {

    private long totalVehicles;
    private long totalServices;
    private long totalFuelLogs;
    private long totalMaintenanceTasks;

    private BigDecimal totalServiceCost;
    private BigDecimal totalFuelCost;
    private BigDecimal totalMaintenanceCost;
    private BigDecimal totalGarageCost;

    private Double averageMileage;

    private List<VehicleReportItemDTO> vehicleBreakdown;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VehicleReportItemDTO {
        private Long vehicleId;
        private String plateNumber;
        private String make;
        private String model;
        private Integer year;
        private String category;
        private Integer currentOdometer;
        private BigDecimal serviceCost;
        private BigDecimal fuelCost;
        private BigDecimal maintenanceCost;
        private BigDecimal totalCost;
        private Double avgMileage;
        private int totalRecords;
    }
}
