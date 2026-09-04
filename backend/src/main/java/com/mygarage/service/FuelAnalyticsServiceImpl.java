package com.mygarage.service;

import com.mygarage.dto.response.*;
import com.mygarage.model.FuelRecord;
import com.mygarage.model.Vehicle;
import com.mygarage.repository.FuelRecordRepository;
import com.mygarage.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FuelAnalyticsServiceImpl implements FuelAnalyticsService {

    private final VehicleRepository vehicleRepository;
    private final FuelRecordRepository fuelRecordRepository;

    @Override
    public VehicleFuelAnalyticsDTO getVehicleFuelAnalytics(Long vehicleId, Long userId) {
        Vehicle vehicle = vehicleRepository.findByVehicleIdAndUserUserId(vehicleId, userId)
                .orElseThrow(() -> new AccessDeniedException("Access denied: You do not own vehicle ID " + vehicleId));

        List<FuelRecord> allRecords = fuelRecordRepository.findByVehicleVehicleIdOrderByFuelDateAsc(vehicleId);

        VehicleFuelAnalyticsDTO.VehicleFuelAnalyticsDTOBuilder builder = VehicleFuelAnalyticsDTO.builder()
                .vehicleId(vehicle.getVehicleId())
                .plateNumber(vehicle.getPlateNumber())
                .make(vehicle.getMake())
                .model(vehicle.getModel())
                .year(vehicle.getYear())
                .fuelType(vehicle.getFuelType())
                .currentOdometer(vehicle.getCurrentOdometer());

        if (allRecords.isEmpty()) {
            return buildEmptyAnalytics(builder);
        }

        if (allRecords.size() == 1) {
            return buildPartialAnalytics(builder, allRecords.get(0));
        }

        // --- Overall Lifetime Stats ---
        int totalFillUps = allRecords.size();
        BigDecimal totalFuelCost = allRecords.stream()
                .map(FuelRecord::getTotalCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalLitres = allRecords.stream()
                .map(FuelRecord::getQuantityLitres)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Filter out null mileage values
        List<FuelRecord> recordsWithMileage = allRecords.stream()
                .filter(r -> r.getEstimatedMileageKmpl() != null)
                .collect(Collectors.toList());

        BigDecimal overallAvgMileageKmpl = null;
        if (!recordsWithMileage.isEmpty()) {
            double avgMileage = recordsWithMileage.stream()
                    .mapToDouble(r -> r.getEstimatedMileageKmpl().doubleValue())
                    .average()
                    .orElse(0.0);
            overallAvgMileageKmpl = BigDecimal.valueOf(avgMileage).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal overallAvgPricePerLitre = allRecords.stream()
                .map(FuelRecord::getCostPerLitre)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(allRecords.size()), 2, RoundingMode.HALF_UP);

        builder.totalFillUps(totalFillUps)
               .totalFuelCost(totalFuelCost)
               .totalLitres(totalLitres)
               .overallAvgMileageKmpl(overallAvgMileageKmpl)
               .overallAvgPricePerLitre(overallAvgPricePerLitre);

        // --- Best and Worst Mileage ---
        if (!recordsWithMileage.isEmpty()) {
            FuelRecord best = Collections.max(recordsWithMileage, Comparator.comparing(FuelRecord::getEstimatedMileageKmpl));
            FuelRecord worst = Collections.min(recordsWithMileage, Comparator.comparing(FuelRecord::getEstimatedMileageKmpl));
            builder.bestMileageDate(best.getFuelDate())
                   .bestMileageKmpl(best.getEstimatedMileageKmpl().setScale(2, RoundingMode.HALF_UP))
                   .worstMileageDate(worst.getFuelDate())
                   .worstMileageKmpl(worst.getEstimatedMileageKmpl().setScale(2, RoundingMode.HALF_UP));
        }

        // --- Fill Frequency ---
        List<Long> gaps = new ArrayList<>();
        for (int i = 1; i < allRecords.size(); i++) {
            long days = ChronoUnit.DAYS.between(allRecords.get(i-1).getFuelDate(), allRecords.get(i).getFuelDate());
            gaps.add(days);
        }
        if (!gaps.isEmpty()) {
            double avgDays = gaps.stream().mapToLong(l -> l).average().orElse(0.0);
            builder.avgDaysBetweenFillUps(Math.round(avgDays * 10.0) / 10.0);
        }

        // --- Price Volatility ---
        builder.priceVolatilityStdDev(computePriceVolatility(allRecords, overallAvgPricePerLitre));
        builder.priceVolatilityLabel(classifyVolatility(builder.build().getPriceVolatilityStdDev()));

        // --- Rolling Windows ---
        LocalDate today = LocalDate.now();
        List<FuelWindowMetricsDTO> windowMetrics = new ArrayList<>();
        windowMetrics.add(computeWindowMetrics(vehicleId, today.minusDays(30), 30));
        windowMetrics.add(computeWindowMetrics(vehicleId, today.minusDays(60), 60));
        windowMetrics.add(computeWindowMetrics(vehicleId, today.minusDays(90), 90));
        windowMetrics.add(computeWindowMetrics(vehicleId, today.minusDays(180), 180));
        builder.windowMetrics(windowMetrics);

        // --- Period Over Period Delta (30d vs prior 30d) ---
        computePeriodDelta(vehicleId, today, builder);

        // --- Monthly Breakdown ---
        builder.monthlyBreakdown(computeMonthlyBreakdown(vehicleId, null, today));

        return builder.insufficientData(false).build();
    }

    @Override
    public GarageFuelIntelligenceDTO getGarageFuelIntelligence(Long userId) {
        List<Vehicle> userVehicles = vehicleRepository.findByUserUserIdOrderByCreatedAtDesc(userId);
        
        if (userVehicles.isEmpty()) {
            return GarageFuelIntelligenceDTO.builder().noFuelData(true).build();
        }

        List<VehicleFuelSummaryItemDTO> vehicleSummaries = new ArrayList<>();
        BigDecimal garageLifetimeFuelCost = BigDecimal.ZERO;
        BigDecimal garageLifetimeLitres = BigDecimal.ZERO;

        for (Vehicle v : userVehicles) {
            List<FuelRecord> records = fuelRecordRepository.findByVehicleVehicleIdOrderByFuelDateAsc(v.getVehicleId());
            if (records.isEmpty()) continue; // Skip vehicles with no fuel data in summary
            
            int totalFillUps = records.size();
            BigDecimal totalFuelCost = records.stream()
                    .map(FuelRecord::getTotalCost)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalLitres = records.stream()
                    .map(FuelRecord::getQuantityLitres)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            garageLifetimeFuelCost = garageLifetimeFuelCost.add(totalFuelCost);
            garageLifetimeLitres = garageLifetimeLitres.add(totalLitres);

            List<FuelRecord> validMileage = records.stream()
                    .filter(r -> r.getEstimatedMileageKmpl() != null)
                    .collect(Collectors.toList());

            BigDecimal avgMileageKmpl = null;
            if (!validMileage.isEmpty()) {
                double avg = validMileage.stream()
                        .mapToDouble(r -> r.getEstimatedMileageKmpl().doubleValue())
                        .average()
                        .orElse(0.0);
                avgMileageKmpl = BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP);
            }

            vehicleSummaries.add(VehicleFuelSummaryItemDTO.builder()
                    .vehicleId(v.getVehicleId())
                    .plateNumber(v.getPlateNumber())
                    .displayName(v.getDisplayName())
                    .fuelType(v.getFuelType())
                    .totalFillUps(totalFillUps)
                    .totalFuelCost(totalFuelCost)
                    .totalLitres(totalLitres)
                    .avgMileageKmpl(avgMileageKmpl)
                    .build());
        }

        if (vehicleSummaries.isEmpty()) {
            return GarageFuelIntelligenceDTO.builder().noFuelData(true).build();
        }

        // Badges
        assignBadges(vehicleSummaries);

        // Weighted Garage Average Mileage
        Double fleetAvgMileage = computeWeightedFleetMileage(vehicleSummaries);

        // Monthly breakdown across garage
        List<FuelMonthlyBreakdownDTO> garageMonthly = computeMonthlyBreakdown(null, userId, LocalDate.now());

        return GarageFuelIntelligenceDTO.builder()
                .vehicleSummaries(vehicleSummaries)
                .garageLifetimeFuelCost(garageLifetimeFuelCost)
                .garageLifetimeLitres(garageLifetimeLitres)
                .garageFleetAvgMileage(fleetAvgMileage)
                .garageMonthlyBreakdown(garageMonthly)
                .noFuelData(false)
                .build();
    }

    // --- Helper Methods ---

    private VehicleFuelAnalyticsDTO buildEmptyAnalytics(VehicleFuelAnalyticsDTO.VehicleFuelAnalyticsDTOBuilder builder) {
        return builder.insufficientData(true)
                .totalFillUps(0)
                .totalFuelCost(BigDecimal.ZERO)
                .totalLitres(BigDecimal.ZERO)
                .mileageTrajectory("NO_DATA")
                .mileageTrendArrow("")
                .priceVolatilityStdDev(BigDecimal.ZERO)
                .priceVolatilityLabel("LOW")
                .windowMetrics(Collections.emptyList())
                .monthlyBreakdown(Collections.emptyList())
                .build();
    }

    private VehicleFuelAnalyticsDTO buildPartialAnalytics(VehicleFuelAnalyticsDTO.VehicleFuelAnalyticsDTOBuilder builder, FuelRecord record) {
        return builder.insufficientData(true)
                .totalFillUps(1)
                .totalFuelCost(record.getTotalCost())
                .totalLitres(record.getQuantityLitres())
                .overallAvgPricePerLitre(record.getCostPerLitre())
                .mileageTrajectory("NO_DATA")
                .mileageTrendArrow("")
                .priceVolatilityStdDev(BigDecimal.ZERO)
                .priceVolatilityLabel("LOW")
                .windowMetrics(Collections.emptyList())
                .monthlyBreakdown(Collections.emptyList())
                .build();
    }

    private FuelWindowMetricsDTO computeWindowMetrics(Long vehicleId, LocalDate fromDate, int windowDays) {
        List<FuelRecord> records = fuelRecordRepository.findByVehicleIdAndFuelDateFromOrderByDateAsc(vehicleId, fromDate);
        
        BigDecimal totalCost = records.stream()
                .map(FuelRecord::getTotalCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalLitres = records.stream()
                .map(FuelRecord::getQuantityLitres)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal avgPrice = null;
        if (!records.isEmpty()) {
            avgPrice = records.stream()
                    .map(FuelRecord::getCostPerLitre)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(records.size()), 2, RoundingMode.HALF_UP);
        }

        List<FuelRecord> recordsWithMileage = records.stream()
                .filter(r -> r.getEstimatedMileageKmpl() != null)
                .collect(Collectors.toList());

        BigDecimal avgMileage = null;
        boolean sufficientData = recordsWithMileage.size() >= 1; // Need at least 1 computed mileage point in window

        if (sufficientData) {
            double avg = recordsWithMileage.stream()
                    .mapToDouble(r -> r.getEstimatedMileageKmpl().doubleValue())
                    .average()
                    .orElse(0.0);
            avgMileage = BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP);
        }

        return FuelWindowMetricsDTO.builder()
                .windowDays(windowDays)
                .recordCount(records.size())
                .avgMileageKmpl(avgMileage)
                .avgPricePerLitre(avgPrice)
                .totalCost(totalCost)
                .totalLitres(totalLitres)
                .sufficientData(sufficientData)
                .build();
    }

    private void computePeriodDelta(Long vehicleId, LocalDate today, VehicleFuelAnalyticsDTO.VehicleFuelAnalyticsDTOBuilder builder) {
        FuelWindowMetricsDTO current30 = computeWindowMetrics(vehicleId, today.minusDays(30), 30);
        
        // Custom query for PRIOR 30 days
        LocalDate prior30Start = today.minusDays(60);
        LocalDate prior30End = today.minusDays(30).minusDays(1);
        List<FuelRecord> prior30Records = fuelRecordRepository.findByVehicleIdAndFuelDateFromOrderByDateAsc(vehicleId, prior30Start)
                .stream().filter(r -> !r.getFuelDate().isAfter(prior30End)).collect(Collectors.toList());
        
        List<FuelRecord> prior30WithMileage = prior30Records.stream()
                .filter(r -> r.getEstimatedMileageKmpl() != null)
                .collect(Collectors.toList());
                
        if (current30.isSufficientData() && !prior30WithMileage.isEmpty()) {
            BigDecimal currentMileage = current30.getAvgMileageKmpl();
            double priorAvg = prior30WithMileage.stream()
                    .mapToDouble(r -> r.getEstimatedMileageKmpl().doubleValue())
                    .average()
                    .orElse(0.0);
            BigDecimal priorMileage = BigDecimal.valueOf(priorAvg).setScale(2, RoundingMode.HALF_UP);
            
            if (priorMileage.compareTo(BigDecimal.ZERO) > 0) {
                double delta = ((currentMileage.doubleValue() - priorMileage.doubleValue()) / priorMileage.doubleValue()) * 100.0;
                builder.mileageDeltaPercentage(Math.round(delta * 10.0) / 10.0);
                
                if (delta > 5.0) {
                    builder.mileageTrajectory("IMPROVING").mileageTrendArrow("↑");
                } else if (delta < -5.0) {
                    builder.mileageTrajectory("DEGRADING").mileageTrendArrow("↓");
                } else {
                    builder.mileageTrajectory("STABLE").mileageTrendArrow("→");
                }
                return;
            }
        }
        
        builder.mileageTrajectory("STABLE").mileageTrendArrow("→").mileageDeltaPercentage(0.0);
    }

    private BigDecimal computePriceVolatility(List<FuelRecord> records, BigDecimal mean) {
        if (records.size() < 2 || mean == null || mean.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        
        double meanDouble = mean.doubleValue();
        double varianceSum = 0;
        
        for (FuelRecord r : records) {
            double price = r.getCostPerLitre().doubleValue();
            varianceSum += Math.pow(price - meanDouble, 2);
        }
        
        double variance = varianceSum / records.size();
        return new BigDecimal(Math.sqrt(variance), MathContext.DECIMAL64).setScale(2, RoundingMode.HALF_UP);
    }

    private String classifyVolatility(BigDecimal stdDev) {
        if (stdDev == null) return "LOW";
        double val = stdDev.doubleValue();
        if (val < 3.0) return "LOW";
        if (val < 10.0) return "MODERATE";
        return "HIGH";
    }

    private List<FuelMonthlyBreakdownDTO> computeMonthlyBreakdown(Long vehicleId, Long userId, LocalDate today) {
        LocalDate fromDate = today.minusMonths(11).withDayOfMonth(1); // last 12 calendar months including current
        
        List<Object[]> results;
        if (vehicleId != null) {
            results = fuelRecordRepository.findMonthlyAggregationByVehicle(vehicleId, fromDate);
        } else {
            results = fuelRecordRepository.findMonthlyAggregationByUser(userId, fromDate);
        }

        Map<String, FuelMonthlyBreakdownDTO> map = new HashMap<>();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MMM yyyy");

        // Init all 12 months with zeros
        for (int i = 0; i < 12; i++) {
            LocalDate monthDate = fromDate.plusMonths(i);
            int year = monthDate.getYear();
            int month = monthDate.getMonthValue();
            String key = year + "-" + month;
            map.put(key, FuelMonthlyBreakdownDTO.builder()
                    .year(year)
                    .month(month)
                    .monthLabel(monthDate.format(dtf))
                    .totalCost(BigDecimal.ZERO)
                    .totalLitres(BigDecimal.ZERO)
                    .avgMileageKmpl(null) // null if no data
                    .fillUps(0)
                    .build());
        }

        // Populate actual data
        for (Object[] row : results) {
            int year = (Integer) row[0];
            int month = (Integer) row[1];
            BigDecimal totalCost = (BigDecimal) row[2];
            BigDecimal totalLitres = (BigDecimal) row[3];
            long fillUps = (Long) row[4];

            String key = year + "-" + month;
            if (map.containsKey(key)) {
                FuelMonthlyBreakdownDTO dto = map.get(key);
                dto.setTotalCost(totalCost != null ? totalCost : BigDecimal.ZERO);
                dto.setTotalLitres(totalLitres != null ? totalLitres : BigDecimal.ZERO);
                dto.setFillUps((int) fillUps);
                // We'll approximate average mileage for the month if we want to display it
                // Since JPQL avg of a calculated column is tricky, we can omit avgMileage from monthly breakdown 
                // or just leave it null (the UI uses cost).
            }
        }

        List<FuelMonthlyBreakdownDTO> sorted = new ArrayList<>(map.values());
        sorted.sort(Comparator.comparing(FuelMonthlyBreakdownDTO::getYear).thenComparing(FuelMonthlyBreakdownDTO::getMonth));
        return sorted;
    }

    private void assignBadges(List<VehicleFuelSummaryItemDTO> summaries) {
        if (summaries.isEmpty()) return;

        VehicleFuelSummaryItemDTO mostEfficient = null;
        VehicleFuelSummaryItemDTO lowestCost = null;
        VehicleFuelSummaryItemDTO highestLitres = null;

        for (VehicleFuelSummaryItemDTO s : summaries) {
            if (s.getAvgMileageKmpl() != null) {
                if (mostEfficient == null || s.getAvgMileageKmpl().compareTo(mostEfficient.getAvgMileageKmpl()) > 0) {
                    mostEfficient = s;
                }
            }
            if (lowestCost == null || s.getTotalFuelCost().compareTo(lowestCost.getTotalFuelCost()) < 0) {
                lowestCost = s;
            }
            if (highestLitres == null || s.getTotalLitres().compareTo(highestLitres.getTotalLitres()) > 0) {
                highestLitres = s;
            }
        }

        if (mostEfficient != null) mostEfficient.setMostFuelEfficient(true);
        if (lowestCost != null) lowestCost.setLowestFuelCost(true);
        if (highestLitres != null) highestLitres.setHighestLitresConsumed(true);
    }

    private Double computeWeightedFleetMileage(List<VehicleFuelSummaryItemDTO> summaries) {
        double totalKmplLitreProduct = 0;
        double totalLitresForAvg = 0;
        
        for (VehicleFuelSummaryItemDTO s : summaries) {
            if (s.getAvgMileageKmpl() != null && s.getTotalLitres() != null && s.getTotalLitres().compareTo(BigDecimal.ZERO) > 0) {
                totalKmplLitreProduct += s.getAvgMileageKmpl().doubleValue() * s.getTotalLitres().doubleValue();
                totalLitresForAvg += s.getTotalLitres().doubleValue();
            }
        }
        
        if (totalLitresForAvg > 0) {
            return Math.round((totalKmplLitreProduct / totalLitresForAvg) * 10.0) / 10.0;
        }
        return null;
    }
}
