package com.mygarage.service;

import com.mygarage.dto.response.*;
import com.mygarage.model.MaintenanceRecord;
import com.mygarage.model.ServiceRecord;
import com.mygarage.model.Vehicle;
import com.mygarage.model.enums.MaintenanceStatus;
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
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VehicleMaintenanceDeficitServiceImpl implements VehicleMaintenanceDeficitService {

    public static final String ANALYTICAL_DISCLAIMER =
            "The Maintenance Deficit Index (MDI) and Compound Cascade Multipliers are MyGarage analytical modeling " +
            "heuristics derived from historical vehicle logs and configurable benchmark cost assumptions. " +
            "They do not represent official ISO 55000 / NASA metrics or manufacturer-certified engineering warranties, " +
            "nor do they guarantee that secondary mechanical damage will occur within a specific timeline. Always consult a certified mechanic.";

    public static final String BENCHMARK_POLICY_NOTE =
            "Benchmark maintenance costs and intervals are authoritative configurable model assumptions (" +
            "Engine Oil: $95.00, 10,000 km / 180 days; Brakes: $180.00, 20,000 km / 365 days; " +
            "Coolant: $140.00, 40,000 km / 730 days; Tires/Suspension: $160.00, 30,000 km / 540 days; " +
            "Major PMS: $320.00, 40,000 km / 730 days; General Maintenance fallback: $110.00, 15,000 km / 365 days). " +
            "Recorded costs take precedence when present and positive; unpriced or negative costs strictly fallback to these domain benchmarks.";

    private final VehicleRepository vehicleRepository;
    private final ServiceRecordRepository serviceRecordRepository;
    private final MaintenanceRecordRepository maintenanceRecordRepository;

    public enum SubsystemDomain {
        ENGINE_OIL_AND_FILTER(
                "Engine Oil & Filter",
                BigDecimal.valueOf(95.00),
                10_000,
                180,
                5.5,
                1.30,
                "Severe oil sludge, camshaft scoring, turbocharger bearing failure, or catastrophic engine seizure",
                List.of("Turbocharger bearing oil starvation", "Timing chain tensioner failure", "Piston ring carbon packing and blow-by"),
                List.of("oil", "engine oil", "oil filter", "lube", "lubricant", "synthetic oil")
        ),
        BRAKING_SYSTEM(
                "Braking System",
                BigDecimal.valueOf(180.00),
                20_000,
                365,
                3.2,
                1.30,
                "Metal-on-metal rotor gouging, caliper piston seizure, and hydraulic brake pressure failure",
                List.of("Hydraulic ABS actuator pressure loss", "Brake fluid boiling from thermal saturation", "Uneven pad tapering and rotor warping"),
                List.of("brake", "brakes", "brake pad", "brake pads", "rotor", "rotors", "caliper", "brake fluid")
        ),
        COOLING_SYSTEM_FLUIDS(
                "Cooling System & Fluids",
                BigDecimal.valueOf(140.00),
                40_000,
                730,
                4.8,
                1.15,
                "Radiator boiling, blown head gasket, warped cylinder head, and engine overheating",
                List.of("Water pump impeller cavitation erosion", "Heater core internal blockage", "Thermostat valve thermal lockup"),
                List.of("coolant", "radiator", "antifreeze", "water pump", "thermostat", "cooling")
        ),
        TIRES_AND_SUSPENSION(
                "Tires & Suspension",
                BigDecimal.valueOf(160.00),
                30_000,
                540,
                2.5,
                1.00,
                "High-speed tire tread separation, blowout risk, and strut mount / tie rod destruction",
                List.of("Premature wheel bearing burnout", "Tie-rod and steering rack play", "Emergency stopping distance degradation"),
                List.of("tire", "tires", "tyre", "tyres", "alignment", "wheel balancing", "suspension", "shock", "strut", "absorber")
        ),
        MAJOR_PMS(
                "Major PMS & Powertrain Overhaul",
                BigDecimal.valueOf(320.00),
                40_000,
                730,
                3.0,
                1.15,
                "Broad multi-system degradation, timing belt snap, and loss of powertrain efficiency",
                List.of("Catalytic converter clogging from unburnt fuel", "Fuel pump strain from filter restriction", "Valvetrain misfire"),
                List.of("pms", "major service", "scheduled maintenance", "timing belt", "spark plug", "transmission fluid")
        ),
        GENERAL_MAINTENANCE(
                "General Maintenance & Inspection",
                BigDecimal.valueOf(110.00),
                15_000,
                365,
                2.0,
                1.00,
                "Premature secondary component wear and operational reliability degradation",
                List.of("Cabin air contamination", "Auxiliary drive belt squeal/snap", "Battery terminal corrosion"),
                List.of()
        );

        public final String displayName;
        public final BigDecimal benchmarkCost;
        public final int intervalKm;
        public final int intervalDays;
        public final double cascadeMultiplier;
        public final double priorityWeight;
        public final String primaryConsequence;
        public final List<String> secondaryConsequences;
        public final List<String> keywords;

        SubsystemDomain(String displayName, BigDecimal benchmarkCost, int intervalKm, int intervalDays,
                        double cascadeMultiplier, double priorityWeight, String primaryConsequence,
                        List<String> secondaryConsequences, List<String> keywords) {
            this.displayName = displayName;
            this.benchmarkCost = benchmarkCost.setScale(2, RoundingMode.HALF_UP);
            this.intervalKm = intervalKm;
            this.intervalDays = intervalDays;
            this.cascadeMultiplier = cascadeMultiplier;
            this.priorityWeight = priorityWeight;
            this.primaryConsequence = primaryConsequence;
            this.secondaryConsequences = secondaryConsequences;
            this.keywords = keywords;
        }

        public static SubsystemDomain classify(String text) {
            if (text == null || text.isBlank()) {
                return GENERAL_MAINTENANCE;
            }
            String lower = text.toLowerCase();
            for (SubsystemDomain domain : values()) {
                if (domain == GENERAL_MAINTENANCE) continue;
                for (String kw : domain.keywords) {
                    if (lower.contains(kw)) {
                        return domain;
                    }
                }
            }
            return GENERAL_MAINTENANCE;
        }
    }

    @Override
    public VehicleMaintenanceDeficitReportDTO evaluateVehicleDeficit(Long vehicleId, Long userId) {
        Vehicle vehicle = vehicleRepository.findByVehicleIdAndUserUserId(vehicleId, userId)
                .orElseThrow(() -> new AccessDeniedException("Access denied: You do not own vehicle ID " + vehicleId));

        List<ServiceRecord> serviceRecords = serviceRecordRepository.findByVehicleVehicleIdOrderByServiceDateDesc(vehicleId);
        List<MaintenanceRecord> maintenanceRecords = maintenanceRecordRepository.findByVehicleVehicleIdOrderByScheduledDateAsc(vehicleId);

        return buildVehicleDeficitReport(vehicle, serviceRecords, maintenanceRecords);
    }

    @Override
    public GarageMaintenanceDeficitMatrixDTO evaluateGarageDeficit(Long userId) {
        List<Vehicle> userVehicles = vehicleRepository.findByUserUserIdOrderByCreatedAtDesc(userId);

        if (userVehicles.isEmpty()) {
            return new GarageMaintenanceDeficitMatrixDTO(
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    0.0,
                    "PRISTINE",
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    0, 0, 0, 0, 0,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    "No active vehicles found in garage. Fleet maintenance posture is optimal.",
                    ANALYTICAL_DISCLAIMER,
                    BENCHMARK_POLICY_NOTE
            );
        }

        BigDecimal totalFleetRAV = BigDecimal.ZERO;
        BigDecimal totalFleetDeferredDebt = BigDecimal.ZERO;
        BigDecimal totalCompoundExposure = BigDecimal.ZERO;
        BigDecimal totalNearTermExposure = BigDecimal.ZERO;
        BigDecimal totalFleet30DayLiability = BigDecimal.ZERO;

        int criticalCount = 0;
        int deficientCount = 0;
        int fairCount = 0;
        int optimalCount = 0;

        List<FleetMaintenanceDebtSummaryDTO> summaries = new ArrayList<>();
        List<BacklogTriageRoadmapItemDTO> allTriageItems = new ArrayList<>();

        for (Vehicle vehicle : userVehicles) {
            VehicleMaintenanceDeficitReportDTO report = evaluateVehicleDeficit(vehicle.getVehicleId(), userId);

            totalFleetRAV = totalFleetRAV.add(report.replacementAssetValue());
            totalFleetDeferredDebt = totalFleetDeferredDebt.add(report.deferredMaintenanceDebt());
            totalCompoundExposure = totalCompoundExposure.add(report.compoundNeglectCostExposure());
            totalNearTermExposure = totalNearTermExposure.add(report.totalNearTermExposure());
            totalFleet30DayLiability = totalFleet30DayLiability.add(report.total30DayMaintenanceLiability());

            switch (report.deficitStatus()) {
                case "CRITICAL" -> criticalCount++;
                case "DEFICIENT" -> deficientCount++;
                case "FAIR" -> fairCount++;
                default -> optimalCount++;
            }

            String topAction = report.triageRoadmap().isEmpty()
                    ? "No immediate triage required"
                    : report.triageRoadmap().get(0).triageAction();

            summaries.add(new FleetMaintenanceDebtSummaryDTO(
                    vehicle.getVehicleId(),
                    vehicle.getPlateNumber(),
                    vehicle.getMake(),
                    vehicle.getModel(),
                    vehicle.getYear(),
                    vehicle.getCurrentOdometer(),
                    report.replacementAssetValue(),
                    report.deferredMaintenanceDebt(),
                    report.maintenanceDeficitIndex(),
                    report.deficitStatus(),
                    report.compoundNeglectCostExposure(),
                    report.totalNearTermExposure(),
                    report.total30DayMaintenanceLiability(),
                    report.backlogItemCount(),
                    topAction
            ));

            allTriageItems.addAll(report.triageRoadmap());
        }

        // Fleet MDI calculation
        double rawFleetMdi = totalFleetRAV.compareTo(BigDecimal.ZERO) > 0
                ? (totalFleetDeferredDebt.doubleValue() / Math.max(1000.0, totalFleetRAV.doubleValue())) * 100.0
                : 0.0;
        double garageFleetMDI = Math.min(100.0, Math.round(rawFleetMdi * 100.0) / 100.0);
        String fleetDeficitStatus = resolveDeficitStatus(garageFleetMDI);

        // Sort all triage items with deterministic 4-stage tie-breaking
        List<BacklogTriageRoadmapItemDTO> sortedFleetTriage = sortTriageItems(allTriageItems);

        String advisory;
        if (criticalCount > 0) {
            advisory = String.format("URGENT: %d vehicle(s) are in CRITICAL maintenance deficit. Prioritize top triage actions immediately to prevent compound mechanical cascade.", criticalCount);
        } else if (deficientCount > 0) {
            advisory = String.format("ATTENTION: %d vehicle(s) show DEFICIENT backlog postures. Clearing overdue obligations will yield high risk mitigation dividends.", deficientCount);
        } else if (fairCount > 0) {
            advisory = "Fleet posture is FAIR. Maintenance liabilities are elevated but manageable with planned servicing.";
        } else {
            advisory = "Fleet maintenance posture is OPTIMAL. Backlog liabilities and compound risks are under active control.";
        }

        return new GarageMaintenanceDeficitMatrixDTO(
                totalFleetRAV.setScale(2, RoundingMode.HALF_UP),
                totalFleetDeferredDebt.setScale(2, RoundingMode.HALF_UP),
                garageFleetMDI,
                fleetDeficitStatus,
                totalCompoundExposure.setScale(2, RoundingMode.HALF_UP),
                totalNearTermExposure.setScale(2, RoundingMode.HALF_UP),
                totalFleet30DayLiability.setScale(2, RoundingMode.HALF_UP),
                userVehicles.size(),
                criticalCount,
                deficientCount,
                fairCount,
                optimalCount,
                summaries,
                sortedFleetTriage,
                advisory,
                ANALYTICAL_DISCLAIMER,
                BENCHMARK_POLICY_NOTE
        );
    }

    private VehicleMaintenanceDeficitReportDTO buildVehicleDeficitReport(
            Vehicle vehicle,
            List<ServiceRecord> serviceRecords,
            List<MaintenanceRecord> maintenanceRecords
    ) {
        LocalDate today = LocalDate.now();
        int currentOdo = vehicle.getCurrentOdometer() != null ? Math.max(0, vehicle.getCurrentOdometer()) : 0;

        List<DeferredBacklogItemDTO> backlogItems = new ArrayList<>();
        List<NearTermExposureItemDTO> nearTermItems = new ArrayList<>();
        Set<String> coveredSubsystems = new HashSet<>();

        // Step 1: Evaluate Active MaintenanceRecords
        for (MaintenanceRecord mr : maintenanceRecords) {
            // Strictly exclude COMPLETED records
            if (mr.getCompletedDate() != null || mr.getStatus() == MaintenanceStatus.COMPLETED) {
                continue;
            }

            LocalDate scheduled = mr.getScheduledDate();
            if (scheduled == null) continue;

            String combinedText = (mr.getTitle() != null ? mr.getTitle() : "") + " " +
                                  (mr.getDescription() != null ? mr.getDescription() : "");
            SubsystemDomain domain = SubsystemDomain.classify(combinedText);

            // Determine authoritative immediate cost
            BigDecimal immediateCost;
            if (mr.getCost() != null && mr.getCost().compareTo(BigDecimal.ZERO) > 0) {
                immediateCost = mr.getCost().setScale(2, RoundingMode.HALF_UP);
            } else {
                immediateCost = domain.benchmarkCost;
            }

            boolean isOverdueOrDueToday = scheduled.isBefore(today)
                    || scheduled.isEqual(today)
                    || mr.getStatus() == MaintenanceStatus.OVERDUE
                    || mr.getStatus() == MaintenanceStatus.DUE_TODAY;

            if (isOverdueOrDueToday) {
                int daysOverdue = (int) Math.max(0, ChronoUnit.DAYS.between(scheduled, today));
                Double mileageOverdueKm = null;

                BigDecimal compoundExposure = immediateCost.multiply(BigDecimal.valueOf(domain.cascadeMultiplier))
                        .setScale(2, RoundingMode.HALF_UP);

                double rmeScore = calculateRmeScore(immediateCost, compoundExposure, daysOverdue, domain.priorityWeight);
                String priority = resolvePriorityBand(domain, daysOverdue);

                backlogItems.add(new DeferredBacklogItemDTO(
                        "MAINT-" + mr.getMaintenanceId(),
                        domain.displayName,
                        mr.getTitle(),
                        "RECORDED_MAINTENANCE",
                        daysOverdue,
                        mileageOverdueKm,
                        immediateCost,
                        domain.cascadeMultiplier,
                        compoundExposure,
                        domain.primaryConsequence,
                        domain.secondaryConsequences,
                        rmeScore,
                        priority,
                        "Explicit maintenance record scheduled for " + scheduled + " is past due."
                ));

                coveredSubsystems.add(domain.name());

            } else if (scheduled.isAfter(today) && !scheduled.isAfter(today.plusDays(30))) {
                // Near-Term Exposure item (upcoming in next 30 days)
                int daysUntilDue = (int) Math.max(0, ChronoUnit.DAYS.between(today, scheduled));
                nearTermItems.add(new NearTermExposureItemDTO(
                        "NEAR-" + mr.getMaintenanceId(),
                        domain.displayName,
                        mr.getTitle(),
                        daysUntilDue,
                        null,
                        immediateCost,
                        "UPCOMING_SCHEDULED",
                        "Scheduled on " + scheduled + " (" + daysUntilDue + " days remaining). Budgetary projection."
                ));

                coveredSubsystems.add(domain.name());
            }
        }

        // Step 2: Evaluate Consumables from ServiceRecords with Deduplication
        List<SubsystemDomain> consumableDomains = List.of(
                SubsystemDomain.ENGINE_OIL_AND_FILTER,
                SubsystemDomain.BRAKING_SYSTEM,
                SubsystemDomain.COOLING_SYSTEM_FLUIDS,
                SubsystemDomain.TIRES_AND_SUSPENSION
        );

        for (SubsystemDomain domain : consumableDomains) {
            // Deduplication: If already covered by an active MaintenanceRecord, DO NOT double count!
            if (coveredSubsystems.contains(domain.name())) {
                log.debug("Consumable domain {} already covered by active MaintenanceRecord. Suppressing duplicate breach.", domain.name());
                continue;
            }

            // Find most recent matching service record
            ServiceRecord latestMatch = null;
            for (ServiceRecord sr : serviceRecords) {
                String srText = (sr.getServiceType() != null ? sr.getServiceType() : "") + " " +
                                (sr.getDescription() != null ? sr.getDescription() : "") + " " +
                                (sr.getNotes() != null ? sr.getNotes() : "");
                if (SubsystemDomain.classify(srText) == domain) {
                    latestMatch = sr;
                    break; // already sorted desc by serviceDate
                }
            }

            boolean isBreached = false;
            int daysOverdue = 0;
            Double mileageOverdueKm = 0.0;

            if (latestMatch == null) {
                // Zero history vehicle or never serviced for this domain
                if (serviceRecords.isEmpty() && maintenanceRecords.isEmpty()) {
                    // Pristine / newly onboarded vehicle with no records: do not penalize aggressively
                    // Only flag if vehicle odometer exceeds domain interval
                    if (currentOdo >= domain.intervalKm) {
                        isBreached = true;
                        mileageOverdueKm = (double) (currentOdo - domain.intervalKm);
                        daysOverdue = 30;
                    }
                } else {
                    isBreached = true;
                    daysOverdue = 45;
                    mileageOverdueKm = (double) Math.max(0, currentOdo - domain.intervalKm);
                }
            } else {
                int srOdo = latestMatch.getOdometerAtService() != null ? Math.max(0, latestMatch.getOdometerAtService()) : 0;
                int kmElapsed = Math.max(0, currentOdo - srOdo);
                int daysElapsed = (int) Math.max(0, ChronoUnit.DAYS.between(latestMatch.getServiceDate(), today));

                if (kmElapsed >= domain.intervalKm || daysElapsed >= domain.intervalDays) {
                    isBreached = true;
                    daysOverdue = Math.max(0, daysElapsed - domain.intervalDays);
                    mileageOverdueKm = (double) Math.max(0, kmElapsed - domain.intervalKm);
                }
            }

            if (isBreached) {
                BigDecimal immediateCost = domain.benchmarkCost;
                BigDecimal compoundExposure = immediateCost.multiply(BigDecimal.valueOf(domain.cascadeMultiplier))
                        .setScale(2, RoundingMode.HALF_UP);

                double rmeScore = calculateRmeScore(immediateCost, compoundExposure, daysOverdue, domain.priorityWeight);
                String priority = resolvePriorityBand(domain, daysOverdue);

                backlogItems.add(new DeferredBacklogItemDTO(
                        "BREACH-" + domain.name(),
                        domain.displayName,
                        domain.displayName + " Interval Exhausted",
                        "BREACHED_INTERVAL",
                        daysOverdue,
                        mileageOverdueKm,
                        immediateCost,
                        domain.cascadeMultiplier,
                        compoundExposure,
                        domain.primaryConsequence,
                        domain.secondaryConsequences,
                        rmeScore,
                        priority,
                        "Historical logbook confirms interval exhaustion without scheduled remediation."
                ));
            }
        }

        // Financial Aggregations
        BigDecimal deferredMaintenanceDebt = BigDecimal.ZERO;
        BigDecimal compoundNeglectCostExposure = BigDecimal.ZERO;

        for (DeferredBacklogItemDTO item : backlogItems) {
            deferredMaintenanceDebt = deferredMaintenanceDebt.add(item.directRemediationCost());
            compoundNeglectCostExposure = compoundNeglectCostExposure.add(item.compoundNeglectCostExposure());
        }

        BigDecimal totalNearTermExposure = BigDecimal.ZERO;
        for (NearTermExposureItemDTO item : nearTermItems) {
            totalNearTermExposure = totalNearTermExposure.add(item.projectedCost());
        }

        BigDecimal total30DayMaintenanceLiability = deferredMaintenanceDebt.add(totalNearTermExposure);

        // Replacement Asset Value & MDI
        BigDecimal replacementAssetValue = calculateVehicleResidualValue(vehicle);

        double rawMdi = (deferredMaintenanceDebt.doubleValue() / Math.max(1000.0, replacementAssetValue.doubleValue())) * 100.0;
        double maintenanceDeficitIndex = Math.min(100.0, Math.round(rawMdi * 100.0) / 100.0);
        String deficitStatus = resolveDeficitStatus(maintenanceDeficitIndex);

        double inactionMultiplier = deferredMaintenanceDebt.compareTo(BigDecimal.ZERO) > 0
                ? Math.round((compoundNeglectCostExposure.doubleValue() / deferredMaintenanceDebt.doubleValue()) * 100.0) / 100.0
                : 1.0;

        // Build Roadmap
        List<BacklogTriageRoadmapItemDTO> triageRoadmap = buildTriageRoadmap(backlogItems);

        // Executive Summary
        String executiveSummary = generateExecutiveSummary(
                vehicle, maintenanceDeficitIndex, deficitStatus, deferredMaintenanceDebt,
                compoundNeglectCostExposure, backlogItems.size(), nearTermItems.size()
        );

        return new VehicleMaintenanceDeficitReportDTO(
                vehicle.getVehicleId(),
                vehicle.getPlateNumber(),
                vehicle.getMake(),
                vehicle.getModel(),
                vehicle.getYear(),
                vehicle.getCurrentOdometer(),
                replacementAssetValue,
                deferredMaintenanceDebt.setScale(2, RoundingMode.HALF_UP),
                maintenanceDeficitIndex,
                deficitStatus,
                compoundNeglectCostExposure.setScale(2, RoundingMode.HALF_UP),
                inactionMultiplier,
                totalNearTermExposure.setScale(2, RoundingMode.HALF_UP),
                total30DayMaintenanceLiability.setScale(2, RoundingMode.HALF_UP),
                backlogItems.size(),
                nearTermItems.size(),
                backlogItems,
                nearTermItems,
                triageRoadmap,
                executiveSummary,
                ANALYTICAL_DISCLAIMER,
                BENCHMARK_POLICY_NOTE
        );
    }

    private double calculateRmeScore(BigDecimal immediateCost, BigDecimal compoundExposure, int daysOverdue, double priorityWeight) {
        BigDecimal netDamagePrevented = compoundExposure.subtract(immediateCost).max(BigDecimal.ZERO);
        double baseRatio = immediateCost.compareTo(BigDecimal.ZERO) > 0
                ? netDamagePrevented.doubleValue() / immediateCost.doubleValue()
                : 0.0;

        // Urgency weight: 1.0 + min(1.5, (daysOverdue / 30.0) * 0.5)
        double urgencyWeight = 1.0 + Math.min(1.5, (Math.max(0, daysOverdue) / 30.0) * 0.5);

        double score = baseRatio * urgencyWeight * priorityWeight;
        return Math.round(score * 100.0) / 100.0;
    }

    private String resolvePriorityBand(SubsystemDomain domain, int daysOverdue) {
        if (domain == SubsystemDomain.BRAKING_SYSTEM || domain == SubsystemDomain.ENGINE_OIL_AND_FILTER || daysOverdue > 60) {
            return "URGENT_REMEDIATION";
        } else if (daysOverdue > 30 || domain == SubsystemDomain.COOLING_SYSTEM_FLUIDS) {
            return "HIGH_RISK";
        } else if (daysOverdue > 14) {
            return "ELEVATED_RISK";
        } else {
            return "MODERATE_RISK";
        }
    }

    private String resolveDeficitStatus(double mdi) {
        if (mdi <= 0.0001) {
            return "PRISTINE";
        } else if (mdi <= 5.0) {
            return "OPTIMAL";
        } else if (mdi <= 15.0) {
            return "FAIR";
        } else if (mdi < 25.0) {
            return "DEFICIENT";
        } else {
            return "CRITICAL";
        }
    }

    private List<BacklogTriageRoadmapItemDTO> buildTriageRoadmap(List<DeferredBacklogItemDTO> backlogItems) {
        if (backlogItems.isEmpty()) {
            return Collections.emptyList();
        }

        List<BacklogTriageRoadmapItemDTO> unsorted = new ArrayList<>();
        for (DeferredBacklogItemDTO item : backlogItems) {
            BigDecimal netSavings = item.compoundNeglectCostExposure().subtract(item.directRemediationCost()).max(BigDecimal.ZERO);
            String action = resolveTriageAction(item);
            String rationale = String.format("Immediate action avoids %.1fx cascade exposure; net savings of $%s.",
                    item.neglectCascadeMultiplier(), netSavings.toPlainString());

            unsorted.add(new BacklogTriageRoadmapItemDTO(
                    0,
                    item.id(),
                    item.taskTitle(),
                    item.subsystem(),
                    item.directRemediationCost(),
                    item.compoundNeglectCostExposure(),
                    netSavings,
                    item.riskMitigationEfficiency(),
                    action,
                    rationale
            ));
        }

        return sortTriageItems(unsorted);
    }

    private List<BacklogTriageRoadmapItemDTO> sortTriageItems(List<BacklogTriageRoadmapItemDTO> items) {
        if (items.isEmpty()) return Collections.emptyList();

        // Deterministic 4-stage tie-breaking:
        // 1. RME Score desc
        // 2. NetDamagePrevented (netSavings) desc
        // 3. ImmediateCost asc
        // 4. TaskTitle asc, then Id asc
        List<BacklogTriageRoadmapItemDTO> sorted = items.stream()
                .sorted(Comparator
                        .comparing(BacklogTriageRoadmapItemDTO::rmeScore, Comparator.reverseOrder())
                        .thenComparing(BacklogTriageRoadmapItemDTO::netSavings, Comparator.reverseOrder())
                        .thenComparing(BacklogTriageRoadmapItemDTO::immediateCost)
                        .thenComparing(BacklogTriageRoadmapItemDTO::taskTitle, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(BacklogTriageRoadmapItemDTO::id)
                )
                .toList();

        // Assign ranks 1 to N
        List<BacklogTriageRoadmapItemDTO> ranked = new ArrayList<>();
        int rank = 1;
        for (BacklogTriageRoadmapItemDTO item : sorted) {
            ranked.add(new BacklogTriageRoadmapItemDTO(
                    rank++,
                    item.id(),
                    item.taskTitle(),
                    item.subsystem(),
                    item.immediateCost(),
                    item.preventedExposure(),
                    item.netSavings(),
                    item.rmeScore(),
                    item.triageAction(),
                    item.urgencyRationale()
            ));
        }
        return ranked;
    }

    private String resolveTriageAction(DeferredBacklogItemDTO item) {
        return switch (item.priority()) {
            case "URGENT_REMEDIATION" -> "Immediate Remediation Required: Book technician service for " + item.subsystem();
            case "HIGH_RISK" -> "High-Priority Service: Schedule remediation within 7 days";
            case "ELEVATED_RISK" -> "Targeted Overhaul: Address prior to long highway journeys";
            default -> "Routine Remediation: Clear task in upcoming maintenance window";
        };
    }

    public BigDecimal calculateVehicleResidualValue(Vehicle vehicle) {
        if (vehicle == null) {
            return BigDecimal.valueOf(10000.00).setScale(2, RoundingMode.HALF_UP);
        }

        String catName = "";
        if (vehicle.getCategory() != null && vehicle.getCategory().getName() != null) {
            catName = vehicle.getCategory().getName().toLowerCase();
        }

        BigDecimal baselineMsrp;
        if (catName.contains("bike") || catName.contains("motorcycle") || catName.contains("scooter") || catName.contains("two-wheeler")) {
            baselineMsrp = BigDecimal.valueOf(5000.00);
        } else if (catName.contains("hatchback")) {
            baselineMsrp = BigDecimal.valueOf(15000.00);
        } else if (catName.contains("sedan")) {
            baselineMsrp = BigDecimal.valueOf(25000.00);
        } else if (catName.contains("suv") || catName.contains("electric") || catName.contains("luxury") || catName.contains("ev") || catName.contains("truck")) {
            baselineMsrp = BigDecimal.valueOf(35000.00);
        } else {
            baselineMsrp = BigDecimal.valueOf(20000.00);
        }

        int currentYear = LocalDate.now().getYear();
        int vehicleYear = (vehicle.getYear() != null && vehicle.getYear() > 1900) ? vehicle.getYear() : (currentYear - 3);
        int age = Math.max(1, currentYear - vehicleYear);

        double retention = 1.0;
        for (int t = 1; t <= age; t++) {
            if (t == 1) {
                retention *= 0.85;
            } else if (t <= 5) {
                retention *= 0.90;
            } else {
                retention *= 0.95;
            }
        }
        retention = Math.max(0.10, retention);

        int odometer = vehicle.getCurrentOdometer() != null ? vehicle.getCurrentOdometer() : 0;
        double annualKm = (double) odometer / age;
        if (annualKm > 20000.0) {
            double excess = (annualKm - 20000.0) / 5000.0;
            retention *= Math.max(0.70, 1.0 - (excess * 0.015));
        }

        BigDecimal residual = baselineMsrp.multiply(BigDecimal.valueOf(retention)).setScale(2, RoundingMode.HALF_UP);
        if (residual.compareTo(BigDecimal.valueOf(1000.00)) < 0) {
            residual = BigDecimal.valueOf(1000.00).setScale(2, RoundingMode.HALF_UP);
        }
        return residual;
    }

    private String generateExecutiveSummary(
            Vehicle vehicle,
            double mdi,
            String deficitStatus,
            BigDecimal debt,
            BigDecimal exposure,
            int backlogCount,
            int nearTermCount
    ) {
        String vehName = vehicle.getMake() + " " + vehicle.getModel() + " (" + vehicle.getPlateNumber() + ")";
        if (backlogCount == 0) {
            return String.format("%s has zero deferred maintenance debt. Posture is PRISTINE with %d upcoming near-term task(s).",
                    vehName, nearTermCount);
        }

        if ("CRITICAL".equals(deficitStatus)) {
            return String.format("%s exhibits an alarming Maintenance Deficit Index of %.1f%% (CRITICAL / Maintenance Bankruptcy). Accrued debt of $%s risks $%s in compound mechanical failures.",
                    vehName, mdi, debt.toPlainString(), exposure.toPlainString());
        } else if ("DEFICIENT".equals(deficitStatus)) {
            return String.format("%s has a DEFICIENT MDI posture of %.1f%% with %d overdue item(s) totalling $%s in deferred liabilities.",
                    vehName, mdi, backlogCount, debt.toPlainString());
        } else if ("FAIR".equals(deficitStatus)) {
            return String.format("%s has a FAIR MDI posture of %.1f%%. Servicing backlog items promptly will preserve residual asset value.",
                    vehName, mdi);
        } else {
            return String.format("%s operates within OPTIMAL MDI limits (%.1f%%) with $%s in deferred maintenance debt.",
                    vehName, mdi, debt.toPlainString());
        }
    }
}
