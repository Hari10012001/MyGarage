package com.mygarage.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleTcoReportDTO {

    // Vehicle Specifications
    private Long vehicleId;
    private String plateNumber;
    private String make;
    private String model;
    private Integer year;
    private String categoryName;
    private String fuelType;
    private Integer currentOdometer;
    private int vehicleAgeYears;

    // Operating Financials (Monetary fields strictly BigDecimal)
    private BigDecimal serviceCost;
    private BigDecimal fuelCost;
    private BigDecimal maintenanceCost;
    private BigDecimal totalLifetimeOpex;
    private BigDecimal annualizedOperatingCost;
    private BigDecimal monthlyOperatingCost;
    private BigDecimal operatingCostPerKm;

    // Proportional Shares (%)
    private double serviceSharePercentage;
    private double fuelSharePercentage;
    private double maintenanceSharePercentage;

    // Asset Valuation & Depreciation
    private BigDecimal estimatedInitialBenchmarkValue;
    private double depreciationPercentage;
    private BigDecimal estimatedResidualValue;
    private BigDecimal trailing12MonthsMaintenanceCost;
    private double repairToResidualValueRatio;

    // Replacement & Retention Advisory
    private String advisoryStatus;
    private String advisorySummary;
    private List<String> keyRecommendationPoints;

    // Direct Tailpipe Carbon Emissions (ESG Metrics)
    private BigDecimal totalFuelLitres;
    private BigDecimal totalDirectCarbonEmissionsKg;
    private BigDecimal totalDirectCarbonEmissionsTonnes;
    private BigDecimal carbonIntensityGramsPerKm;
    private String ecoTailpipeRating;

    // Disclaimers
    @Builder.Default
    private String valuationDisclaimer = "Modelled automotive estimate based on category segment benchmarks, age, and mileage. Not a certified appraisal or guaranteed trade-in value.";

    @Builder.Default
    private String carbonDisclaimer = "Calculated direct tailpipe emissions based on recorded fuel combustion; does not represent full upstream lifecycle ESG footprint.";
}
