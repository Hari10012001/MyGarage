package com.mygarage.service;

import com.mygarage.dto.request.ScheduleForecastRequestDTO;
import com.mygarage.dto.response.ExpenseForecastDTO;
import com.mygarage.dto.response.PredictiveMaintenanceMilestoneDTO;
import com.mygarage.dto.response.VehicleHealthIndexDTO;
import com.mygarage.dto.response.VehiclePredictiveReportDTO;
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
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PredictiveMaintenanceServiceImpl implements PredictiveMaintenanceService {

    private final VehicleRepository vehicleRepository;
    private final ServiceRecordRepository serviceRecordRepository;
    private final FuelRecordRepository fuelRecordRepository;
    private final MaintenanceRecordRepository maintenanceRecordRepository;

    private static final List<MaintenanceStatus> PENDING_STATUSES = Arrays.asList(
            MaintenanceStatus.UPCOMING,
            MaintenanceStatus.OVERDUE,
            MaintenanceStatus.DUE_TODAY
    );

    @Override
    public VehiclePredictiveReportDTO getVehicleForecast(Long vehicleId, Long userId) {
        Vehicle vehicle = vehicleRepository.findByVehicleIdAndUserUserId(vehicleId, userId)
                .orElseThrow(() -> new AccessDeniedException("Access denied: You do not own vehicle ID " + vehicleId));

        int currentOdometer = vehicle.getCurrentOdometer() != null ? vehicle.getCurrentOdometer() : 0;
        boolean isTwoWheeler = isTwoWheeler(vehicle);

        // 1. Calculate Daily Driving Velocity (km/day) with rollback protection
        VelocityComputation velocityResult = computeVelocity(vehicleId, isTwoWheeler);

        // 2. Reference Cost Averages
        BigDecimal avgServiceCost = computeAverageServiceCost(vehicleId, isTwoWheeler);
        BigDecimal avgFuelCostPerKm = computeAverageFuelCostPerKm(vehicleId, currentOdometer, isTwoWheeler);

        // 3. Repeating Periodic Maintenance Schedule (PMS) Milestones
        List<PredictiveMaintenanceMilestoneDTO> milestones = computeRepeatingMilestones(
                vehicleId, currentOdometer, velocityResult.velocity, avgServiceCost
        );

        // 4. Algorithmic Vehicle Health Index (Clamped strictly to 0 - 100)
        VehicleHealthIndexDTO healthIndex = computeVehicleHealthIndex(vehicleId, currentOdometer, milestones);

        // 5. Forward Expense Budgets (30, 60, 90, 180 Days)
        List<ExpenseForecastDTO> expenseForecasts = computeExpenseForecasts(
                velocityResult.velocity, avgFuelCostPerKm, milestones
        );

        return VehiclePredictiveReportDTO.builder()
                .vehicleId(vehicle.getVehicleId())
                .plateNumber(vehicle.getPlateNumber())
                .make(vehicle.getMake())
                .model(vehicle.getModel())
                .year(vehicle.getYear())
                .color(vehicle.getColor())
                .categoryName(vehicle.getCategory() != null ? vehicle.getCategory().getName() : "General")
                .fuelType(vehicle.getFuelType())
                .currentOdometer(currentOdometer)
                .dailyDrivingVelocityKm(velocityResult.velocity)
                .velocityConfidence(velocityResult.confidence)
                .odometerSamplePoints(velocityResult.samplePoints)
                .healthIndex(healthIndex)
                .milestones(milestones)
                .expenseForecasts(expenseForecasts)
                .averageFuelCostPerKm(avgFuelCostPerKm)
                .averageServiceCost(avgServiceCost)
                .build();
    }

    @Override
    public List<VehiclePredictiveReportDTO> getGarageFleetForecast(Long userId) {
        List<Vehicle> vehicles = vehicleRepository.findByUserUserIdOrderByCreatedAtDesc(userId);
        return vehicles.stream()
                .map(v -> getVehicleForecast(v.getVehicleId(), userId))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public MaintenanceRecord scheduleForecastedMilestone(Long vehicleId, ScheduleForecastRequestDTO request, Long userId) {
        Vehicle vehicle = vehicleRepository.findByVehicleIdAndUserUserId(vehicleId, userId)
                .orElseThrow(() -> new AccessDeniedException("Access denied: You do not own vehicle ID " + vehicleId));

        // Safeguard: Prevent duplicate scheduling of the same pending forecast milestone
        if (maintenanceRecordRepository.existsByVehicleVehicleIdAndTitleIgnoreCaseAndStatusIn(
                vehicleId, request.getTitle(), PENDING_STATUSES)) {
            throw new IllegalStateException("A maintenance task with title '" + request.getTitle() +
                    "' is already scheduled or pending for this vehicle.");
        }

        MaintenanceRecord record = new MaintenanceRecord();
        record.setVehicle(vehicle);
        record.setTitle(request.getTitle().trim());
        record.setDescription(request.getDescription() != null ? request.getDescription().trim() : "Scheduled via Predictive Service Planner");
        record.setScheduledDate(request.getScheduledDate());
        record.setCost(request.getEstimatedCost() != null ? request.getEstimatedCost() : BigDecimal.ZERO);
        record.setStatus(MaintenanceService.computeStatus(request.getScheduledDate(), null));

        return maintenanceRecordRepository.save(record);
    }

    // =========================================================================
    // VELOCITY ENGINE (WITH ROLLBACK / ANOMALY SAFEGUARD)
    // =========================================================================

    private static class OdometerPoint {
        final LocalDate date;
        final int odometer;

        OdometerPoint(LocalDate date, int odometer) {
            this.date = date;
            this.odometer = odometer;
        }
    }

    private static class VelocityComputation {
        final double velocity;
        final String confidence;
        final int samplePoints;

        VelocityComputation(double velocity, String confidence, int samplePoints) {
            this.velocity = velocity;
            this.confidence = confidence;
            this.samplePoints = samplePoints;
        }
    }

    private VelocityComputation computeVelocity(Long vehicleId, boolean isTwoWheeler) {
        List<FuelRecord> fuelLogs = fuelRecordRepository.findByVehicleVehicleIdOrderByFuelDateDesc(vehicleId);
        List<ServiceRecord> services = serviceRecordRepository.findByVehicleVehicleIdOrderByServiceDateDesc(vehicleId);

        List<OdometerPoint> points = new ArrayList<>();
        for (FuelRecord f : fuelLogs) {
            if (f.getOdometerAtFill() != null && f.getOdometerAtFill() > 0 && f.getFuelDate() != null) {
                points.add(new OdometerPoint(f.getFuelDate(), f.getOdometerAtFill()));
            }
        }
        for (ServiceRecord s : services) {
            if (s.getOdometerAtService() != null && s.getOdometerAtService() > 0 && s.getServiceDate() != null) {
                points.add(new OdometerPoint(s.getServiceDate(), s.getOdometerAtService()));
            }
        }

        // Sort chronologically ascending
        points.sort(Comparator.comparing(p -> p.date));

        int totalPositiveDistance = 0;
        long totalElapsedDays = 0;
        int validIntervals = 0;

        if (points.size() >= 2) {
            OdometerPoint lastValid = points.get(0);
            for (int i = 1; i < points.size(); i++) {
                OdometerPoint curr = points.get(i);
                int distDelta = curr.odometer - lastValid.odometer;
                long daysDelta = ChronoUnit.DAYS.between(lastValid.date, curr.date);

                // Safeguard: Treat negative odometer deltas as invalid/anomalous readings; never convert rollback into positive distance
                if (distDelta > 0 && daysDelta > 0) {
                    totalPositiveDistance += distDelta;
                    totalElapsedDays += daysDelta;
                    validIntervals++;
                    lastValid = curr;
                } else if (distDelta < 0) {
                    log.warn("Odometer rollback/anomaly detected for vehicle {}: from {} to {} on {}. Step ignored.",
                            vehicleId, lastValid.odometer, curr.odometer, curr.date);
                }
            }
        }

        if (totalPositiveDistance > 0 && totalElapsedDays > 0) {
            double rawVelocity = (double) totalPositiveDistance / (double) totalElapsedDays;
            // Clamp velocity between realistic limits: 5 km/day to 500 km/day
            double velocity = Math.max(5.0, Math.min(500.0, Math.round(rawVelocity * 10.0) / 10.0));
            String confidence = points.size() >= 3 ? "HIGH" : "MODERATE";
            return new VelocityComputation(velocity, confidence, points.size());
        }

        // Fallback default estimate if insufficient valid chronological intervals
        double defaultVelocity = isTwoWheeler ? 15.0 : 25.0;
        return new VelocityComputation(defaultVelocity, "DEFAULT_ESTIMATE", points.size());
    }

    // =========================================================================
    // REPEATING PERIODIC MAINTENANCE SCHEDULE (PMS) MILESTONES
    // =========================================================================

    private static class MilestoneTemplate {
        final int intervalKm;
        final String serviceName;
        final String description;
        final double costMultiplier;

        MilestoneTemplate(int intervalKm, String serviceName, String description, double costMultiplier) {
            this.intervalKm = intervalKm;
            this.serviceName = serviceName;
            this.description = description;
            this.costMultiplier = costMultiplier;
        }
    }

    private List<PredictiveMaintenanceMilestoneDTO> computeRepeatingMilestones(
            Long vehicleId, int currentOdometer, double velocity, BigDecimal baseServiceCost) {

        List<MilestoneTemplate> templates = Arrays.asList(
                new MilestoneTemplate(5000, "Engine Oil & Filter Service",
                        "Replace engine oil, change oil filter, top up fluids, multi-point visual inspection.", 0.8),
                new MilestoneTemplate(10000, "Tire Rotation, Alignment & Inspection",
                        "Rotate tires across all axles, wheel balancing, brake pads visual inspection.", 0.6),
                new MilestoneTemplate(20000, "Brake System & Filters Overhaul",
                        "Brake pad wear replacement, brake fluid flush, replace engine air and cabin pollen filters.", 1.5),
                new MilestoneTemplate(40000, "Major Transmission & Coolant Service",
                        "Transmission fluid drain & refill, radiator coolant flush, inspect serpentine drive belts.", 2.2),
                new MilestoneTemplate(60000, "Comprehensive Suspension & Spark Plugs",
                        "Replace spark plugs, fuel filter replacement, shock absorber and suspension bushings inspection.", 3.0)
        );

        List<PredictiveMaintenanceMilestoneDTO> results = new ArrayList<>();

        for (MilestoneTemplate tmpl : templates) {
            // Calculate next repeating multiple of interval strictly greater than current odometer
            // Formula: Target = (floor(currentOdo / interval) + 1) * interval
            int targetOdometer;
            if (currentOdometer <= 0) {
                targetOdometer = tmpl.intervalKm;
            } else {
                targetOdometer = ((currentOdometer / tmpl.intervalKm) + 1) * tmpl.intervalKm;
            }

            int kilometersRemaining = Math.max(0, targetOdometer - currentOdometer);
            long daysRemaining = Math.max(1, Math.round((double) kilometersRemaining / velocity));
            LocalDate estimatedDueDate = LocalDate.now().plusDays(daysRemaining);

            String urgency;
            if (kilometersRemaining <= 0) {
                urgency = "DUE_NOW";
            } else if (kilometersRemaining <= 800 || daysRemaining <= 14) {
                urgency = "DUE_SOON";
            } else {
                urgency = "UPCOMING";
            }

            BigDecimal estimatedCost = baseServiceCost
                    .multiply(BigDecimal.valueOf(tmpl.costMultiplier))
                    .setScale(2, RoundingMode.HALF_UP);

            // Check if matching pending maintenance task already exists
            Optional<MaintenanceRecord> existingTask = maintenanceRecordRepository
                    .findFirstByVehicleVehicleIdAndTitleIgnoreCaseAndStatusIn(
                            vehicleId, tmpl.serviceName, PENDING_STATUSES);

            boolean isScheduled = existingTask.isPresent();
            Long existingId = existingTask.map(MaintenanceRecord::getMaintenanceId).orElse(null);

            results.add(PredictiveMaintenanceMilestoneDTO.builder()
                    .intervalKm(tmpl.intervalKm)
                    .targetOdometer(targetOdometer)
                    .kilometersRemaining(kilometersRemaining)
                    .serviceName(tmpl.serviceName)
                    .description(tmpl.description)
                    .urgency(urgency)
                    .estimatedDueDate(estimatedDueDate)
                    .daysRemaining(daysRemaining)
                    .estimatedCost(estimatedCost)
                    .isScheduled(isScheduled)
                    .existingMaintenanceId(existingId)
                    .build());
        }

        // Sort milestones by target odometer ascending
        results.sort(Comparator.comparingInt(PredictiveMaintenanceMilestoneDTO::getTargetOdometer));
        return results;
    }

    // =========================================================================
    // VEHICLE HEALTH INDEX (0 - 100 STRICT CLAMP)
    // =========================================================================

    private VehicleHealthIndexDTO computeVehicleHealthIndex(
            Long vehicleId, int currentOdometer, List<PredictiveMaintenanceMilestoneDTO> milestones) {

        int overdueCount = (int) maintenanceRecordRepository
                .countByVehicleVehicleIdAndStatus(vehicleId, MaintenanceStatus.OVERDUE);

        long upcomingCount = maintenanceRecordRepository
                .countByVehicleVehicleIdAndStatus(vehicleId, MaintenanceStatus.UPCOMING);

        // Distance driven since most recent service record
        List<ServiceRecord> services = serviceRecordRepository.findByVehicleVehicleIdOrderByServiceDateDesc(vehicleId);
        int kmSinceLastService = currentOdometer;
        if (!services.isEmpty() && services.get(0).getOdometerAtService() != null) {
            kmSinceLastService = Math.max(0, currentOdometer - services.get(0).getOdometerAtService());
        }

        // 1. Service Freshness Score (0 - 35 points)
        int freshnessScore;
        if (kmSinceLastService <= 4000) {
            freshnessScore = 35;
        } else if (kmSinceLastService <= 8000) {
            freshnessScore = 25;
        } else if (kmSinceLastService <= 12000) {
            freshnessScore = 15;
        } else {
            freshnessScore = 5;
        }

        // 2. Maintenance Compliance Score (0 - 45 points base)
        // Overdue task penalty: -25 points each
        int overduePenalty = overdueCount * 25;
        int complianceScore = Math.max(0, 45 - overduePenalty);

        // 3. Mileage & Log Stability Score (0 - 20 points)
        long fuelLogCount = fuelRecordRepository.countByVehicleVehicleId(vehicleId);
        int stabilityScore = fuelLogCount >= 3 ? 20 : (fuelLogCount > 0 ? 12 : 5);

        // Pending uncompleted task minor deduction (-3 per scheduled item)
        int pendingDeduction = (int) Math.min(10, upcomingCount * 3);

        // Raw score combines sub-scores and applies full overdue penalties
        int rawScore = freshnessScore + (45 - overduePenalty) + stabilityScore - pendingDeduction;

        // Safeguard: Clamp Vehicle Health Index strictly to 0–100
        int healthScore = Math.max(0, Math.min(100, rawScore));

        String rating;
        String badgeColor;
        String recommendation;

        if (healthScore >= 90) {
            rating = "EXCELLENT";
            badgeColor = "success";
            recommendation = "Vehicle is in peak operational health. Routine fluid inspections recommended.";
        } else if (healthScore >= 75) {
            rating = "GOOD";
            badgeColor = "info";
            recommendation = "Vehicle running soundly. Keep an eye on upcoming periodic service milestones.";
        } else if (healthScore >= 50) {
            rating = "FAIR";
            badgeColor = "warning";
            recommendation = "Service attention due soon. Overdue tasks or high mileage since last service detected.";
        } else {
            rating = "ATTENTION_REQUIRED";
            badgeColor = "danger";
            recommendation = "Immediate service required. Overdue maintenance tasks detected. Risk of mechanical wear.";
        }

        return VehicleHealthIndexDTO.builder()
                .healthScore(healthScore)
                .healthRating(rating)
                .badgeColor(badgeColor)
                .overdueTasksCount(overdueCount)
                .kmSinceLastService(kmSinceLastService)
                .serviceFreshnessScore(freshnessScore)
                .maintenanceComplianceScore(complianceScore)
                .mileageStabilityScore(stabilityScore)
                .recommendationSummary(recommendation)
                .build();
    }

    // =========================================================================
    // FORWARD EXPENSE BUDGET FORECAST (30, 60, 90, 180 DAYS)
    // =========================================================================

    private List<ExpenseForecastDTO> computeExpenseForecasts(
            double velocity, BigDecimal avgFuelCostPerKm, List<PredictiveMaintenanceMilestoneDTO> milestones) {

        int[] horizons = {30, 60, 90, 180};
        List<ExpenseForecastDTO> forecasts = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int days : horizons) {
            LocalDate horizonCutoff = today.plusDays(days);
            int projectedMileage = (int) Math.round(velocity * days);

            BigDecimal projectedFuelSpend = avgFuelCostPerKm
                    .multiply(BigDecimal.valueOf(projectedMileage))
                    .setScale(2, RoundingMode.HALF_UP);

            // Safeguard: Include only maintenance milestones whose projected due dates fall within each expense-forecast horizon
            List<PredictiveMaintenanceMilestoneDTO> dueInWindow = milestones.stream()
                    .filter(m -> m.getEstimatedDueDate() != null && !m.getEstimatedDueDate().isAfter(horizonCutoff))
                    .collect(Collectors.toList());

            BigDecimal projectedMaintSpend = dueInWindow.stream()
                    .map(PredictiveMaintenanceMilestoneDTO::getEstimatedCost)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalProjectedSpend = projectedFuelSpend.add(projectedMaintSpend);

            forecasts.add(ExpenseForecastDTO.builder()
                    .periodDays(days)
                    .projectedMileageKm(projectedMileage)
                    .projectedFuelSpend(projectedFuelSpend)
                    .projectedMaintenanceSpend(projectedMaintSpend)
                    .totalProjectedSpend(totalProjectedSpend)
                    .dueMilestonesCount(dueInWindow.size())
                    .build());
        }

        return forecasts;
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private boolean isTwoWheeler(Vehicle vehicle) {
        if (vehicle.getCategory() != null && vehicle.getCategory().getName() != null) {
            String cat = vehicle.getCategory().getName().toLowerCase();
            return cat.contains("bike") || cat.contains("motorcycle") || cat.contains("scooter") || cat.contains("two");
        }
        return false;
    }

    private BigDecimal computeAverageServiceCost(Long vehicleId, boolean isTwoWheeler) {
        List<ServiceRecord> services = serviceRecordRepository.findByVehicleVehicleIdOrderByServiceDateDesc(vehicleId);
        if (!services.isEmpty()) {
            BigDecimal sum = services.stream()
                    .map(ServiceRecord::getCost)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (sum.compareTo(BigDecimal.ZERO) > 0) {
                return sum.divide(BigDecimal.valueOf(services.size()), 2, RoundingMode.HALF_UP);
            }
        }
        return isTwoWheeler ? new BigDecimal("1800.00") : new BigDecimal("4500.00");
    }

    private BigDecimal computeAverageFuelCostPerKm(Long vehicleId, int currentOdometer, boolean isTwoWheeler) {
        BigDecimal totalFuelSpend = fuelRecordRepository.sumTotalCostByVehicleId(vehicleId);
        if (totalFuelSpend != null && totalFuelSpend.compareTo(BigDecimal.ZERO) > 0 && currentOdometer > 0) {
            return totalFuelSpend.divide(BigDecimal.valueOf(currentOdometer), 2, RoundingMode.HALF_UP);
        }
        return isTwoWheeler ? new BigDecimal("2.20") : new BigDecimal("5.50");
    }
}
