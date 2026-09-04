package com.mygarage.service;

import com.mygarage.dto.response.GarageTcoSummaryDTO;
import com.mygarage.dto.response.VehicleTcoReportDTO;
import com.mygarage.dto.response.VehicleTcoSummaryItemDTO;
import com.mygarage.model.FuelRecord;
import com.mygarage.model.MaintenanceRecord;
import com.mygarage.model.ServiceRecord;
import com.mygarage.model.Vehicle;
import com.mygarage.model.enums.MaintenanceStatus;
import com.mygarage.repository.FuelRecordRepository;
import com.mygarage.repository.MaintenanceRecordRepository;
import com.mygarage.repository.ServiceRecordRepository;
import com.mygarage.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VehicleTcoServiceImpl implements VehicleTcoService {

    private final VehicleRepository vehicleRepository;
    private final ServiceRecordRepository serviceRecordRepository;
    private final FuelRecordRepository fuelRecordRepository;
    private final MaintenanceRecordRepository maintenanceRecordRepository;

    @Override
    public VehicleTcoReportDTO calculateVehicleTco(Long vehicleId, Long userId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found with ID: " + vehicleId));

        // Strict two-tier ownership enforcement
        if (!vehicle.getUser().getUserId().equals(userId)) {
            log.warn("Unauthorized TCO access attempt: User {} attempted to access vehicle {}", userId, vehicleId);
            throw new AccessDeniedException("You do not have permission to view TCO analysis for vehicle ID: " + vehicleId);
        }

        List<ServiceRecord> serviceRecords = serviceRecordRepository.findByVehicleVehicleIdOrderByServiceDateDesc(vehicleId);
        List<FuelRecord> fuelRecords = fuelRecordRepository.findByVehicleVehicleIdOrderByFuelDateDesc(vehicleId);
        List<MaintenanceRecord> maintenanceRecords = maintenanceRecordRepository.findByVehicleVehicleIdOrderByScheduledDateAsc(vehicleId);

        // 1. Operating Financials
        BigDecimal serviceCost = serviceRecords.stream()
                .map(ServiceRecord::getCost)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal fuelCost = fuelRecords.stream()
                .map(FuelRecord::getTotalCost)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal maintenanceCost = maintenanceRecords.stream()
                .filter(m -> m.getStatus() == MaintenanceStatus.COMPLETED)
                .map(MaintenanceRecord::getCost)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalLifetimeOpex = serviceCost.add(fuelCost).add(maintenanceCost).setScale(2, RoundingMode.HALF_UP);

        // 2. Vehicle Operational Age & Run-Rates
        int currentYear = LocalDate.now().getYear();
        int vehicleYear = vehicle.getYear() != null ? vehicle.getYear() : currentYear;
        int vehicleAgeYears = Math.max(1, currentYear - vehicleYear);

        BigDecimal annualizedOperatingCost = totalLifetimeOpex
                .divide(BigDecimal.valueOf(vehicleAgeYears), 2, RoundingMode.HALF_UP);

        BigDecimal monthlyOperatingCost = annualizedOperatingCost
                .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);

        int currentOdometer = vehicle.getCurrentOdometer() != null ? vehicle.getCurrentOdometer() : 0;
        BigDecimal operatingCostPerKm;
        if (currentOdometer > 0) {
            operatingCostPerKm = totalLifetimeOpex
                    .divide(BigDecimal.valueOf(currentOdometer), 2, RoundingMode.HALF_UP);
        } else {
            operatingCostPerKm = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        // 3. Proportional Shares (%)
        double serviceShare = 0.0;
        double fuelShare = 0.0;
        double maintenanceShare = 0.0;
        if (totalLifetimeOpex.compareTo(BigDecimal.ZERO) > 0) {
            serviceShare = serviceCost.divide(totalLifetimeOpex, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).doubleValue();
            fuelShare = fuelCost.divide(totalLifetimeOpex, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).doubleValue();
            maintenanceShare = maintenanceCost.divide(totalLifetimeOpex, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).doubleValue();
        }

        // 4. Asset Valuation & Depreciation Modelling
        BigDecimal initialBenchmarkValue = determineCategoryBenchmarkValue(vehicle);
        double retentionFactor = calculateRetentionFactor(vehicleAgeYears, currentOdometer);
        double depreciationPercentage = Math.round((1.0 - retentionFactor) * 1000.0) / 10.0;

        BigDecimal estimatedResidualValue = initialBenchmarkValue
                .multiply(BigDecimal.valueOf(retentionFactor))
                .setScale(2, RoundingMode.HALF_UP);

        // 5. Trailing 12-Month (T12M) Maintenance Burden & RRVR
        LocalDate cutoffDate = LocalDate.now().minusDays(365);
        BigDecimal trailingServiceCost = serviceRecords.stream()
                .filter(s -> s.getServiceDate() != null && !s.getServiceDate().isBefore(cutoffDate))
                .map(ServiceRecord::getCost)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal trailingMaintCost = maintenanceRecords.stream()
                .filter(m -> m.getStatus() == MaintenanceStatus.COMPLETED
                        && m.getCompletedDate() != null
                        && !m.getCompletedDate().isBefore(cutoffDate))
                .map(MaintenanceRecord::getCost)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal trailing12MonthsMaintenanceCost = trailingServiceCost.add(trailingMaintCost)
                .setScale(2, RoundingMode.HALF_UP);

        double repairToResidualValueRatio = 0.0;
        if (estimatedResidualValue.compareTo(BigDecimal.ZERO) > 0) {
            repairToResidualValueRatio = trailing12MonthsMaintenanceCost
                    .divide(estimatedResidualValue, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).doubleValue();
        }

        // 6. Economic Replacement & Retention Advisory
        AdvisoryResult advisory = evaluateReplacementAdvisory(repairToResidualValueRatio);

        // 7. Direct Tailpipe Carbon Footprint (ESG Intelligence)
        CarbonMetrics carbon = calculateDirectTailpipeCarbon(vehicle, fuelRecords, currentOdometer);

        return VehicleTcoReportDTO.builder()
                .vehicleId(vehicle.getVehicleId())
                .plateNumber(vehicle.getPlateNumber())
                .make(vehicle.getMake())
                .model(vehicle.getModel())
                .year(vehicle.getYear())
                .categoryName(vehicle.getCategory() != null ? vehicle.getCategory().getName() : "Standard")
                .fuelType(vehicle.getFuelType() != null ? vehicle.getFuelType() : "PETROL")
                .currentOdometer(currentOdometer)
                .vehicleAgeYears(vehicleAgeYears)
                .serviceCost(serviceCost)
                .fuelCost(fuelCost)
                .maintenanceCost(maintenanceCost)
                .totalLifetimeOpex(totalLifetimeOpex)
                .annualizedOperatingCost(annualizedOperatingCost)
                .monthlyOperatingCost(monthlyOperatingCost)
                .operatingCostPerKm(operatingCostPerKm)
                .serviceSharePercentage(serviceShare)
                .fuelSharePercentage(fuelShare)
                .maintenanceSharePercentage(maintenanceShare)
                .estimatedInitialBenchmarkValue(initialBenchmarkValue)
                .depreciationPercentage(depreciationPercentage)
                .estimatedResidualValue(estimatedResidualValue)
                .trailing12MonthsMaintenanceCost(trailing12MonthsMaintenanceCost)
                .repairToResidualValueRatio(repairToResidualValueRatio)
                .advisoryStatus(advisory.status)
                .advisorySummary(advisory.summary)
                .keyRecommendationPoints(advisory.recommendations)
                .totalFuelLitres(carbon.totalFuelLitres)
                .totalDirectCarbonEmissionsKg(carbon.totalEmissionsKg)
                .totalDirectCarbonEmissionsTonnes(carbon.totalEmissionsTonnes)
                .carbonIntensityGramsPerKm(carbon.intensityGramsPerKm)
                .ecoTailpipeRating(carbon.ecoRating)
                .build();
    }

    @Override
    public GarageTcoSummaryDTO calculateGarageTcoSummary(Long userId) {
        List<Vehicle> vehicles = vehicleRepository.findByUserUserIdOrderByCreatedAtDesc(userId);

        BigDecimal totalFleetResidual = BigDecimal.ZERO;
        BigDecimal totalFleetAnnualOpex = BigDecimal.ZERO;
        BigDecimal totalFleetCarbonTonnes = BigDecimal.ZERO;

        int healthy = 0;
        int moderate = 0;
        int watchlist = 0;
        int disposal = 0;

        List<VehicleTcoSummaryItemDTO> items = new ArrayList<>();

        for (Vehicle v : vehicles) {
            VehicleTcoReportDTO report = calculateVehicleTco(v.getVehicleId(), userId);

            totalFleetResidual = totalFleetResidual.add(report.getEstimatedResidualValue());
            totalFleetAnnualOpex = totalFleetAnnualOpex.add(report.getAnnualizedOperatingCost());
            totalFleetCarbonTonnes = totalFleetCarbonTonnes.add(report.getTotalDirectCarbonEmissionsTonnes());

            switch (report.getAdvisoryStatus()) {
                case "HEALTHY_RETENTION" -> healthy++;
                case "MODERATE_EXPENSE" -> moderate++;
                case "REPLACEMENT_WATCHLIST" -> watchlist++;
                case "DISPOSAL_RECOMMENDED" -> disposal++;
                default -> healthy++;
            }

            items.add(VehicleTcoSummaryItemDTO.builder()
                    .vehicleId(v.getVehicleId())
                    .plateNumber(v.getPlateNumber())
                    .make(v.getMake())
                    .model(v.getModel())
                    .categoryName(v.getCategory() != null ? v.getCategory().getName() : "Standard")
                    .estimatedResidualValue(report.getEstimatedResidualValue())
                    .annualizedOperatingCost(report.getAnnualizedOperatingCost())
                    .repairToResidualValueRatio(report.getRepairToResidualValueRatio())
                    .advisoryStatus(report.getAdvisoryStatus())
                    .build());
        }

        return GarageTcoSummaryDTO.builder()
                .totalVehicles(vehicles.size())
                .totalFleetResidualValue(totalFleetResidual.setScale(2, RoundingMode.HALF_UP))
                .totalFleetAnnualOperatingCost(totalFleetAnnualOpex.setScale(2, RoundingMode.HALF_UP))
                .totalFleetDirectCarbonTonnes(totalFleetCarbonTonnes.setScale(3, RoundingMode.HALF_UP))
                .healthyRetentionCount(healthy)
                .moderateExpenseCount(moderate)
                .replacementWatchlistCount(watchlist)
                .disposalRecommendedCount(disposal)
                .vehicles(items)
                .build();
    }

    // ==========================================
    // Internal Business Logic & Helper Methods
    // ==========================================

    private BigDecimal determineCategoryBenchmarkValue(Vehicle vehicle) {
        String catName = "";
        if (vehicle.getCategory() != null && vehicle.getCategory().getName() != null) {
            catName = vehicle.getCategory().getName().toLowerCase();
        }

        if (catName.contains("bike") || catName.contains("motorcycle") || catName.contains("scooter") || catName.contains("two-wheeler")) {
            return BigDecimal.valueOf(200000.00).setScale(2, RoundingMode.HALF_UP); // ₹2.00 Lakh
        } else if (catName.contains("hatchback")) {
            return BigDecimal.valueOf(800000.00).setScale(2, RoundingMode.HALF_UP); // ₹8.00 Lakh
        } else if (catName.contains("sedan")) {
            return BigDecimal.valueOf(1500000.00).setScale(2, RoundingMode.HALF_UP); // ₹15.00 Lakh
        } else if (catName.contains("suv") || catName.contains("electric") || catName.contains("luxury") || catName.contains("ev")) {
            return BigDecimal.valueOf(2500000.00).setScale(2, RoundingMode.HALF_UP); // ₹25.00 Lakh
        } else {
            return BigDecimal.valueOf(1200000.00).setScale(2, RoundingMode.HALF_UP); // ₹12.00 Lakh
        }
    }

    private double calculateRetentionFactor(int vehicleAgeYears, int currentOdometer) {
        double retention = 1.0;

        // Declining balance depreciation curve
        for (int t = 1; t <= vehicleAgeYears; t++) {
            if (t == 1) {
                retention *= 0.85; // Year 1: 15% drop
            } else if (t <= 5) {
                retention *= 0.90; // Years 2-5: 10% annual drop
            } else {
                retention *= 0.95; // Years 6+: 5% annual drop
            }
        }

        // Mileage Intensity Adjustment: baseline standard 15,000 km/year
        double annualMileage = (double) currentOdometer / vehicleAgeYears;
        if (annualMileage > 20000.0) {
            double excessBlocks = (annualMileage - 20000.0) / 5000.0;
            double mileagePenaltyFactor = Math.max(0.70, 1.0 - (excessBlocks * 0.015));
            retention *= mileagePenaltyFactor;
        }

        // Hard salvage value floor of 10%
        return Math.max(0.10, retention);
    }

    private AdvisoryResult evaluateReplacementAdvisory(double rrvr) {
        if (rrvr < 15.0) {
            return new AdvisoryResult(
                    "HEALTHY_RETENTION",
                    "Vehicle is in an optimal economic retention window. Trailing 12-month maintenance costs are well within normal operating parameters relative to asset equity.",
                    Arrays.asList(
                            "Continue following scheduled periodic maintenance intervals.",
                            "Vehicle retains strong residual equity; no replacement action required.",
                            "Maintain current usage patterns for maximum cost efficiency."
                    )
            );
        } else if (rrvr < 30.0) {
            return new AdvisoryResult(
                    "MODERATE_EXPENSE",
                    "Maintenance and wear-and-tear expenditures are moderate. Repair costs are justifiable; track upcoming scheduled tasks closely.",
                    Arrays.asList(
                            "Monitor wear-and-tear components such as brakes, suspension, and fluids.",
                            "Review major scheduled service estimates before committing.",
                            "Maintain a 6-month repair reserve budget."
                    )
            );
        } else if (rrvr < 50.0) {
            return new AdvisoryResult(
                    "REPLACEMENT_WATCHLIST",
                    "High maintenance burden relative to vehicle asset equity. Trailing repairs represent a significant fraction of estimated market value.",
                    Arrays.asList(
                            "Place vehicle on the garage replacement watchlist (12–24 month horizon).",
                            "Prioritize safety-critical maintenance while deferring non-essential cosmetic repairs.",
                            "Begin evaluating newer replacement models with higher fuel efficiency and lower warranty repair costs."
                    )
            );
        } else {
            return new AdvisoryResult(
                    "DISPOSAL_RECOMMENDED",
                    "Diminishing financial returns. Cumulative annual maintenance expenditures exceed 50% of the vehicle's estimated residual value.",
                    Arrays.asList(
                            "Economic replacement or disposal is strongly recommended over further major mechanical overhauls.",
                            "Continued capital expenditure yields negative financial return on investment.",
                            "Consider trade-in or liquidation to reallocate capital into a lower-operating-cost asset."
                    )
            );
        }
    }

    private CarbonMetrics calculateDirectTailpipeCarbon(Vehicle vehicle, List<FuelRecord> fuelRecords, int currentOdometer) {
        BigDecimal totalLitres = fuelRecords.stream()
                .map(FuelRecord::getQuantityLitres)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        String fuelType = vehicle.getFuelType() != null ? vehicle.getFuelType().toUpperCase() : "PETROL";
        double factor;
        boolean isElectric = false;

        if (fuelType.contains("DIESEL")) {
            factor = 2.68;
        } else if (fuelType.contains("CNG")) {
            factor = 2.75;
        } else if (fuelType.contains("HYBRID")) {
            factor = 1.60;
        } else if (fuelType.contains("ELECTRIC") || fuelType.contains("EV")) {
            factor = 0.00;
            isElectric = true;
        } else {
            factor = 2.31; // Default PETROL
        }

        BigDecimal totalKg = totalLitres.multiply(BigDecimal.valueOf(factor)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalTonnes = totalKg.divide(BigDecimal.valueOf(1000), 3, RoundingMode.HALF_UP);

        BigDecimal intensityGramsPerKm;
        if (currentOdometer > 0 && !isElectric) {
            double gramsPerKm = (totalKg.doubleValue() * 1000.0) / currentOdometer;
            intensityGramsPerKm = BigDecimal.valueOf(gramsPerKm).setScale(2, RoundingMode.HALF_UP);
        } else {
            intensityGramsPerKm = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        String ecoRating;
        if (isElectric || intensityGramsPerKm.compareTo(BigDecimal.valueOf(100.0)) < 0) {
            ecoRating = "ECO_EXCELLENT";
        } else if (intensityGramsPerKm.compareTo(BigDecimal.valueOf(180.0)) <= 0) {
            ecoRating = "ECO_MODERATE";
        } else {
            ecoRating = "HIGH_EMISSIONS";
        }

        return new CarbonMetrics(totalLitres, totalKg, totalTonnes, intensityGramsPerKm, ecoRating);
    }

    private record AdvisoryResult(String status, String summary, List<String> recommendations) {}
    private record CarbonMetrics(BigDecimal totalFuelLitres, BigDecimal totalEmissionsKg, BigDecimal totalEmissionsTonnes, BigDecimal intensityGramsPerKm, String ecoRating) {}
}
