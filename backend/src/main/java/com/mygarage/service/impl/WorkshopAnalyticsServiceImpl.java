package com.mygarage.service.impl;

import com.mygarage.dto.response.*;
import com.mygarage.exception.ResourceNotFoundException;
import com.mygarage.model.ServiceRecord;
import com.mygarage.model.User;
import com.mygarage.model.Vehicle;
import com.mygarage.model.enums.Role;
import com.mygarage.repository.ServiceRecordRepository;
import com.mygarage.repository.UserRepository;
import com.mygarage.repository.VehicleRepository;
import com.mygarage.service.WorkshopAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * M21: Implementation of the Service Center Ecosystem,
 * Workshop Benchmarking & Vendor Cost Intelligence Engine.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkshopAnalyticsServiceImpl implements WorkshopAnalyticsService {

    private final ServiceRecordRepository serviceRecordRepository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;

    // --- IMMUTABLE APPLICATION-LEVEL MYGARAGE REFERENCE BENCHMARK COSTS ($ / USD) ---
    public static final BigDecimal BENCHMARK_OIL_FILTER = new BigDecimal("95.00");
    public static final BigDecimal BENCHMARK_BRAKES = new BigDecimal("180.00");
    public static final BigDecimal BENCHMARK_COOLANT = new BigDecimal("140.00");
    public static final BigDecimal BENCHMARK_TIRES = new BigDecimal("160.00");
    public static final BigDecimal BENCHMARK_MAJOR_PMS = new BigDecimal("320.00");
    public static final BigDecimal BENCHMARK_GENERAL = new BigDecimal("110.00");

    public static final String LEGAL_DISCLAIMER =
            "All workshop price indices, vendor ratings, rework probabilities, and quality tiers are model-derived " +
            "analytical heuristics based on recorded service log history and MyGarage Reference Benchmark Costs. " +
            "They do not constitute official OEM ratings, certified merchant reviews, or mechanical guarantees.";

    // Corporate entity suffixes to prune only from the very end of workshop names
    private static final List<String> TRAILING_CORPORATE_SUFFIXES = List.of(
            "PRIVATE LIMITED", "PVT LTD", "LIMITED", "LTD", "INCORPORATED", "INC", "LLC", "CORPORATION", "CORP", "CO"
    );

    // Subsystem classification keywords
    private static final Pattern OIL_PATTERN = Pattern.compile("\\b(oil|engine oil|oil filter|lube|synthetic|10w40|5w30|0w20)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern BRAKE_PATTERN = Pattern.compile("\\b(brake|pad|pads|rotor|rotors|caliper|brake fluid|disc|discs|shoe|shoes|lining|abs)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern COOLANT_PATTERN = Pattern.compile("\\b(coolant|radiator|antifreeze|water pump|thermostat|hose|flush)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern TIRE_PATTERN = Pattern.compile("\\b(tire|tires|tyre|tyres|alignment|balancing|rotation|strut|struts|shock|shocks|bushing|wheel|wheels)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern PMS_PATTERN = Pattern.compile("\\b(pms|periodic|timing belt|major service|tune up|spark plug|spark plugs|transmission overhaul)\\b", Pattern.CASE_INSENSITIVE);

    // Rework & defect keywords
    private static final Pattern CORRECTIVE_PATTERN = Pattern.compile("\\b(repair|repairs|leak|leaks|leaking|noise|vibration|broken|failed|check engine|warning|diagnos|diagnosis|diagnostic|damage|damaged|blown|worn out|fault|faulty|fix|fixed|defect|defective)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern ROUTINE_PATTERN = Pattern.compile("\\b(routine|scheduled|periodic|pms|inspection|annual check|rotation|oil change)\\b", Pattern.CASE_INSENSITIVE);

    @Override
    public GarageWorkshopEcosystemMatrixDTO getGarageWorkshopEcosystem(User user) {
        enforceNonAdmin(user);
        return computeEcosystemMatrix(user);
    }

    @Override
    public GarageWorkshopEcosystemMatrixDTO getGarageWorkshopEcosystem(String userEmail) {
        User user = getUserByEmail(userEmail);
        return getGarageWorkshopEcosystem(user);
    }

    @Override
    public WorkshopAnalyticsReportDTO getWorkshopDetail(User user, String workshopKey) {
        enforceNonAdmin(user);

        if (workshopKey == null || workshopKey.trim().isEmpty()) {
            throw new ResourceNotFoundException("Workshop key cannot be empty");
        }

        String normalizedTargetKey = GarageNameNormalizer.normalizeKey(workshopKey);

        // Fetch user's completed service records
        List<ServiceRecord> userCompletedServices = getCompletedServicesForUser(user.getUserId());

        // Check if user has any records matching this workshopKey
        List<ServiceRecord> workshopServices = userCompletedServices.stream()
                .filter(s -> GarageNameNormalizer.normalizeKey(s.getGarageName()).equals(normalizedTargetKey))
                .collect(Collectors.toList());

        if (workshopServices.isEmpty()) {
            // Check if ANY user in the system has records for this workshop
            boolean existsForAnyUser = serviceRecordRepository.findAll().stream()
                    .anyMatch(s -> s.getGarageName() != null &&
                            GarageNameNormalizer.normalizeKey(s.getGarageName()).equals(normalizedTargetKey));

            if (existsForAnyUser) {
                // Cross-user attempt: workshop exists in DB but not for this user
                throw new AccessDeniedException("Access denied: You do not own service history for workshop key " + workshopKey);
            } else {
                // Nonexistent: workshop key does not exist anywhere in the database
                throw new ResourceNotFoundException("Workshop not found with key: " + workshopKey);
            }
        }

        return computeSingleWorkshopReport(normalizedTargetKey, workshopServices, userCompletedServices);
    }

    @Override
    public WorkshopAnalyticsReportDTO getWorkshopDetail(String userEmail, String workshopKey) {
        User user = getUserByEmail(userEmail);
        return getWorkshopDetail(user, workshopKey);
    }

    // =========================================================================
    // CORE ANALYTICAL COMPUTATIONS
    // =========================================================================

    private GarageWorkshopEcosystemMatrixDTO computeEcosystemMatrix(User user) {
        List<ServiceRecord> userCompletedServices = getCompletedServicesForUser(user.getUserId());

        if (userCompletedServices.isEmpty()) {
            return new GarageWorkshopEcosystemMatrixDTO(
                    user.getUserId(),
                    0,
                    0,
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    "None (No Workshop History)",
                    BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP),
                    "HIGHLY_FRAGMENTED",
                    Collections.emptyList(),
                    List.of("No service visits logged across owned vehicles."),
                    LEGAL_DISCLAIMER
            );
        }

        // Group completed services by normalized workshop key
        Map<String, List<ServiceRecord>> recordsByWorkshopKey = new HashMap<>();
        for (ServiceRecord sr : userCompletedServices) {
            String key = GarageNameNormalizer.normalizeKey(sr.getGarageName());
            recordsByWorkshopKey.computeIfAbsent(key, k -> new ArrayList<>()).add(sr);
        }

        List<WorkshopAnalyticsReportDTO> workshopReports = new ArrayList<>();
        for (Map.Entry<String, List<ServiceRecord>> entry : recordsByWorkshopKey.entrySet()) {
            workshopReports.add(computeSingleWorkshopReport(entry.getKey(), entry.getValue(), userCompletedServices));
        }

        // Sort workshops by multi-stage deterministic tie-breaking
        workshopReports.sort(WORKSHOP_COMPARATOR);

        int totalUniqueWorkshops = workshopReports.size();
        int totalFleetServiceVisits = userCompletedServices.size();

        // Calculate total fleet spend using Authoritative Eligible-Cost Policy
        BigDecimal totalFleetSpend = BigDecimal.ZERO;
        for (ServiceRecord sr : userCompletedServices) {
            if (sr.getCost() != null && sr.getCost().compareTo(BigDecimal.ZERO) >= 0) {
                totalFleetSpend = totalFleetSpend.add(sr.getCost());
            }
        }
        totalFleetSpend = totalFleetSpend.setScale(2, RoundingMode.HALF_UP);

        // Calculate HHI Index
        BigDecimal fleetHhi = BigDecimal.ZERO;
        String concentrationTier = "HIGHLY_FRAGMENTED";

        if (totalFleetSpend.compareTo(BigDecimal.ZERO) > 0) {
            double hhiSum = 0.0;
            double fleetSpendDouble = totalFleetSpend.doubleValue();

            for (WorkshopAnalyticsReportDTO wr : workshopReports) {
                double spendShare = (wr.totalSpend().doubleValue() / fleetSpendDouble) * 100.0;
                hhiSum += (spendShare * spendShare);
            }
            fleetHhi = BigDecimal.valueOf(hhiSum).setScale(1, RoundingMode.HALF_UP);

            if (fleetHhi.compareTo(new BigDecimal("5000.0")) >= 0) {
                concentrationTier = "HIGHLY_CONCENTRATED";
            } else if (fleetHhi.compareTo(new BigDecimal("2500.0")) >= 0) {
                concentrationTier = "MODERATELY_CONCENTRATED";
            } else {
                concentrationTier = "HIGHLY_FRAGMENTED";
            }
        }

        // Determine top preferred workshop name
        String topPreferredWorkshopName = "None (No Tier-1 Preferred Workshop Qualified)";
        for (WorkshopAnalyticsReportDTO wr : workshopReports) {
            if ("TIER_1_PREFERRED".equals(wr.valueTier())) {
                topPreferredWorkshopName = wr.workshopName();
                break;
            }
        }

        // Generate ecosystem insights
        List<String> insights = new ArrayList<>();
        insights.add(String.format("Fleet maintenance distributed across %d service centers with %s concentration (HHI: %s).",
                totalUniqueWorkshops, concentrationTier.toLowerCase().replace('_', ' '), fleetHhi));

        if ("None (No Tier-1 Preferred Workshop Qualified)".equals(topPreferredWorkshopName)) {
            insights.add("No workshop currently meets Tier-1 Preferred standards (requires >= 2 visits, VRP <= 10.0%, WPI <= 135.0, WVS >= 80.0).");
        } else {
            insights.add(String.format("Top preferred provider '%s' qualified under Tier-1 benchmark standards.", topPreferredWorkshopName));
        }

        return new GarageWorkshopEcosystemMatrixDTO(
                user.getUserId(),
                totalUniqueWorkshops,
                totalFleetServiceVisits,
                totalFleetSpend,
                topPreferredWorkshopName,
                fleetHhi,
                concentrationTier,
                workshopReports,
                insights,
                LEGAL_DISCLAIMER
        );
    }

    private WorkshopAnalyticsReportDTO computeSingleWorkshopReport(
            String workshopKey,
            List<ServiceRecord> workshopServices,
            List<ServiceRecord> allUserCompletedServices
    ) {
        int totalVisits = workshopServices.size();
        int warrantyVisitCount = 0;
        BigDecimal totalSpend = BigDecimal.ZERO;
        List<String> rawNames = new ArrayList<>();
        List<String> vehiclesServiced = new ArrayList<>();
        LocalDate firstVisitDate = null;
        LocalDate mostRecentVisitDate = null;

        // Collect workshop attributes
        for (ServiceRecord sr : workshopServices) {
            rawNames.add(sr.getGarageName());
            if (sr.getVehicle() != null && sr.getVehicle().getPlateNumber() != null) {
                String plate = sr.getVehicle().getPlateNumber();
                if (!vehiclesServiced.contains(plate)) {
                    vehiclesServiced.add(plate);
                }
            }
            if (sr.getServiceDate() != null) {
                if (firstVisitDate == null || sr.getServiceDate().isBefore(firstVisitDate)) {
                    firstVisitDate = sr.getServiceDate();
                }
                if (mostRecentVisitDate == null || sr.getServiceDate().isAfter(mostRecentVisitDate)) {
                    mostRecentVisitDate = sr.getServiceDate();
                }
            }

            // Eligible cost policy
            if (sr.getCost() != null) {
                if (sr.getCost().compareTo(BigDecimal.ZERO) == 0) {
                    warrantyVisitCount++;
                } else if (sr.getCost().compareTo(BigDecimal.ZERO) > 0) {
                    totalSpend = totalSpend.add(sr.getCost());
                }
            }
        }
        totalSpend = totalSpend.setScale(2, RoundingMode.HALF_UP);

        // Canonical display name
        String workshopDisplayName = GarageNameNormalizer.selectCanonicalDisplayName(rawNames);

        // Average visit cost (total spend divided by total visits)
        BigDecimal averageVisitCost = totalVisits > 0
                ? totalSpend.divide(BigDecimal.valueOf(totalVisits), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        // 1. WPI Calculation
        BigDecimal totalActualPricedSpend = BigDecimal.ZERO;
        BigDecimal totalReferenceBenchmarkSum = BigDecimal.ZERO;
        int pricedVisitsCount = 0;

        for (ServiceRecord sr : workshopServices) {
            if (sr.getCost() != null && sr.getCost().compareTo(BigDecimal.ZERO) > 0) {
                String subsystem = classifySubsystem(sr);
                BigDecimal benchmark = getBenchmarkForSubsystem(subsystem);

                totalActualPricedSpend = totalActualPricedSpend.add(sr.getCost());
                totalReferenceBenchmarkSum = totalReferenceBenchmarkSum.add(benchmark);
                pricedVisitsCount++;
            }
        }

        BigDecimal wpi = new BigDecimal("100.0");
        if (pricedVisitsCount > 0 && totalReferenceBenchmarkSum.compareTo(BigDecimal.ZERO) > 0) {
            wpi = totalActualPricedSpend
                    .multiply(new BigDecimal("100.0"))
                    .divide(totalReferenceBenchmarkSum, 1, RoundingMode.HALF_UP);
        }

        // Pricing Category
        String pricingCategory;
        if (wpi.compareTo(new BigDecimal("85.0")) < 0) {
            pricingCategory = "COMPETITIVE_DISCOUNT";
        } else if (wpi.compareTo(new BigDecimal("115.0")) <= 0) {
            pricingCategory = "MARKET_PARITY";
        } else if (wpi.compareTo(new BigDecimal("140.0")) <= 0) {
            pricingCategory = "PREMIUM_PRICING";
        } else {
            pricingCategory = "HIGH_COST_OUTLIER";
        }

        // 2. VRP % & Rework Events Detection
        List<WorkshopReworkEventDTO> reworkEvents = new ArrayList<>();
        int visitsFollowedByReworkCount = 0;

        for (ServiceRecord visitA : workshopServices) {
            WorkshopReworkEventDTO detectedEvent = findReworkIncidentForVisit(visitA, allUserCompletedServices);
            if (detectedEvent != null) {
                visitsFollowedByReworkCount++;
                reworkEvents.add(detectedEvent);
            }
        }

        BigDecimal vrp = totalVisits > 0
                ? BigDecimal.valueOf(visitsFollowedByReworkCount)
                        .multiply(new BigDecimal("100.0"))
                        .divide(BigDecimal.valueOf(totalVisits), 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);

        boolean isPreliminaryData = totalVisits < 2;

        // 3. Workshop Value Score (WVS)
        // WVS = max(0, min(100, 100 - (WPI - 100) * 0.35 - (VRP * 0.65)))
        double wpiDiff = wpi.doubleValue() - 100.0;
        double vrpVal = vrp.doubleValue();
        double wvsRaw = 100.0 - (wpiDiff * 0.35) - (vrpVal * 0.65);
        double wvsClamped = Math.max(0.0, Math.min(100.0, wvsRaw));
        BigDecimal workshopValueScore = BigDecimal.valueOf(wvsClamped).setScale(1, RoundingMode.HALF_UP);

        // 4. Strict Deterministic 5-Stage Tier Precedence
        String valueTier;
        if (totalVisits < 2) {
            valueTier = "TIER_3_EVALUATING"; // Rule 1: Insufficient Sample
        } else if (vrp.compareTo(new BigDecimal("25.0")) > 0) {
            valueTier = "TIER_5_CAUTION_HIGH_REWORK"; // Rule 2: Defect Risk Trumps Cost
        } else if (wpi.compareTo(new BigDecimal("135.0")) > 0) {
            valueTier = "TIER_4_CAUTION_EXPENSIVE"; // Rule 3: Premium Cost Outlier
        } else if (vrp.compareTo(new BigDecimal("10.0")) <= 0 &&
                   workshopValueScore.compareTo(new BigDecimal("80.0")) >= 0 &&
                   wpi.compareTo(new BigDecimal("135.0")) <= 0) {
            valueTier = "TIER_1_PREFERRED"; // Rule 4: Preferred Value Partner
        } else {
            valueTier = "TIER_2_APPROVED"; // Rule 5: Standard Market Provider
        }

        // Subsystem Metrics
        List<WorkshopSubsystemMetricDTO> subsystemMetrics = computeSubsystemMetrics(workshopServices);

        // Recommendation
        String recommendation;
        switch (valueTier) {
            case "TIER_1_PREFERRED" -> recommendation = "Highly Recommended: Demonstrates superior quality control and competitive market pricing.";
            case "TIER_2_APPROVED" -> recommendation = "Approved Provider: Reliable standard market service center aligned with expected cost benchmarks.";
            case "TIER_3_EVALUATING" -> recommendation = "Preliminary Assessment: Insufficient service visit history to establish a verified quality rating (minimum 2 visits required).";
            case "TIER_4_CAUTION_EXPENSIVE" -> recommendation = "Cost Caution: Premium pricing detected significantly above reference benchmarks (>35% markup). Review itemized labor/part estimates.";
            case "TIER_5_CAUTION_HIGH_REWORK" -> recommendation = "Quality Caution: Elevated rework probability (>25%) detected within 60 days of service. Inspect mechanical workmanship standards.";
            default -> recommendation = "Standard automotive service center.";
        }

        return new WorkshopAnalyticsReportDTO(
                workshopKey,
                workshopDisplayName,
                totalVisits,
                warrantyVisitCount,
                totalSpend,
                averageVisitCost,
                wpi,
                pricingCategory,
                vrp,
                reworkEvents.size(),
                isPreliminaryData,
                workshopValueScore,
                valueTier,
                subsystemMetrics,
                reworkEvents,
                vehiclesServiced,
                firstVisitDate,
                mostRecentVisitDate,
                recommendation,
                LEGAL_DISCLAIMER
        );
    }

    private WorkshopReworkEventDTO findReworkIncidentForVisit(ServiceRecord visitA, List<ServiceRecord> allUserServices) {
        if (visitA.getVehicle() == null || visitA.getServiceDate() == null) {
            return null;
        }

        Long vehicleId = visitA.getVehicle().getVehicleId();
        LocalDate dateA = visitA.getServiceDate();
        Integer odoA = visitA.getOdometerAtService();
        String subsystemA = classifySubsystem(visitA);

        // Find candidate subsequent service visits on the same vehicle
        for (ServiceRecord visitB : allUserServices) {
            if (visitB.getVehicle() == null || !vehicleId.equals(visitB.getVehicle().getVehicleId())) {
                continue;
            }
            if (visitB.getServiceId().equals(visitA.getServiceId())) {
                continue;
            }

            LocalDate dateB = visitB.getServiceDate();
            if (dateB == null) continue;

            // 1. Chronological succession
            boolean isSubsequent = dateB.isAfter(dateA) ||
                    (dateB.isEqual(dateA) && visitB.getServiceId() > visitA.getServiceId());
            if (!isSubsequent) {
                continue;
            }

            // 2. Calendar time boundary (0 <= days <= 60)
            long daysDelta = ChronoUnit.DAYS.between(dateA, dateB);
            if (daysDelta < 0 || daysDelta > 60) {
                continue;
            }

            // 3. Distance traveled boundary (<= 3000 km) & Odometer integrity
            Integer odoB = visitB.getOdometerAtService();
            Integer kmDelta = null;
            if (odoA != null && odoB != null && odoB >= odoA) {
                kmDelta = odoB - odoA;
                if (kmDelta > 3000) {
                    // Not rework: high mileage wear indicates normal operational accumulation
                    continue;
                }
            } // else odometer rollback or missing: distance check bypassed; 60-day window governs

            // 4. False-positive suppression for planned routine recurrence
            if (isPlannedRoutineRecurrence(visitB)) {
                continue; // Pure routine maintenance within 60 days is NOT rework
            }

            // 5. Defect or same-subsystem match
            String subsystemB = classifySubsystem(visitB);
            boolean isCorrective = isCorrectiveRepair(visitB);
            boolean isSameSubsystem = subsystemA.equals(subsystemB);

            if (isCorrective || isSameSubsystem) {
                BigDecimal reworkCost = (visitB.getCost() != null && visitB.getCost().compareTo(BigDecimal.ZERO) >= 0)
                        ? visitB.getCost()
                        : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

                String vehiclePlate = visitB.getVehicle().getPlateNumber() != null
                        ? visitB.getVehicle().getPlateNumber()
                        : "Unknown Plate";

                return new WorkshopReworkEventDTO(
                        visitA.getServiceId(),
                        dateA,
                        visitA.getServiceType(),
                        visitB.getServiceId(),
                        dateB,
                        visitB.getServiceType(),
                        (int) daysDelta,
                        kmDelta,
                        reworkCost,
                        vehiclePlate
                );
            }
        }

        return null;
    }

    private List<WorkshopSubsystemMetricDTO> computeSubsystemMetrics(List<ServiceRecord> workshopServices) {
        Map<String, List<ServiceRecord>> bySubsystem = new HashMap<>();
        for (ServiceRecord sr : workshopServices) {
            String sub = classifySubsystem(sr);
            bySubsystem.computeIfAbsent(sub, k -> new ArrayList<>()).add(sr);
        }

        List<WorkshopSubsystemMetricDTO> metrics = new ArrayList<>();
        for (Map.Entry<String, List<ServiceRecord>> entry : bySubsystem.entrySet()) {
            String subsystem = entry.getKey();
            List<ServiceRecord> records = entry.getValue();
            int count = records.size();

            BigDecimal totalSubsystemSpend = BigDecimal.ZERO;
            for (ServiceRecord r : records) {
                if (r.getCost() != null && r.getCost().compareTo(BigDecimal.ZERO) >= 0) {
                    totalSubsystemSpend = totalSubsystemSpend.add(r.getCost());
                }
            }
            totalSubsystemSpend = totalSubsystemSpend.setScale(2, RoundingMode.HALF_UP);

            BigDecimal avgCost = count > 0
                    ? totalSubsystemSpend.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

            BigDecimal benchmark = getBenchmarkForSubsystem(subsystem);

            BigDecimal variancePct = BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
            if (avgCost.compareTo(BigDecimal.ZERO) > 0 && benchmark.compareTo(BigDecimal.ZERO) > 0) {
                variancePct = avgCost.subtract(benchmark)
                        .multiply(new BigDecimal("100.0"))
                        .divide(benchmark, 1, RoundingMode.HALF_UP);
            }

            metrics.add(new WorkshopSubsystemMetricDTO(
                    subsystem,
                    getSubsystemDisplayName(subsystem),
                    count,
                    totalSubsystemSpend,
                    avgCost,
                    benchmark,
                    variancePct
            ));
        }

        // Sort metrics by service count descending
        metrics.sort((m1, m2) -> Integer.compare(m2.serviceCount(), m1.serviceCount()));
        return metrics;
    }

    // =========================================================================
    // DOMAIN & KEYWORD CLASSIFICATION
    // =========================================================================

    public static String classifySubsystem(ServiceRecord sr) {
        String text = ((sr.getServiceType() != null ? sr.getServiceType() : "") + " " +
                (sr.getDescription() != null ? sr.getDescription() : "")).toLowerCase();

        if (OIL_PATTERN.matcher(text).find()) {
            return "ENGINE_OIL_AND_FILTER";
        } else if (BRAKE_PATTERN.matcher(text).find()) {
            return "BRAKING_SYSTEM";
        } else if (COOLANT_PATTERN.matcher(text).find()) {
            return "COOLING_SYSTEM_FLUIDS";
        } else if (TIRE_PATTERN.matcher(text).find()) {
            return "TIRES_AND_SUSPENSION";
        } else if (PMS_PATTERN.matcher(text).find()) {
            return "MAJOR_PMS";
        } else {
            return "GENERAL_MAINTENANCE";
        }
    }

    public static BigDecimal getBenchmarkForSubsystem(String subsystem) {
        return switch (subsystem) {
            case "ENGINE_OIL_AND_FILTER" -> BENCHMARK_OIL_FILTER;
            case "BRAKING_SYSTEM" -> BENCHMARK_BRAKES;
            case "COOLING_SYSTEM_FLUIDS" -> BENCHMARK_COOLANT;
            case "TIRES_AND_SUSPENSION" -> BENCHMARK_TIRES;
            case "MAJOR_PMS" -> BENCHMARK_MAJOR_PMS;
            default -> BENCHMARK_GENERAL;
        };
    }

    public static String getSubsystemDisplayName(String subsystem) {
        return switch (subsystem) {
            case "ENGINE_OIL_AND_FILTER" -> "Engine Oil & Filter";
            case "BRAKING_SYSTEM" -> "Braking System";
            case "COOLING_SYSTEM_FLUIDS" -> "Cooling System & Fluids";
            case "TIRES_AND_SUSPENSION" -> "Tires & Suspension";
            case "MAJOR_PMS" -> "Major PMS & Engine Service";
            default -> "General Maintenance & Inspection";
        };
    }

    private boolean isPlannedRoutineRecurrence(ServiceRecord r) {
        String text = ((r.getServiceType() != null ? r.getServiceType() : "") + " " +
                (r.getDescription() != null ? r.getDescription() : "")).toLowerCase();
        boolean hasRoutine = ROUTINE_PATTERN.matcher(text).find();
        boolean hasCorrective = CORRECTIVE_PATTERN.matcher(text).find();
        return hasRoutine && !hasCorrective;
    }

    private boolean isCorrectiveRepair(ServiceRecord r) {
        String text = ((r.getServiceType() != null ? r.getServiceType() : "") + " " +
                (r.getDescription() != null ? r.getDescription() : "")).toLowerCase();
        return CORRECTIVE_PATTERN.matcher(text).find();
    }

    // =========================================================================
    // GARAGE NAME NORMALIZATION & CANONICAL DISPLAY NAME
    // =========================================================================

    public static class GarageNameNormalizer {

        public static String normalizeKey(String rawName) {
            if (rawName == null || rawName.trim().isEmpty() ||
                    "N/A".equalsIgnoreCase(rawName.trim()) ||
                    "null".equalsIgnoreCase(rawName.trim())) {
                return "INDEPENDENT_UNSPECIFIED";
            }

            // 1. Replace punctuation with space
            String cleaned = rawName.replaceAll("[,.\\-/\\\\\'\"()#&@_]", " ");

            // 2. Uppercase
            cleaned = cleaned.toUpperCase();

            // 3. Compress whitespace and trim
            cleaned = cleaned.replaceAll("\\s+", " ").trim();

            // 4. Prune trailing legal entity corporate suffixes only
            boolean changed = true;
            while (changed) {
                changed = false;
                for (String suffix : TRAILING_CORPORATE_SUFFIXES) {
                    if (cleaned.endsWith(" " + suffix)) {
                        cleaned = cleaned.substring(0, cleaned.length() - suffix.length() - 1).trim();
                        changed = true;
                        break;
                    }
                }
            }

            if (cleaned.isEmpty()) {
                return "INDEPENDENT_UNSPECIFIED";
            }

            // 5. Form normalized key
            return cleaned.replace(' ', '_');
        }

        public static String selectCanonicalDisplayName(List<String> rawNames) {
            if (rawNames == null || rawNames.isEmpty()) {
                return "Independent / Unspecified Workshop";
            }

            // Group by raw name to find occurrence frequencies
            Map<String, Integer> freqMap = new HashMap<>();
            for (String raw : rawNames) {
                if (raw != null && !raw.trim().isEmpty()) {
                    freqMap.put(raw, freqMap.getOrDefault(raw, 0) + 1);
                }
            }

            if (freqMap.isEmpty()) {
                return "Independent / Unspecified Workshop";
            }

            // Sort according to deterministic rules:
            // 1. Frequency descending
            // 2. Length descending
            // 3. Lexicographical ascending
            List<String> sortedRaw = new ArrayList<>(freqMap.keySet());
            sortedRaw.sort((s1, s2) -> {
                int freqCompare = Integer.compare(freqMap.get(s2), freqMap.get(s1));
                if (freqCompare != 0) return freqCompare;
                int lenCompare = Integer.compare(s2.length(), s1.length());
                if (lenCompare != 0) return lenCompare;
                return s1.compareTo(s2);
            });

            String winningRaw = sortedRaw.get(0);
            return formatCanonicalTitle(winningRaw);
        }

        public static String formatCanonicalTitle(String raw) {
            if (raw == null || raw.trim().isEmpty() ||
                    "N/A".equalsIgnoreCase(raw.trim()) ||
                    "null".equalsIgnoreCase(raw.trim())) {
                return "Independent / Unspecified Workshop";
            }

            // Clean punctuation
            String cleaned = raw.replaceAll("[,.\\-/\\\\\'\"()#&@_]", " ");
            cleaned = cleaned.replaceAll("\\s+", " ").trim();

            // Prune trailing corporate suffixes
            boolean changed = true;
            while (changed) {
                changed = false;
                for (String suffix : TRAILING_CORPORATE_SUFFIXES) {
                    String upper = cleaned.toUpperCase();
                    if (upper.endsWith(" " + suffix)) {
                        cleaned = cleaned.substring(0, cleaned.length() - suffix.length() - 1).trim();
                        changed = true;
                        break;
                    }
                }
            }

            if (cleaned.isEmpty()) {
                return "Independent / Unspecified Workshop";
            }

            // Title-case
            String[] words = cleaned.split(" ");
            StringBuilder sb = new StringBuilder();
            for (String w : words) {
                if (w.isEmpty()) continue;
                if (sb.length() > 0) sb.append(" ");
                sb.append(Character.toUpperCase(w.charAt(0)));
                if (w.length() > 1) {
                    sb.append(w.substring(1).toLowerCase());
                }
            }
            return sb.toString();
        }
    }

    // =========================================================================
    // SORTING & TIE-BREAKING
    // =========================================================================

    private static final Comparator<WorkshopAnalyticsReportDTO> WORKSHOP_COMPARATOR = (w1, w2) -> {
        // 1. Tier Rank
        int tierRank1 = getTierRank(w1.valueTier());
        int tierRank2 = getTierRank(w2.valueTier());
        if (tierRank1 != tierRank2) {
            return Integer.compare(tierRank1, tierRank2);
        }

        // 2. Workshop Value Score descending
        int wvsCompare = w2.workshopValueScore().compareTo(w1.workshopValueScore());
        if (wvsCompare != 0) return wvsCompare;

        // 3. Total Visits descending
        int visitCompare = Integer.compare(w2.totalVisits(), w1.totalVisits());
        if (visitCompare != 0) return visitCompare;

        // 4. Total Spend descending
        int spendCompare = w2.totalSpend().compareTo(w1.totalSpend());
        if (spendCompare != 0) return spendCompare;

        // 5. Normalized Key ascending
        return w1.workshopKey().compareTo(w2.workshopKey());
    };

    private static int getTierRank(String tier) {
        return switch (tier) {
            case "TIER_1_PREFERRED" -> 1;
            case "TIER_2_APPROVED" -> 2;
            case "TIER_3_EVALUATING" -> 3;
            case "TIER_4_CAUTION_EXPENSIVE" -> 4;
            case "TIER_5_CAUTION_HIGH_REWORK" -> 5;
            default -> 6;
        };
    }

    // =========================================================================
    // SECURITY & HELPERS
    // =========================================================================

    private List<ServiceRecord> getCompletedServicesForUser(Long userId) {
        LocalDate today = LocalDate.now();
        return serviceRecordRepository.findAllByUserId(userId).stream()
                .filter(sr -> sr.getServiceId() != null &&
                        sr.getVehicle() != null &&
                        sr.getServiceDate() != null &&
                        !sr.getServiceDate().isAfter(today))
                .collect(Collectors.toList());
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AccessDeniedException("User not found: " + email));
    }

    private void enforceNonAdmin(User user) {
        if (user.getRole() == Role.ADMIN) {
            throw new AccessDeniedException("Admin users are isolated from private user workshop analytics");
        }
    }
}
