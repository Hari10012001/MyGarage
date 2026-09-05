package com.mygarage.service;

import com.mygarage.dto.response.*;
import com.mygarage.model.FuelRecord;
import com.mygarage.model.MaintenanceRecord;
import com.mygarage.model.ServiceRecord;
import com.mygarage.model.User;
import com.mygarage.model.Vehicle;
import com.mygarage.model.enums.MaintenanceStatus;
import com.mygarage.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * M20: Implementation of Vehicle Operational Budgeting, Predictive Cash-Flow Forecast
 * & Maintenance Expense Burn-Rate Engine (Fleet Fiscal Management).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VehicleFiscalBudgetServiceImpl implements VehicleFiscalBudgetService {

    private final VehicleRepository vehicleRepository;
    private final ServiceRecordRepository serviceRecordRepository;
    private final FuelRecordRepository fuelRecordRepository;
    private final MaintenanceRecordRepository maintenanceRecordRepository;
    private final UserRepository userRepository;
    private final PredictiveMaintenanceService predictiveMaintenanceService;

    // --- IMMUTABLE MAINTENANCE BENCHMARK COSTS ($ / USD) ---
    public static final BigDecimal BENCHMARK_OIL_FILTER = new BigDecimal("95.00");
    public static final BigDecimal BENCHMARK_BRAKES = new BigDecimal("180.00");
    public static final BigDecimal BENCHMARK_COOLANT = new BigDecimal("140.00");
    public static final BigDecimal BENCHMARK_TIRES = new BigDecimal("160.00");
    public static final BigDecimal BENCHMARK_MAJOR_PMS = new BigDecimal("320.00");
    public static final BigDecimal BENCHMARK_GENERAL_FALLBACK = new BigDecimal("110.00");

    // --- IMMUTABLE CATEGORY FUEL BENCHMARKS (Monthly Baseline Spend $ / USD) ---
    public static final BigDecimal FUEL_BENCHMARK_CAR = new BigDecimal("89.29");       // 14.0 km/L, $1.25/L, 1000 km
    public static final BigDecimal FUEL_BENCHMARK_SUV_TRUCK = new BigDecimal("122.73"); // 11.0 km/L, $1.35/L, 1000 km
    public static final BigDecimal FUEL_BENCHMARK_BIKE = new BigDecimal("16.45");      // 38.0 km/L, $1.25/L, 500 km
    public static final BigDecimal FUEL_BENCHMARK_SCOOTER = new BigDecimal("14.29");   // 35.0 km/L, $1.25/L, 400 km
    public static final BigDecimal FUEL_BENCHMARK_EV = new BigDecimal("30.00");        // 0 km/L, EV charging baseline
    public static final BigDecimal FUEL_BENCHMARK_DEFAULT = new BigDecimal("100.00");   // 13.0 km/L, $1.30/L, 1000 km

    public static final BigDecimal LIQUIDITY_BUFFER_FLOOR = new BigDecimal("250.00");
    public static final String CURRENCY_SYMBOL = "$";
    public static final String LEGAL_DISCLAIMER =
            "The Recommended Emergency Maintenance Liquidity Buffer and 12-Month Operating Budget Forecast are deterministic analytical heuristics calculated by MyGarage based on recorded vehicle history, established maintenance schedules, and benchmark assumptions. They do not constitute financial advice, an actuarial calculation, or a mechanical guarantee.";

    private static final DateTimeFormatter MONTH_LABEL_FMT = DateTimeFormatter.ofPattern("MMM yyyy");

    @Override
    public VehicleFiscalBudgetReportDTO getVehicleFiscalBudgetReport(Long vehicleId, String userEmail) {
        User user = getUserByEmail(userEmail);
        enforceNonAdmin(user);

        Vehicle vehicle = vehicleRepository.findByVehicleIdAndUserUserId(vehicleId, user.getUserId())
                .orElseThrow(() -> new AccessDeniedException("Access denied: You do not own vehicle ID " + vehicleId));

        return computeVehicleReport(vehicle, user);
    }

    @Override
    public GarageFiscalBudgetMatrixDTO getGarageFiscalBudgetMatrix(String userEmail) {
        User user = getUserByEmail(userEmail);
        enforceNonAdmin(user);

        List<Vehicle> vehicles = vehicleRepository.findByUserUserIdOrderByCreatedAtDesc(user.getUserId());
        if (vehicles.isEmpty()) {
            return new GarageFiscalBudgetMatrixDTO(
                    user.getUserId(),
                    0,
                    CURRENCY_SYMBOL,
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    Collections.emptyList(),
                    generateEmptyConsolidatedForecast(),
                    LEGAL_DISCLAIMER
            );
        }

        List<VehicleFiscalBudgetReportDTO> vehicleReports = vehicles.stream()
                .map(v -> computeVehicleReport(v, user))
                .collect(Collectors.toList());

        BigDecimal totalPortfolioMonthlyBurn = vehicleReports.stream()
                .map(VehicleFiscalBudgetReportDTO::rollingMonthlyBurnRate)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalPortfolioAnnualProjected = vehicleReports.stream()
                .map(VehicleFiscalBudgetReportDTO::projectedNextYearExpense)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalRecommendedFleetBuffer = vehicleReports.stream()
                .map(VehicleFiscalBudgetReportDTO::recommendedLiquidityBuffer)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        List<FleetVehicleBudgetItemDTO> vehicleBreakdown = new ArrayList<>();
        for (VehicleFiscalBudgetReportDTO r : vehicleReports) {
            BigDecimal budgetShare = BigDecimal.ZERO;
            if (totalPortfolioMonthlyBurn.compareTo(BigDecimal.ZERO) > 0) {
                budgetShare = r.rollingMonthlyBurnRate()
                        .multiply(new BigDecimal("100.0"))
                        .divide(totalPortfolioMonthlyBurn, 1, RoundingMode.HALF_UP);
            }
            vehicleBreakdown.add(new FleetVehicleBudgetItemDTO(
                    r.vehicleId(),
                    r.vehicleName(),
                    r.plateNumber(),
                    r.rollingMonthlyBurnRate(),
                    r.projectedNextYearExpense(),
                    r.recommendedLiquidityBuffer(),
                    budgetShare,
                    r.volatilityTier()
            ));
        }

        // Consolidated 12-Month Forecast
        List<MonthlyCashFlowPointDTO> consolidatedForecast = consolidateFleetForecast(vehicleReports);

        return new GarageFiscalBudgetMatrixDTO(
                user.getUserId(),
                vehicles.size(),
                CURRENCY_SYMBOL,
                totalPortfolioMonthlyBurn,
                totalPortfolioAnnualProjected,
                totalRecommendedFleetBuffer,
                vehicleBreakdown,
                consolidatedForecast,
                LEGAL_DISCLAIMER
        );
    }

    private VehicleFiscalBudgetReportDTO computeVehicleReport(Vehicle vehicle, User user) {
        Long vehicleId = vehicle.getVehicleId();
        List<ServiceRecord> services = serviceRecordRepository.findByVehicleVehicleIdOrderByServiceDateDesc(vehicleId);
        List<FuelRecord> fuels = fuelRecordRepository.findByVehicleVehicleIdOrderByFuelDateDesc(vehicleId);
        List<MaintenanceRecord> maintenances = maintenanceRecordRepository.findByVehicleVehicleIdOrderByScheduledDateAsc(vehicleId);

        // 1. Determine deterministic history start date & completed months window
        LocalDate historyStartDate = resolveHistoryStartDate(vehicle, services, fuels, maintenances);
        LocalDate today = LocalDate.now();
        YearMonth currentYearMonth = YearMonth.from(today);
        YearMonth startYearMonth = YearMonth.from(historyStartDate);

        int monthsActive = (int) ChronoUnit.MONTHS.between(startYearMonth, currentYearMonth);
        if (monthsActive < 0) monthsActive = 0;

        int windowN = Math.min(12, Math.max(1, monthsActive));

        // 2. Perform deterministic historical event deduplication between completed maintenance and service records
        List<DeduplicatedHistoricalEvent> deduplicatedEvents = deduplicateHistoricalEvents(services, maintenances);

        // 3. Construct complete calendar window (excluding current partial month)
        List<HistoricalSpendPointDTO> historicalSpendTrend = new ArrayList<>();
        List<YearMonth> completedMonths = new ArrayList<>();
        for (int i = windowN; i >= 1; i--) {
            completedMonths.add(currentYearMonth.minusMonths(i));
        }

        BigDecimal totalWindowSpend = BigDecimal.ZERO;
        BigDecimal totalWindowFuel = BigDecimal.ZERO;
        BigDecimal totalWindowSched = BigDecimal.ZERO;
        BigDecimal totalWindowRepair = BigDecimal.ZERO;
        BigDecimal peakSingleSpend = BigDecimal.ZERO;

        List<BigDecimal> monthlyTotalSpends = new ArrayList<>();

        for (YearMonth ym : completedMonths) {
            LocalDate monthStart = ym.atDay(1);
            LocalDate monthEnd = ym.atEndOfMonth();

            BigDecimal monthFuel = BigDecimal.ZERO;
            for (FuelRecord f : fuels) {
                if (!f.getFuelDate().isBefore(monthStart) && !f.getFuelDate().isAfter(monthEnd)) {
                    BigDecimal cost = f.getTotalCost() != null ? f.getTotalCost() : BigDecimal.ZERO;
                    monthFuel = monthFuel.add(cost);
                    if (cost.compareTo(peakSingleSpend) > 0) peakSingleSpend = cost;
                }
            }

            BigDecimal monthSched = BigDecimal.ZERO;
            BigDecimal monthRepair = BigDecimal.ZERO;

            for (DeduplicatedHistoricalEvent evt : deduplicatedEvents) {
                if (!evt.eventDate.isBefore(monthStart) && !evt.eventDate.isAfter(monthEnd)) {
                    if (evt.isCorrective) {
                        monthRepair = monthRepair.add(evt.cost);
                    } else {
                        monthSched = monthSched.add(evt.cost);
                    }
                    if (evt.cost.compareTo(peakSingleSpend) > 0) peakSingleSpend = evt.cost;
                }
            }

            BigDecimal monthTotal = monthFuel.add(monthSched).add(monthRepair);
            historicalSpendTrend.add(new HistoricalSpendPointDTO(
                    ym.format(MONTH_LABEL_FMT),
                    monthFuel.setScale(2, RoundingMode.HALF_UP),
                    monthRepair.setScale(2, RoundingMode.HALF_UP),
                    monthSched.setScale(2, RoundingMode.HALF_UP),
                    monthTotal.setScale(2, RoundingMode.HALF_UP)
            ));

            totalWindowSpend = totalWindowSpend.add(monthTotal);
            totalWindowFuel = totalWindowFuel.add(monthFuel);
            totalWindowSched = totalWindowSched.add(monthSched);
            totalWindowRepair = totalWindowRepair.add(monthRepair);
            monthlyTotalSpends.add(monthTotal);
        }

        // 4. Burn Rates
        BigDecimal divisorN = new BigDecimal(windowN);
        BigDecimal rollingMonthlyBurnRate = totalWindowSpend.divide(divisorN, 2, RoundingMode.HALF_UP);
        BigDecimal fuelBurnRate = totalWindowFuel.divide(divisorN, 2, RoundingMode.HALF_UP);
        BigDecimal maintenanceBurnRate = totalWindowSched.divide(divisorN, 2, RoundingMode.HALF_UP);
        BigDecimal repairBurnRate = totalWindowRepair.divide(divisorN, 2, RoundingMode.HALF_UP);

        BigDecimal dailyBurnRate = rollingMonthlyBurnRate.divide(new BigDecimal("30.4375"), 2, RoundingMode.HALF_UP);

        // Distance Intensity & Odometer Anomaly Handling
        DistanceIntensityResult distanceResult = computeDistanceIntensity(vehicle, completedMonths, totalWindowSpend, services, fuels);

        // 5. Expenditure Volatility & Risk Index (EVRI 0–100)
        EvriResult evriResult = computeEvri(monthlyTotalSpends, rollingMonthlyBurnRate, windowN);

        // 6. Recommended Liquidity Buffer
        BigDecimal recommendedLiquidityBuffer = computeLiquidityBuffer(
                vehicle, peakSingleSpend, rollingMonthlyBurnRate, evriResult.evri
        );

        // 7. Data Confidence & Evidence Metadata
        FiscalDataConfidenceDTO dataConfidence = computeDataConfidence(
                fuels, services, maintenances, windowN, distanceResult
        );

        // 8. 12-Month Forward Predictive Cash-Flow Forecast (Reusing M14 Milestones & Fuel Fallback)
        List<MonthlyCashFlowPointDTO> twelveMonthForecast = computeTwelveMonthForecast(
                vehicle, user.getUserId(), fuels, maintenances, dataConfidence.fuelEstimationTier()
        );

        BigDecimal nextMonthExpense = twelveMonthForecast.isEmpty() ? BigDecimal.ZERO : twelveMonthForecast.get(0).totalProjectedExpense();
        BigDecimal nextQuarterExpense = twelveMonthForecast.stream().limit(3)
                .map(MonthlyCashFlowPointDTO::totalProjectedExpense)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        BigDecimal nextYearExpense = twelveMonthForecast.stream().limit(12)
                .map(MonthlyCashFlowPointDTO::totalProjectedExpense)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);

        return new VehicleFiscalBudgetReportDTO(
                vehicle.getVehicleId(),
                vehicle.getDisplayName(),
                vehicle.getPlateNumber(),
                CURRENCY_SYMBOL,
                rollingMonthlyBurnRate,
                dailyBurnRate,
                distanceResult.distanceIntensity,
                fuelBurnRate,
                maintenanceBurnRate,
                repairBurnRate,
                evriResult.evri,
                evriResult.tier,
                recommendedLiquidityBuffer,
                peakSingleSpend.setScale(2, RoundingMode.HALF_UP),
                nextMonthExpense,
                nextQuarterExpense,
                nextYearExpense,
                historicalSpendTrend,
                twelveMonthForecast,
                dataConfidence,
                LEGAL_DISCLAIMER
        );
    }

    // --- HISTORICAL START DATE RESOLUTION ---
    private LocalDate resolveHistoryStartDate(Vehicle vehicle, List<ServiceRecord> services,
                                               List<FuelRecord> fuels, List<MaintenanceRecord> maintenances) {
        if (vehicle.getCreatedAt() != null) {
            return vehicle.getCreatedAt().toLocalDate();
        }
        LocalDate earliest = null;
        for (FuelRecord f : fuels) {
            if (f.getFuelDate() != null && (earliest == null || f.getFuelDate().isBefore(earliest))) {
                earliest = f.getFuelDate();
            }
        }
        for (ServiceRecord s : services) {
            if (s.getServiceDate() != null && (earliest == null || s.getServiceDate().isBefore(earliest))) {
                earliest = s.getServiceDate();
            }
        }
        for (MaintenanceRecord m : maintenances) {
            LocalDate d = m.getCompletedDate() != null ? m.getCompletedDate() : m.getScheduledDate();
            if (d != null && (earliest == null || d.isBefore(earliest))) {
                earliest = d;
            }
        }
        return earliest != null ? earliest : LocalDate.now();
    }

    // --- EXPENSE CLASSIFICATION POLICY ---
    private boolean isCorrectiveRepair(String text) {
        if (text == null || text.isBlank()) return false;
        String lower = text.toLowerCase();
        return lower.contains("repair") || lower.contains("replace") || lower.contains("broken")
                || lower.contains("leak") || lower.contains("clutch") || lower.contains("radiator")
                || lower.contains("alternator") || lower.contains("starter") || lower.contains("transmission overhaul")
                || lower.contains("strut") || lower.contains("shock") || lower.contains("caliper")
                || lower.contains("rotor") || lower.contains("overhaul") || lower.contains("engine check")
                || lower.contains("diagnos") || lower.contains("noise") || lower.contains("vibration")
                || lower.contains("damage") || lower.contains("accident") || lower.contains("failure")
                || lower.contains("worn out") || lower.contains("blown") || lower.contains("sensor")
                || lower.contains("battery replacement");
    }

    // --- DETERMINISTIC HISTORICAL EVENT DEDUPLICATION ---
    private static class DeduplicatedHistoricalEvent {
        LocalDate eventDate;
        BigDecimal cost;
        boolean isCorrective;
        String subsystem;

        DeduplicatedHistoricalEvent(LocalDate eventDate, BigDecimal cost, boolean isCorrective, String subsystem) {
            this.eventDate = eventDate;
            this.cost = cost;
            this.isCorrective = isCorrective;
            this.subsystem = subsystem;
        }
    }

    private List<DeduplicatedHistoricalEvent> deduplicateHistoricalEvents(List<ServiceRecord> services, List<MaintenanceRecord> maintenances) {
        List<DeduplicatedHistoricalEvent> events = new ArrayList<>();
        Set<Long> mergedMaintenanceIds = new HashSet<>();

        // Match each service record against completed maintenance records
        for (ServiceRecord s : services) {
            LocalDate sDate = s.getServiceDate();
            BigDecimal sCost = s.getCost() != null ? s.getCost() : BigDecimal.ZERO;
            String sText = (s.getServiceType() != null ? s.getServiceType() : "") + " "
                    + (s.getDescription() != null ? s.getDescription() : "") + " "
                    + (s.getNotes() != null ? s.getNotes() : "");
            String sSubsystem = normalizeSubsystem(sText);
            boolean sCorrective = isCorrectiveRepair(sText);

            MaintenanceRecord matchedMaint = null;

            for (MaintenanceRecord m : maintenances) {
                if (m.isCompleted() && m.getCompletedDate() != null && !mergedMaintenanceIds.contains(m.getMaintenanceId())) {
                    long daysDiff = Math.abs(ChronoUnit.DAYS.between(m.getCompletedDate(), sDate));
                    if (daysDiff <= 2) {
                        String mText = (m.getTitle() != null ? m.getTitle() : "") + " "
                                + (m.getDescription() != null ? m.getDescription() : "") + " "
                                + (m.getNotes() != null ? m.getNotes() : "");
                        String mSubsystem = normalizeSubsystem(mText);

                        // MANDATORY: Same normalized subsystem
                        if (sSubsystem.equals(mSubsystem)) {
                            BigDecimal mCost = m.getCost() != null ? m.getCost() : BigDecimal.ZERO;
                            boolean sHasCost = sCost.compareTo(BigDecimal.ZERO) > 0;
                            boolean mHasCost = mCost.compareTo(BigDecimal.ZERO) > 0;

                            if (sHasCost && mHasCost) {
                                if (sCost.subtract(mCost).abs().compareTo(new BigDecimal("0.01")) < 0) {
                                    matchedMaint = m;
                                    break;
                                }
                            } else {
                                // Missing cost exception: one or both are zero/null
                                matchedMaint = m;
                                break;
                            }
                        }
                    }
                }
            }

            if (matchedMaint != null) {
                mergedMaintenanceIds.add(matchedMaint.getMaintenanceId());
                BigDecimal mCost = matchedMaint.getCost() != null ? matchedMaint.getCost() : BigDecimal.ZERO;
                BigDecimal eventCost = sCost.max(mCost);
                if (eventCost.compareTo(BigDecimal.ZERO) == 0) {
                    eventCost = getBenchmarkCostForSubsystem(sSubsystem);
                }
                String mText = (matchedMaint.getTitle() != null ? matchedMaint.getTitle() : "") + " "
                        + (matchedMaint.getDescription() != null ? matchedMaint.getDescription() : "");
                boolean isCorrective = sCorrective || isCorrectiveRepair(mText);

                events.add(new DeduplicatedHistoricalEvent(sDate, eventCost, isCorrective, sSubsystem));
            } else {
                BigDecimal eventCost = sCost;
                if (eventCost.compareTo(BigDecimal.ZERO) == 0) {
                    eventCost = getBenchmarkCostForSubsystem(sSubsystem);
                }
                events.add(new DeduplicatedHistoricalEvent(sDate, eventCost, sCorrective, sSubsystem));
            }
        }

        // Add remaining completed maintenance records that were not merged
        for (MaintenanceRecord m : maintenances) {
            if (m.isCompleted() && m.getCompletedDate() != null && !mergedMaintenanceIds.contains(m.getMaintenanceId())) {
                String mText = (m.getTitle() != null ? m.getTitle() : "") + " "
                        + (m.getDescription() != null ? m.getDescription() : "") + " "
                        + (m.getNotes() != null ? m.getNotes() : "");
                String mSubsystem = normalizeSubsystem(mText);
                boolean isCorrective = isCorrectiveRepair(mText);
                BigDecimal mCost = m.getCost() != null ? m.getCost() : BigDecimal.ZERO;
                if (mCost.compareTo(BigDecimal.ZERO) == 0) {
                    mCost = getBenchmarkCostForSubsystem(mSubsystem);
                }
                events.add(new DeduplicatedHistoricalEvent(m.getCompletedDate(), mCost, isCorrective, mSubsystem));
            }
        }

        return events;
    }

    // --- NORMALIZED SUBSYSTEM & OBLIGATION KEYS ---
    public static String normalizeSubsystem(String text) {
        if (text == null || text.isBlank()) return "GENERAL";
        String lower = text.toLowerCase();
        if (lower.contains("oil") || lower.contains("filter") || lower.contains("lube")) {
            return "ENGINE_OIL";
        }
        if (lower.contains("brake") || lower.contains("pad") || lower.contains("caliper") || lower.contains("rotor")) {
            return "BRAKING_SYSTEM";
        }
        if (lower.contains("coolan") || lower.contains("radiat") || lower.contains("antifreeze")) {
            return "COOLING_SYSTEM_FLUIDS";
        }
        if (lower.contains("tire") || lower.contains("tyre") || lower.contains("wheel")
                || lower.contains("alignment") || lower.contains("rotation") || lower.contains("suspension")
                || lower.contains("strut") || lower.contains("shock")) {
            return "TIRES_AND_SUSPENSION";
        }
        if (lower.contains("pms") || lower.contains("timing") || lower.contains("major") || lower.contains("tune up")) {
            return "MAJOR_PMS";
        }
        return "GENERAL";
    }

    public static BigDecimal getBenchmarkCostForSubsystem(String subsystem) {
        return switch (subsystem) {
            case "ENGINE_OIL" -> BENCHMARK_OIL_FILTER;
            case "BRAKING_SYSTEM" -> BENCHMARK_BRAKES;
            case "COOLING_SYSTEM_FLUIDS" -> BENCHMARK_COOLANT;
            case "TIRES_AND_SUSPENSION" -> BENCHMARK_TIRES;
            case "MAJOR_PMS" -> BENCHMARK_MAJOR_PMS;
            default -> BENCHMARK_GENERAL_FALLBACK;
        };
    }

    // --- DISTANCE INTENSITY & ODOMETER DEFENSE ---
    private static class DistanceIntensityResult {
        BigDecimal distanceIntensity;
        boolean available;
        String odometerStatus;

        DistanceIntensityResult(BigDecimal distanceIntensity, boolean available, String odometerStatus) {
            this.distanceIntensity = distanceIntensity;
            this.available = available;
            this.odometerStatus = odometerStatus;
        }
    }

    private DistanceIntensityResult computeDistanceIntensity(Vehicle vehicle, List<YearMonth> completedMonths,
                                                             BigDecimal totalSpend, List<ServiceRecord> services, List<FuelRecord> fuels) {
        if (completedMonths.isEmpty()) {
            return new DistanceIntensityResult(null, false, "NO_DATA");
        }

        LocalDate windowStart = completedMonths.get(0).atDay(1);
        LocalDate windowEnd = completedMonths.get(completedMonths.size() - 1).atEndOfMonth();

        Integer earliestOdo = null;
        Integer latestOdo = null;

        // Store dated odometer readings
        record DatedOdometer(LocalDate date, int odometer) {}
        List<DatedOdometer> readings = new ArrayList<>();

        for (FuelRecord f : fuels) {
            if (f.getOdometerAtFill() != null && !f.getFuelDate().isBefore(windowStart) && !f.getFuelDate().isAfter(windowEnd)) {
                readings.add(new DatedOdometer(f.getFuelDate(), f.getOdometerAtFill()));
            }
        }
        for (ServiceRecord s : services) {
            if (s.getOdometerAtService() != null && !s.getServiceDate().isBefore(windowStart) && !s.getServiceDate().isAfter(windowEnd)) {
                readings.add(new DatedOdometer(s.getServiceDate(), s.getOdometerAtService()));
            }
        }

        if (readings.isEmpty()) {
            if (vehicle.getCurrentOdometer() != null && vehicle.getCurrentOdometer() > 0) {
                latestOdo = vehicle.getCurrentOdometer();
                earliestOdo = latestOdo;
            } else {
                return new DistanceIntensityResult(null, false, "NO_DATA");
            }
        } else {
            readings.sort(Comparator.comparing(DatedOdometer::date));
            earliestOdo = readings.get(0).odometer();
            latestOdo = readings.get(readings.size() - 1).odometer();
        }

        int deltaOdo = latestOdo - earliestOdo;
        if (deltaOdo <= 0) {
            String status = deltaOdo == 0 ? "ZERO_DELTA" : "ROLLBACK_DETECTED";
            return new DistanceIntensityResult(null, false, status);
        }

        BigDecimal deltaKm = new BigDecimal(deltaOdo);
        BigDecimal distanceIntensity = totalSpend.divide(deltaKm, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100.0"))
                .setScale(2, RoundingMode.HALF_UP);

        return new DistanceIntensityResult(distanceIntensity, true, "NORMAL");
    }

    // --- EVRI CALCULATION ---
    private static class EvriResult {
        BigDecimal evri;
        String tier;

        EvriResult(BigDecimal evri, String tier) {
            this.evri = evri;
            this.tier = tier;
        }
    }

    private EvriResult computeEvri(List<BigDecimal> monthlySpends, BigDecimal meanMonthlySpend, int windowN) {
        if (meanMonthlySpend.compareTo(BigDecimal.ZERO) == 0 || windowN <= 1) {
            return new EvriResult(BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP), "STABLE");
        }

        double mean = meanMonthlySpend.doubleValue();
        double sumSquaredDiff = 0.0;
        for (BigDecimal s : monthlySpends) {
            double diff = s.doubleValue() - mean;
            sumSquaredDiff += (diff * diff);
        }

        double sampleVariance = sumSquaredDiff / (windowN - 1);
        double sampleStdDev = Math.sqrt(sampleVariance);

        double cv = sampleStdDev / Math.max(1.0, mean);
        double evriVal = Math.min(100.0, Math.max(0.0, cv * 50.0));

        BigDecimal evri = BigDecimal.valueOf(evriVal).setScale(1, RoundingMode.HALF_UP);
        String tier;
        if (evriVal < 25.0) {
            tier = "STABLE";
        } else if (evriVal < 50.0) {
            tier = "MODERATE";
        } else if (evriVal < 75.0) {
            tier = "ELEVATED";
        } else {
            tier = "VOLATILE";
        }

        return new EvriResult(evri, tier);
    }

    // --- EMERGENCY LIQUIDITY BUFFER ---
    private BigDecimal computeLiquidityBuffer(Vehicle vehicle, BigDecimal peakSingleSpend,
                                              BigDecimal rollingMonthlyBurnRate, BigDecimal evri) {
        int currentYear = LocalDate.now().getYear();
        int vehicleYear = vehicle.getYear() != null ? vehicle.getYear() : currentYear;
        int age = Math.max(0, currentYear - vehicleYear);

        double ageFactor = 1.0 + Math.min(0.50, (double) age / 20.0);
        double evriFactor = 1.0 + (evri.doubleValue() / 100.0);

        BigDecimal baselineNeed = peakSingleSpend.max(rollingMonthlyBurnRate.multiply(new BigDecimal("1.5")));

        double calculatedBuffer = baselineNeed.doubleValue() * evriFactor * ageFactor;
        BigDecimal finalBuffer = BigDecimal.valueOf(calculatedBuffer).max(LIQUIDITY_BUFFER_FLOOR);

        return finalBuffer.setScale(2, RoundingMode.HALF_UP);
    }

    // --- DATA CONFIDENCE METADATA ---
    private FiscalDataConfidenceDTO computeDataConfidence(List<FuelRecord> fuels, List<ServiceRecord> services,
                                                          List<MaintenanceRecord> maintenances, int windowN,
                                                          DistanceIntensityResult dist) {
        int fuelCount = fuels.size();
        int serviceCount = services.size();
        int maintCount = maintenances.size();
        int totalRecords = fuelCount + serviceCount + maintCount;

        String fuelTier;
        LocalDate ninetyDaysAgo = LocalDate.now().minusDays(90);
        long recentFuels = fuels.stream().filter(f -> !f.getFuelDate().isBefore(ninetyDaysAgo)).count();

        if (recentFuels >= 2) {
            fuelTier = "TRAILING_90_DAYS";
        } else if (fuelCount >= 1) {
            fuelTier = "LIFETIME_AVERAGE";
        } else {
            fuelTier = "CATEGORY_BENCHMARK";
        }

        String confidenceRating;
        if (totalRecords == 0) {
            confidenceRating = "BASELINE_ONLY";
        } else if (windowN >= 6 && fuelCount >= 3 && dist.available) {
            confidenceRating = "HIGH";
        } else if (windowN >= 3 && fuelCount >= 1) {
            confidenceRating = "MEDIUM";
        } else {
            confidenceRating = "LOW";
        }

        return new FiscalDataConfidenceDTO(
                confidenceRating,
                fuelCount,
                serviceCount,
                maintCount,
                windowN,
                fuelTier,
                dist.available,
                dist.odometerStatus
        );
    }

    // --- 12-MONTH FORWARD PREDICTIVE CASH FLOW FORECAST ---
    private List<MonthlyCashFlowPointDTO> computeTwelveMonthForecast(Vehicle vehicle, Long userId,
                                                                     List<FuelRecord> fuels,
                                                                     List<MaintenanceRecord> maintenances,
                                                                     String fuelTier) {
        List<MonthlyCashFlowPointDTO> forecast = new ArrayList<>();
        YearMonth currentYearMonth = YearMonth.from(LocalDate.now());

        // Baseline monthly fuel cost
        BigDecimal monthlyFuelExpense = resolveMonthlyFuelExpense(vehicle, fuels, fuelTier);

        // Fetch M14 milestones
        List<PredictiveMaintenanceMilestoneDTO> m14Milestones = Collections.emptyList();
        try {
            VehiclePredictiveReportDTO m14Report = predictiveMaintenanceService.getVehicleForecast(vehicle.getVehicleId(), userId);
            if (m14Report != null && m14Report.getMilestones() != null) {
                m14Milestones = m14Report.getMilestones();
            }
        } catch (Exception e) {
            log.warn("M14 forecast unavailable for vehicle {}: {}", vehicle.getVehicleId(), e.getMessage());
        }

        // Project next 12 forward months
        for (int i = 1; i <= 12; i++) {
            YearMonth targetYm = currentYearMonth.plusMonths(i);
            LocalDate monthStart = targetYm.atDay(1);
            LocalDate monthEnd = targetYm.atEndOfMonth();

            BigDecimal scheduledExpense = BigDecimal.ZERO;
            List<String> actionItems = new ArrayList<>();
            Map<String, String> activeScheduledKeys = new HashMap<>();

            // 1. Explicit active scheduled maintenance records
            for (MaintenanceRecord m : maintenances) {
                if (m.getStatus() != MaintenanceStatus.COMPLETED && m.getScheduledDate() != null) {
                    if (!m.getScheduledDate().isBefore(monthStart) && !m.getScheduledDate().isAfter(monthEnd)) {
                        BigDecimal cost = (m.getCost() != null && m.getCost().compareTo(BigDecimal.ZERO) > 0)
                                ? m.getCost() : getBenchmarkCostForSubsystem(normalizeSubsystem(m.getTitle()));
                        scheduledExpense = scheduledExpense.add(cost);
                        actionItems.add(m.getTitle() + " (Scheduled: $" + cost.setScale(2, RoundingMode.HALF_UP) + ")");

                        String subsystem = normalizeSubsystem(m.getTitle());
                        String scope = extractScopeCode(m.getTitle(), 0);
                        activeScheduledKeys.put(subsystem, scope);
                    }
                }
            }

            // 2. Projected consumable wear milestones from M14 (with Normalized Obligation Key deduplication)
            BigDecimal projectedWearExpense = BigDecimal.ZERO;
            for (PredictiveMaintenanceMilestoneDTO w : m14Milestones) {
                if (w.getEstimatedDueDate() != null && !w.getEstimatedDueDate().isBefore(monthStart) && !w.getEstimatedDueDate().isAfter(monthEnd)) {
                    String subsystem = normalizeSubsystem(w.getServiceName());
                    String scope = extractScopeCode(w.getServiceName(), w.getIntervalKm());

                    // Deduplication rule:
                    // Suppress if already scheduled or if an explicit task with same subsystem AND matching scope exists
                    boolean isSuppressed = w.isScheduled();
                    if (!isSuppressed && activeScheduledKeys.containsKey(subsystem)) {
                        String schedScope = activeScheduledKeys.get(subsystem);
                        if (schedScope.equals(scope) || schedScope.equals("GENERAL") || scope.equals("GENERAL")) {
                            isSuppressed = true;
                        }
                    }

                    if (!isSuppressed) {
                        BigDecimal wearCost = (w.getEstimatedCost() != null && w.getEstimatedCost().compareTo(BigDecimal.ZERO) > 0)
                                ? w.getEstimatedCost() : getBenchmarkCostForSubsystem(subsystem);
                        projectedWearExpense = projectedWearExpense.add(wearCost);
                        actionItems.add(w.getServiceName() + " (Projected Wear: $" + wearCost.setScale(2, RoundingMode.HALF_UP) + ")");
                    }
                }
            }

            BigDecimal totalMonthProjected = monthlyFuelExpense.add(scheduledExpense).add(projectedWearExpense);

            forecast.add(new MonthlyCashFlowPointDTO(
                    targetYm.format(MONTH_LABEL_FMT),
                    monthStart,
                    monthlyFuelExpense.setScale(2, RoundingMode.HALF_UP),
                    scheduledExpense.setScale(2, RoundingMode.HALF_UP),
                    projectedWearExpense.setScale(2, RoundingMode.HALF_UP),
                    totalMonthProjected.setScale(2, RoundingMode.HALF_UP),
                    actionItems
            ));
        }

        return forecast;
    }

    private String extractScopeCode(String text, int intervalKm) {
        if (intervalKm > 0) {
            return "INTERVAL_" + (intervalKm / 1000) + "K";
        }
        if (text == null) return "GENERAL";
        String lower = text.toLowerCase();
        if (lower.contains("fluid")) return "FLUID";
        if (lower.contains("pad")) return "PAD";
        if (lower.contains("rotor") || lower.contains("disc")) return "ROTOR";
        if (lower.contains("rotation")) return "ROTATION";
        if (lower.contains("replacement") || lower.contains("new")) return "REPLACEMENT";
        return "GENERAL";
    }

    private BigDecimal resolveMonthlyFuelExpense(Vehicle vehicle, List<FuelRecord> fuels, String fuelTier) {
        if ("TRAILING_90_DAYS".equals(fuelTier)) {
            LocalDate ninetyDaysAgo = LocalDate.now().minusDays(90);
            BigDecimal recentSum = fuels.stream()
                    .filter(f -> !f.getFuelDate().isBefore(ninetyDaysAgo))
                    .map(f -> f.getTotalCost() != null ? f.getTotalCost() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal dailyFuel = recentSum.divide(new BigDecimal("90.0"), 4, RoundingMode.HALF_UP);
            return dailyFuel.multiply(new BigDecimal("30.4375")).setScale(2, RoundingMode.HALF_UP);
        } else if ("LIFETIME_AVERAGE".equals(fuelTier)) {
            LocalDate earliest = fuels.get(fuels.size() - 1).getFuelDate();
            long days = Math.max(1, ChronoUnit.DAYS.between(earliest, LocalDate.now()));
            BigDecimal lifetimeSum = fuels.stream()
                    .map(f -> f.getTotalCost() != null ? f.getTotalCost() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal dailyFuel = lifetimeSum.divide(new BigDecimal(days), 4, RoundingMode.HALF_UP);
            return dailyFuel.multiply(new BigDecimal("30.4375")).setScale(2, RoundingMode.HALF_UP);
        } else {
            // Category Benchmark Fallback
            return getCategoryFuelBenchmark(vehicle);
        }
    }

    private BigDecimal getCategoryFuelBenchmark(Vehicle vehicle) {
        if (vehicle.getFuelType() != null && vehicle.getFuelType().equalsIgnoreCase("ELECTRIC")) {
            return FUEL_BENCHMARK_EV;
        }
        if (vehicle.getCategory() != null && vehicle.getCategory().getName() != null) {
            String cat = vehicle.getCategory().getName().toUpperCase();
            if (cat.contains("CAR") || cat.contains("SEDAN") || cat.contains("HATCHBACK")) {
                return FUEL_BENCHMARK_CAR;
            }
            if (cat.contains("SUV") || cat.contains("TRUCK")) {
                return FUEL_BENCHMARK_SUV_TRUCK;
            }
            if (cat.contains("BIKE") || cat.contains("MOTORCYCLE")) {
                return FUEL_BENCHMARK_BIKE;
            }
            if (cat.contains("SCOOTER")) {
                return FUEL_BENCHMARK_SCOOTER;
            }
        }
        return FUEL_BENCHMARK_DEFAULT;
    }

    private List<MonthlyCashFlowPointDTO> consolidateFleetForecast(List<VehicleFiscalBudgetReportDTO> vehicleReports) {
        if (vehicleReports.isEmpty()) return Collections.emptyList();

        List<MonthlyCashFlowPointDTO> consolidated = new ArrayList<>();
        int monthCount = vehicleReports.get(0).twelveMonthForecast().size();

        for (int m = 0; m < monthCount; m++) {
            BigDecimal totalFuel = BigDecimal.ZERO;
            BigDecimal totalSched = BigDecimal.ZERO;
            BigDecimal totalWear = BigDecimal.ZERO;
            List<String> combinedActions = new ArrayList<>();
            String label = "";
            LocalDate monthStart = LocalDate.now();

            for (VehicleFiscalBudgetReportDTO vr : vehicleReports) {
                if (m < vr.twelveMonthForecast().size()) {
                    MonthlyCashFlowPointDTO pt = vr.twelveMonthForecast().get(m);
                    label = pt.monthLabel();
                    monthStart = pt.monthStartDate();
                    totalFuel = totalFuel.add(pt.fuelExpense());
                    totalSched = totalSched.add(pt.scheduledMaintenanceExpense());
                    totalWear = totalWear.add(pt.projectedWearExpense());
                    for (String act : pt.plannedActionItems()) {
                        combinedActions.add(vr.vehicleName() + ": " + act);
                    }
                }
            }

            BigDecimal total = totalFuel.add(totalSched).add(totalWear);
            consolidated.add(new MonthlyCashFlowPointDTO(
                    label,
                    monthStart,
                    totalFuel.setScale(2, RoundingMode.HALF_UP),
                    totalSched.setScale(2, RoundingMode.HALF_UP),
                    totalWear.setScale(2, RoundingMode.HALF_UP),
                    total.setScale(2, RoundingMode.HALF_UP),
                    combinedActions
            ));
        }

        return consolidated;
    }

    private List<MonthlyCashFlowPointDTO> generateEmptyConsolidatedForecast() {
        List<MonthlyCashFlowPointDTO> forecast = new ArrayList<>();
        YearMonth currentYearMonth = YearMonth.from(LocalDate.now());
        for (int i = 1; i <= 12; i++) {
            YearMonth targetYm = currentYearMonth.plusMonths(i);
            forecast.add(new MonthlyCashFlowPointDTO(
                    targetYm.format(MONTH_LABEL_FMT),
                    targetYm.atDay(1),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    Collections.emptyList()
            ));
        }
        return forecast;
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AccessDeniedException("User not found: " + email));
    }

    private void enforceNonAdmin(User user) {
        if (user.getRole() == com.mygarage.model.enums.Role.ADMIN) {
            throw new AccessDeniedException("Admin users are isolated from private user vehicle fiscal portfolios");
        }
    }
}
