package com.mygarage.service;

import com.mygarage.dto.response.*;
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
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VehicleReadinessServiceImpl implements VehicleReadinessService {

    public static final String ANALYTICAL_DISCLAIMER =
            "The Trip Readiness Index (TRI) is an analytical readiness estimate based on recorded vehicle history, " +
            "deterministic software rules, and benchmark model assumptions. It is NOT an engineering or mechanical safety guarantee, " +
            "and does not guarantee that the vehicle will safely complete any journey. Always conduct a thorough physical pre-trip inspection.";

    public static final String BENCHMARK_ASSUMPTIONS_NOTE =
            "Benchmark maintenance intervals are configurable model assumptions (Engine Oil: 10,000 km / 180 days; " +
            "Brakes: 20,000 km / 365 days; Coolant: 40,000 km / 730 days; Tires/Suspension: 30,000 km / 540 days), " +
            "not universal OEM manufacturer specifications. Missing historical records produce conservative estimates.";

    private final VehicleRepository vehicleRepository;
    private final ServiceRecordRepository serviceRecordRepository;
    private final MaintenanceRecordRepository maintenanceRecordRepository;
    private final FuelRecordRepository fuelRecordRepository;

    // Subsystem benchmark definitions
    private record SubsystemSpec(
            String name,
            int intervalKm,
            int intervalDays,
            List<String> keywords
    ) {}

    private static final List<SubsystemSpec> SUBSYSTEM_SPECS = List.of(
            new SubsystemSpec("Engine Oil & Filter", 10_000, 180,
                    List.of("oil", "engine oil", "oil filter", "lube", "lubricant", "synthetic oil")),
            new SubsystemSpec("Braking System", 20_000, 365,
                    List.of("brake", "brakes", "brake pad", "brake pads", "rotor", "rotors", "caliper", "brake fluid")),
            new SubsystemSpec("Cooling System & Fluids", 40_000, 730,
                    List.of("coolant", "radiator", "antifreeze", "water pump", "thermostat", "cooling")),
            new SubsystemSpec("Tires & Suspension", 30_000, 540,
                    List.of("tire", "tires", "tyre", "tyres", "alignment", "wheel balancing", "suspension", "shock", "strut", "absorber"))
    );

    @Override
    public VehicleTripReadinessReportDTO evaluateVehicleReadiness(Long vehicleId, Double tripDistanceKm, Integer tripDays, String drivingRegime, Long userId) {
        Vehicle vehicle = vehicleRepository.findByVehicleIdAndUserUserId(vehicleId, userId)
                .orElseThrow(() -> new AccessDeniedException("Access denied: You do not own vehicle ID " + vehicleId));

        // Sanitize & clamp simulation parameters
        double sanitizedDistance = clampDistance(tripDistanceKm);
        int sanitizedDays = clampDays(tripDays);
        String sanitizedRegime = sanitizeRegime(drivingRegime);

        List<ServiceRecord> serviceRecords = serviceRecordRepository.findByVehicleVehicleIdOrderByServiceDateDesc(vehicleId);
        List<MaintenanceRecord> maintenanceRecords = maintenanceRecordRepository.findByVehicleVehicleIdOrderByScheduledDateAsc(vehicleId);
        List<FuelRecord> fuelRecords = fuelRecordRepository.findByVehicleVehicleIdOrderByFuelDateDesc(vehicleId);

        return buildVehicleReadinessReport(vehicle, sanitizedDistance, sanitizedDays, sanitizedRegime, serviceRecords, maintenanceRecords, fuelRecords);
    }

    @Override
    public GarageFleetDispatchReportDTO evaluateFleetDispatch(Double tripDistanceKm, Integer tripDays, String drivingRegime, Long userId) {
        List<Vehicle> userVehicles = vehicleRepository.findByUserUserIdOrderByCreatedAtDesc(userId);

        double sanitizedDistance = clampDistance(tripDistanceKm);
        int sanitizedDays = clampDays(tripDays);
        String sanitizedRegime = sanitizeRegime(drivingRegime);

        if (userVehicles.isEmpty()) {
            return new GarageFleetDispatchReportDTO(
                    sanitizedDistance,
                    sanitizedDays,
                    sanitizedRegime,
                    null,
                    null,
                    "No active vehicles found in garage for dispatch evaluation.",
                    Collections.emptyList(),
                    0,
                    0,
                    ANALYTICAL_DISCLAIMER
            );
        }

        List<FleetDispatchCandidateDTO> candidates = new ArrayList<>();
        int missionReadyCount = 0;

        for (Vehicle v : userVehicles) {
            List<ServiceRecord> services = serviceRecordRepository.findByVehicleVehicleIdOrderByServiceDateDesc(v.getVehicleId());
            List<MaintenanceRecord> maints = maintenanceRecordRepository.findByVehicleVehicleIdOrderByScheduledDateAsc(v.getVehicleId());
            List<FuelRecord> fuels = fuelRecordRepository.findByVehicleVehicleIdOrderByFuelDateDesc(v.getVehicleId());

            VehicleTripReadinessReportDTO report = buildVehicleReadinessReport(v, sanitizedDistance, sanitizedDays, sanitizedRegime, services, maints, fuels);

            if ("MISSION_READY".equals(report.readinessBand())) {
                missionReadyCount++;
            }

            // Calculate dispatch scoring
            double fuelCostVal = report.fuelStaging().estimatedFuelCost().doubleValue();
            double costScore = Math.max(0.0, 100.0 - Math.min(100.0, fuelCostVal * 0.8));
            double compositeDispatchScore = (report.tripReadinessIndex() * 0.65) + (costScore * 0.35);

            String recommendation;
            String rationale;

            if (report.tripReadinessIndex() < 50 || report.criticalActionCount() > 0) {
                recommendation = "HIGH_RISK";
                rationale = "Significant risk: " + report.criticalActionCount() + " critical maintenance or interval breach alerts require resolution.";
            } else if (report.hasMidTripBreach()) {
                recommendation = "NOT_RECOMMENDED";
                rationale = "Maintenance interval will be breached during trip. Service required before dispatch.";
            } else if (report.tripReadinessIndex() >= 85) {
                recommendation = "VIABLE_ALTERNATIVE";
                rationale = "High readiness (" + report.tripReadinessIndex() + "%). Estimated fuel spend: $" + report.fuelStaging().estimatedFuelCost();
            } else {
                recommendation = "VIABLE_ALTERNATIVE";
                rationale = "Moderate readiness (" + report.tripReadinessIndex() + "%). Caution recommended.";
            }

            candidates.add(new FleetDispatchCandidateDTO(
                    v.getVehicleId(),
                    v.getYear() + " " + v.getMake() + " " + v.getModel(),
                    v.getPlateNumber(),
                    v.getCategory() != null ? v.getCategory().getName() : "Standard",
                    v.getCurrentOdometer(),
                    report.tripReadinessIndex(),
                    report.readinessBand(),
                    report.fuelStaging().estimatedFuelCost(),
                    report.fuelStaging().estimatedFuelNeededLiters(),
                    report.criticalActionCount(),
                    report.hasMidTripBreach(),
                    recommendation,
                    rationale
            ));
        }

        // Sort candidates: Highest TRI first, then lowest fuel cost
        candidates.sort((a, b) -> {
            int triCompare = Integer.compare(b.tripReadinessIndex(), a.tripReadinessIndex());
            if (triCompare != 0) return triCompare;
            return a.estimatedTripFuelCost().compareTo(b.estimatedTripFuelCost());
        });

        // Determine optimal vehicle
        Long optimalId = null;
        String optimalName = null;
        String summary = "No vehicle currently meets full mission readiness criteria without inspection.";

        if (!candidates.isEmpty()) {
            FleetDispatchCandidateDTO top = candidates.get(0);
            if (top.tripReadinessIndex() >= 60 && top.criticalIssuesCount() == 0) {
                // Update recommendation to OPTIMAL_CHOICE
                candidates.set(0, new FleetDispatchCandidateDTO(
                        top.vehicleId(),
                        top.vehicleName(),
                        top.licensePlate(),
                        top.categoryName(),
                        top.currentMileage(),
                        top.tripReadinessIndex(),
                        top.readinessBand(),
                        top.estimatedTripFuelCost(),
                        top.estimatedFuelNeededLiters(),
                        top.criticalIssuesCount(),
                        top.hasMidTripBreach(),
                        "OPTIMAL_CHOICE",
                        "Recommended dispatch vehicle: Highest readiness index (" + top.tripReadinessIndex() +
                        "%) with $" + top.estimatedTripFuelCost() + " projected fuel cost."
                ));
                optimalId = top.vehicleId();
                optimalName = top.vehicleName();
                summary = "Optimal vehicle selected: " + optimalName + " with " + top.tripReadinessIndex() + "% readiness index.";
            } else {
                summary = "Caution: Top ranked vehicle (" + top.vehicleName() + ") has active advisories or lower readiness (" +
                        top.tripReadinessIndex() + "%). Pre-trip inspection advised.";
            }
        }

        return new GarageFleetDispatchReportDTO(
                sanitizedDistance,
                sanitizedDays,
                sanitizedRegime,
                optimalId,
                optimalName,
                summary,
                candidates,
                userVehicles.size(),
                missionReadyCount,
                ANALYTICAL_DISCLAIMER
        );
    }

    private VehicleTripReadinessReportDTO buildVehicleReadinessReport(
            Vehicle vehicle,
            double tripDistanceKm,
            int tripDays,
            String drivingRegime,
            List<ServiceRecord> serviceRecords,
            List<MaintenanceRecord> maintenanceRecords,
            List<FuelRecord> fuelRecords
    ) {
        int currentOdo = vehicle.getCurrentOdometer() != null ? vehicle.getCurrentOdometer() : 0;
        double postTripOdo = currentOdo + tripDistanceKm;
        boolean hasHistory = !serviceRecords.isEmpty() || !maintenanceRecords.isEmpty() || !fuelRecords.isEmpty();

        // 1. Evaluate Consumable Reserve Margins & Mid-Trip Breaches
        List<ConsumableReserveMarginDTO> consumableMargins = new ArrayList<>();
        List<String> midTripBreachAlerts = new ArrayList<>();
        int consumablePenalty = 0;

        for (SubsystemSpec spec : SUBSYSTEM_SPECS) {
            ServiceRecord latestMatch = findLatestServiceForSubsystem(serviceRecords, spec.keywords);

            int kmSince;
            int daysSince;

            if (latestMatch != null) {
                int serviceOdo = latestMatch.getOdometerAtService() != null ? latestMatch.getOdometerAtService() : currentOdo;
                kmSince = Math.max(0, currentOdo - serviceOdo);
                daysSince = (int) Math.max(0, ChronoUnit.DAYS.between(latestMatch.getServiceDate(), LocalDate.now()));
            } else {
                // Conservative/uncertain fallback when no matching record exists
                kmSince = Math.min(currentOdo, (int) (spec.intervalKm * 0.65));
                daysSince = (int) (spec.intervalDays * 0.65);
            }

            int remainingMarginKm = Math.max(0, spec.intervalKm - kmSince);
            double remainingMarginPercent = Math.max(0.0, Math.min(100.0, ((double) remainingMarginKm / spec.intervalKm) * 100.0));
            int postTripMarginKm = remainingMarginKm - (int) Math.round(tripDistanceKm);
            boolean willBreach = postTripMarginKm < 0;
            Integer breachAtKm = willBreach ? Math.max(0, remainingMarginKm) : null;

            String statusBand;
            if (willBreach) {
                statusBand = "BREACHED";
                consumablePenalty += 10;
                midTripBreachAlerts.add(String.format("Warning: %s benchmark interval (%d km) will be exceeded at km %d of your %.0f km journey.",
                        spec.name, spec.intervalKm, breachAtKm, tripDistanceKm));
            } else if (postTripMarginKm < spec.intervalKm * 0.15) {
                statusBand = "LOW_RESERVE";
                consumablePenalty += 5;
            } else if (postTripMarginKm < spec.intervalKm * 0.50) {
                statusBand = "ADEQUATE";
            } else {
                statusBand = "OPTIMAL";
            }

            consumableMargins.add(new ConsumableReserveMarginDTO(
                    spec.name,
                    spec.intervalKm,
                    spec.intervalDays,
                    kmSince,
                    daysSince,
                    remainingMarginKm,
                    Math.round(remainingMarginPercent * 10.0) / 10.0,
                    postTripMarginKm,
                    willBreach,
                    breachAtKm,
                    statusBand
            ));
        }

        // 2. Evaluate Maintenance Records
        int maintPenalty = 0;
        List<PreTripChecklistItemDTO> checklist = new ArrayList<>();
        int criticalCount = 0;
        int advisoryCount = 0;
        int passedCount = 0;

        LocalDate tripEndDate = LocalDate.now().plusDays(tripDays);

        for (MaintenanceRecord mr : maintenanceRecords) {
            if (mr.isCompleted()) continue;

            if (mr.getStatus() == MaintenanceStatus.OVERDUE) {
                maintPenalty += 15;
                criticalCount++;
                checklist.add(new PreTripChecklistItemDTO(
                        "MAINTENANCE",
                        "Overdue Maintenance: " + mr.getTitle(),
                        "Task is already past due (" + mr.getScheduledDate() + "). Immediate service required before departure.",
                        "CRITICAL",
                        true
                ));
            } else if (mr.getStatus() == MaintenanceStatus.DUE_TODAY) {
                maintPenalty += 12;
                criticalCount++;
                checklist.add(new PreTripChecklistItemDTO(
                        "MAINTENANCE",
                        "Maintenance Due Today: " + mr.getTitle(),
                        "Task is scheduled for today. Complete before embarking on the journey.",
                        "CRITICAL",
                        true
                ));
            } else if (mr.getStatus() == MaintenanceStatus.UPCOMING) {
                if (mr.getScheduledDate() != null && !mr.getScheduledDate().isAfter(tripEndDate)) {
                    maintPenalty += 10;
                    criticalCount++;
                    midTripBreachAlerts.add(String.format("Scheduled task '%s' will become due on %s during your %d-day trip.",
                            mr.getTitle(), mr.getScheduledDate(), tripDays));
                    checklist.add(new PreTripChecklistItemDTO(
                        "MAINTENANCE",
                        "Imminent Maintenance: " + mr.getTitle(),
                        "Task scheduled for " + mr.getScheduledDate() + " will become due during the planned trip window.",
                        "CRITICAL",
                        true
                    ));
                } else if (mr.getScheduledDate() != null && mr.getScheduledDate().isBefore(LocalDate.now().plusDays(14))) {
                    advisoryCount++;
                    checklist.add(new PreTripChecklistItemDTO(
                            "MAINTENANCE",
                            "Upcoming Maintenance Soon: " + mr.getTitle(),
                            "Due within 14 days (" + mr.getScheduledDate() + "). Consider servicing early.",
                            "ADVISORY",
                            false
                    ));
                }
            }
        }

        // Add consumable checklist items
        for (ConsumableReserveMarginDTO cm : consumableMargins) {
            if ("BREACHED".equals(cm.statusBand())) {
                criticalCount++;
                checklist.add(new PreTripChecklistItemDTO(
                        "CONSUMABLE",
                        cm.subsystemName() + " Interval Exhaustion",
                        "Benchmark interval will be exhausted at km " + cm.breachAtTripKm() + " of trip. Service before trip.",
                        "CRITICAL",
                        true
                ));
            } else if ("LOW_RESERVE".equals(cm.statusBand())) {
                advisoryCount++;
                checklist.add(new PreTripChecklistItemDTO(
                        "CONSUMABLE",
                        cm.subsystemName() + " Low Margin",
                        "Post-trip margin is low (" + cm.postTripMarginKm() + " km remaining). Inspect prior to departure.",
                        "ADVISORY",
                        false
                ));
            } else {
                passedCount++;
                checklist.add(new PreTripChecklistItemDTO(
                        "CONSUMABLE",
                        cm.subsystemName() + " Verified",
                        "Sufficient reserve margin (" + cm.remainingMarginKm() + " km available; " + cm.postTripMarginKm() + " km after trip).",
                        "PASSED",
                        false
                ));
            }
        }

        // Add standard journey safety checklist items
        if (tripDistanceKm >= 500.0) {
            advisoryCount += 2;
            checklist.add(new PreTripChecklistItemDTO(
                    "SAFETY",
                    "Cold Tire Pressure & Tread Depth",
                    "Verify tire inflation pressure to manufacturer specification and check for uneven tread wear.",
                    "ADVISORY",
                    false
            ));
            checklist.add(new PreTripChecklistItemDTO(
                    "SAFETY",
                    "Emergency Roadside Kit & Spare Tire",
                    "Confirm presence and proper pressure of spare tire, jack, wrench, warning triangle, and first-aid kit.",
                    "ADVISORY",
                    false
            ));
        }

        // 3. Odometer & Vehicle Age Stress Penalty
        int stressPenalty = 0;
        int vehicleAge = LocalDate.now().getYear() - (vehicle.getYear() != null ? vehicle.getYear() : LocalDate.now().getYear());
        if (vehicleAge > 10 && tripDistanceKm > 1000.0) {
            stressPenalty += 8;
        }
        if (currentOdo > 200_000) {
            stressPenalty += 5;
        }
        stressPenalty = Math.min(15, stressPenalty);

        // Cap penalties
        consumablePenalty = Math.min(30, consumablePenalty);
        maintPenalty = Math.min(45, maintPenalty);

        int totalPenalty = maintPenalty + consumablePenalty + stressPenalty;
        int tri = Math.max(0, Math.min(100, 100 - totalPenalty));

        String readinessBand;
        String readinessSummary;

        if (tri >= 90) {
            readinessBand = "MISSION_READY";
            readinessSummary = "Vehicle demonstrates high operational readiness for the planned journey. Consumable margins are healthy.";
        } else if (tri >= 75) {
            readinessBand = "GOOD_CONDITION";
            readinessSummary = "Vehicle is in good condition for the journey. Minor advisory checks recommended prior to departure.";
        } else if (tri >= 50) {
            readinessBand = "CAUTION_REQUIRED";
            readinessSummary = "Caution required: Low consumable reserves or upcoming maintenance detected. Pre-trip service advised.";
        } else {
            readinessBand = "HIGH_RISK";
            readinessSummary = "High operational risk: Critical maintenance overdue or interval breach detected. Do not dispatch without service.";
        }

        // 4. Fuel Staging & Cruising Range Model
        TripFuelStagingDTO fuelStaging = calculateFuelStaging(vehicle, tripDistanceKm, drivingRegime, fuelRecords);

        return new VehicleTripReadinessReportDTO(
                vehicle.getVehicleId(),
                vehicle.getYear() + " " + vehicle.getMake() + " " + vehicle.getModel(),
                vehicle.getPlateNumber(),
                vehicle.getCategory() != null ? vehicle.getCategory().getName() : "Standard",
                currentOdo,
                tripDistanceKm,
                tripDays,
                drivingRegime,
                postTripOdo,
                tri,
                readinessBand,
                readinessSummary,
                !midTripBreachAlerts.isEmpty(),
                midTripBreachAlerts,
                consumableMargins,
                fuelStaging,
                checklist,
                criticalCount,
                advisoryCount,
                passedCount,
                ANALYTICAL_DISCLAIMER,
                BENCHMARK_ASSUMPTIONS_NOTE,
                hasHistory
        );
    }

    private TripFuelStagingDTO calculateFuelStaging(
            Vehicle vehicle,
            double tripDistanceKm,
            String drivingRegime,
            List<FuelRecord> fuelRecords
    ) {
        double regimeMultiplier = switch (drivingRegime) {
            case "HIGHWAY_CRUISE" -> 0.90;
            case "CITY_CONGESTED" -> 1.15;
            case "MOUNTAIN_SEVERE" -> 1.25;
            default -> 1.00; // MIXED_BALANCED
        };

        String regimeDesc = switch (drivingRegime) {
            case "HIGHWAY_CRUISE" -> "Highway Cruise (0.90x consumption factor)";
            case "CITY_CONGESTED" -> "City Congested Traffic (1.15x consumption factor)";
            case "MOUNTAIN_SEVERE" -> "Mountain / Severe Duty (1.25x consumption factor)";
            default -> "Mixed Balanced (1.00x consumption factor)";
        };

        boolean hasFuelHistory = false;
        double baselineL100km;
        BigDecimal avgCostPerLiter = BigDecimal.valueOf(1.50); // Sensible baseline

        // Calculate actual historical consumption if available
        List<FuelRecord> validFuel = fuelRecords.stream()
                .filter(f -> f.getEstimatedMileageKmpl() != null && f.getEstimatedMileageKmpl().compareTo(BigDecimal.ZERO) > 0)
                .toList();

        if (!validFuel.isEmpty()) {
            hasFuelHistory = true;
            double avgKmpl = validFuel.stream()
                    .mapToDouble(f -> f.getEstimatedMileageKmpl().doubleValue())
                    .average()
                    .orElse(12.5);

            baselineL100km = 100.0 / Math.max(1.0, avgKmpl);

            double sumCostPerL = fuelRecords.stream()
                    .filter(f -> f.getCostPerLitre() != null)
                    .mapToDouble(f -> f.getCostPerLitre().doubleValue())
                    .average()
                    .orElse(1.50);
            avgCostPerLiter = BigDecimal.valueOf(sumCostPerL).setScale(2, RoundingMode.HALF_UP);
        } else {
            // Category/FuelType defaults
            String categoryName = vehicle.getCategory() != null ? vehicle.getCategory().getName().toLowerCase() : "car";
            if (categoryName.contains("bike") || categoryName.contains("motorcycle") || categoryName.contains("scooter")) {
                baselineL100km = 2.8;
            } else if ("DIESEL".equalsIgnoreCase(vehicle.getFuelType())) {
                baselineL100km = 6.2;
            } else if ("ELECTRIC".equalsIgnoreCase(vehicle.getFuelType())) {
                baselineL100km = 18.0; // kWh/100km
            } else {
                baselineL100km = 7.8;
            }
        }

        double adjustedL100km = Math.round(baselineL100km * regimeMultiplier * 10.0) / 10.0;
        double fuelNeeded = Math.round(((tripDistanceKm * adjustedL100km) / 100.0) * 10.0) / 10.0;
        BigDecimal tripCost = BigDecimal.valueOf(fuelNeeded).multiply(avgCostPerLiter).setScale(2, RoundingMode.HALF_UP);

        // Cruising range based on tank capacity
        double tankCapacity = 50.0;
        String catName = vehicle.getCategory() != null ? vehicle.getCategory().getName().toLowerCase() : "car";
        if (catName.contains("bike") || catName.contains("motorcycle")) {
            tankCapacity = 14.0;
        } else if (!fuelRecords.isEmpty()) {
            double maxQuantity = fuelRecords.stream()
                    .filter(f -> f.getQuantityLitres() != null)
                    .mapToDouble(f -> f.getQuantityLitres().doubleValue())
                    .max()
                    .orElse(45.0);
            tankCapacity = Math.max(tankCapacity, Math.round(maxQuantity * 1.15));
        }

        double cruisingRangeKm = Math.round((tankCapacity / Math.max(0.1, adjustedL100km)) * 100.0);
        double safeRange = cruisingRangeKm * 0.85; // 15% safety buffer
        int stopsRequired = (int) Math.max(0, Math.ceil(tripDistanceKm / Math.max(50.0, safeRange)) - 1);

        String fuelTypeLabel = vehicle.getFuelType() != null ? vehicle.getFuelType() : "Petrol";

        return new TripFuelStagingDTO(
                tripDistanceKm,
                adjustedL100km,
                fuelNeeded,
                tripCost,
                cruisingRangeKm,
                stopsRequired,
                fuelTypeLabel,
                regimeDesc,
                hasFuelHistory
        );
    }

    private ServiceRecord findLatestServiceForSubsystem(List<ServiceRecord> records, List<String> keywords) {
        for (ServiceRecord r : records) {
            String combinedText = ((r.getServiceType() != null ? r.getServiceType() : "") + " " +
                                   (r.getDescription() != null ? r.getDescription() : "")).toLowerCase();

            for (String kw : keywords) {
                // Regex word boundary matching to prevent substring false positives
                Pattern pattern = Pattern.compile("\\b" + Pattern.quote(kw.toLowerCase()) + "\\b");
                if (pattern.matcher(combinedText).find()) {
                    return r;
                }
            }
        }
        return null;
    }

    private double clampDistance(Double distance) {
        if (distance == null || distance < 10.0) return 10.0;
        if (distance > 10_000.0) return 10_000.0;
        return Math.round(distance * 10.0) / 10.0;
    }

    private int clampDays(Integer days) {
        if (days == null || days < 1) return 1;
        if (days > 30) return 30;
        return days;
    }

    private String sanitizeRegime(String regime) {
        if (regime == null) return "HIGHWAY_CRUISE";
        String upper = regime.trim().toUpperCase();
        return switch (upper) {
            case "CITY_CONGESTED", "MOUNTAIN_SEVERE", "MIXED_BALANCED" -> upper;
            default -> "HIGHWAY_CRUISE";
        };
    }
}
