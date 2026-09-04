package com.mygarage.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleComparisonDTO {

    // 1. Vehicle Specifications
    private Long vehicleId;
    private String plateNumber;
    private String make;
    private String model;
    private Integer year;
    private String color;
    private String categoryName;
    private String fuelType;
    private Integer currentOdometer;

    // 2. Financial Metrics
    private BigDecimal serviceCost;
    private BigDecimal fuelCost;
    private BigDecimal maintenanceCost;
    private BigDecimal totalOwnershipCost;

    // 3. Operational Efficiency
    private Double avgMileageKmpl;
    private BigDecimal totalFuelLitres;
    private BigDecimal runningCostPerKm;  // Total Cost / Distance
    private BigDecimal fuelCostPerKm;     // Fuel Cost / Distance

    // 4. Maintenance Health & Reliability
    private long totalServicesCount;
    private long totalFuelLogsCount;
    private long totalMaintenanceTasksCount;
    private long maintenanceCompletedCount;
    private long maintenanceOverdueCount;
    private Double maintenanceCompletionRate; // percentage 0 - 100

    // 5. Automated Performance Badges
    private boolean mostFuelEfficient;
    private boolean lowestCostPerKm;
    private boolean lowestMaintenanceCost;
    private boolean fleetWorkhorse; // Highest odometer / distance tracked
}
