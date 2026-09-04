package com.mygarage.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleFuelAnalyticsDTO {
    // Vehicle Specifications
    private Long vehicleId;
    private String plateNumber;
    private String make;
    private String model;
    private Integer year;
    private String fuelType;
    private Integer currentOdometer;

    // Overall lifetime stats
    private int totalFillUps;
    private BigDecimal totalFuelCost;
    private BigDecimal totalLitres;
    private BigDecimal overallAvgMileageKmpl;
    private BigDecimal overallAvgPricePerLitre;

    // Rolling window analytics (30/60/90/180 days)
    private List<FuelWindowMetricsDTO> windowMetrics;

    // Period-over-period delta (current 30d vs prior 30d)
    private Double mileageDeltaPercentage;
    private String mileageTrajectory; // "IMPROVING" / "STABLE" / "DEGRADING" / "NO_DATA"
    private String mileageTrendArrow; // "↑" / "→" / "↓" / ""

    // Best and worst mileage fill-ups
    private LocalDate bestMileageDate;
    private BigDecimal bestMileageKmpl;
    private LocalDate worstMileageDate;
    private BigDecimal worstMileageKmpl;

    // Fill frequency
    private Double avgDaysBetweenFillUps;

    // Price volatility
    private BigDecimal priceVolatilityStdDev;
    private String priceVolatilityLabel; // "LOW" / "MODERATE" / "HIGH"

    // Monthly breakdown (last 12 calendar months)
    private List<FuelMonthlyBreakdownDTO> monthlyBreakdown;

    // Edge case flags
    private boolean insufficientData;
    private boolean partialWindow;
}
