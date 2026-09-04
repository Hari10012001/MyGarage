package com.mygarage.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehiclePredictiveReportDTO {

    // Vehicle Specifications
    private Long vehicleId;
    private String plateNumber;
    private String make;
    private String model;
    private Integer year;
    private String color;
    private String categoryName;
    private String fuelType;
    private Integer currentOdometer;

    // Driving Velocity
    private double dailyDrivingVelocityKm;      // Computed km/day
    private String velocityConfidence;          // "HIGH", "MODERATE", "DEFAULT_ESTIMATE"
    private int odometerSamplePoints;           // Number of distinct chronological odometer readings evaluated

    // Vehicle Health Index
    private VehicleHealthIndexDTO healthIndex;

    // Projected Maintenance Milestones
    private List<PredictiveMaintenanceMilestoneDTO> milestones;

    // Forward Expense Budgets
    private List<ExpenseForecastDTO> expenseForecasts;

    // Financial Reference Averages
    private BigDecimal averageFuelCostPerKm;
    private BigDecimal averageServiceCost;
}
