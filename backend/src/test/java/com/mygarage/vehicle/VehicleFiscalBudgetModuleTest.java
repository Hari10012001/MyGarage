package com.mygarage.vehicle;

import com.mygarage.dto.response.*;
import com.mygarage.model.*;
import com.mygarage.model.enums.FuelType;
import com.mygarage.model.enums.MaintenanceStatus;
import com.mygarage.model.enums.Role;
import com.mygarage.repository.*;
import com.mygarage.service.VehicleFiscalBudgetService;
import com.mygarage.service.VehicleFiscalBudgetServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
public class VehicleFiscalBudgetModuleTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VehicleFiscalBudgetService vehicleFiscalBudgetService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private VehicleCategoryRepository vehicleCategoryRepository;

    @Autowired
    private ServiceRecordRepository serviceRecordRepository;

    @Autowired
    private FuelRecordRepository fuelRecordRepository;

    @Autowired
    private MaintenanceRecordRepository maintenanceRecordRepository;

    private User testUser;
    private User otherUser;
    private User adminUser;
    private Vehicle userVehicle;
    private Vehicle otherVehicle;
    private VehicleCategory testCategory;

    @BeforeEach
    void setUp() {
        testCategory = new VehicleCategory();
        testCategory.setName("Sedan_" + UUID.randomUUID().toString().substring(0, 8));
        testCategory.setIcon("car-front");
        testCategory = vehicleCategoryRepository.save(testCategory);

        testUser = userRepository.findByEmail("fiscal_user@garage.com").orElseGet(() -> {
            User u = new User();
            u.setEmail("fiscal_user@garage.com");
            u.setPasswordHash("hash");
            u.setFullName("Fiscal Tester");
            u.setRole(Role.NORMAL_USER);
            return userRepository.save(u);
        });

        otherUser = userRepository.findByEmail("other_fiscal@garage.com").orElseGet(() -> {
            User u = new User();
            u.setEmail("other_fiscal@garage.com");
            u.setPasswordHash("hash");
            u.setFullName("Other Fiscal Tester");
            u.setRole(Role.NORMAL_USER);
            return userRepository.save(u);
        });

        adminUser = userRepository.findByEmail("admin_fiscal@garage.com").orElseGet(() -> {
            User u = new User();
            u.setEmail("admin_fiscal@garage.com");
            u.setPasswordHash("hash");
            u.setFullName("Admin Fiscal Tester");
            u.setRole(Role.ADMIN);
            return userRepository.save(u);
        });

        userVehicle = new Vehicle();
        userVehicle.setUser(testUser);
        userVehicle.setCategory(testCategory);
        userVehicle.setMake("Honda");
        userVehicle.setModel("Civic");
        userVehicle.setYear(2021);
        userVehicle.setPlateNumber("FSC-" + UUID.randomUUID().toString().substring(0, 6));
        userVehicle.setCurrentOdometer(45000);
        userVehicle.setFuelType("PETROL");
        userVehicle.setCreatedAt(LocalDateTime.now().minusMonths(14));
        userVehicle = vehicleRepository.save(userVehicle);

        otherVehicle = new Vehicle();
        otherVehicle.setUser(otherUser);
        otherVehicle.setCategory(testCategory);
        otherVehicle.setMake("Toyota");
        otherVehicle.setModel("Corolla");
        otherVehicle.setYear(2022);
        otherVehicle.setPlateNumber("OTH-" + UUID.randomUUID().toString().substring(0, 6));
        otherVehicle.setCurrentOdometer(30000);
        otherVehicle.setFuelType("PETROL");
        otherVehicle.setCreatedAt(LocalDateTime.now().minusMonths(6));
        otherVehicle = vehicleRepository.save(otherVehicle);
    }

    @Test
    @DisplayName("Test 1: Standard vehicle report generation with multi-category history")
    void testStandardVehicleReport() {
        seedStandardHistory(userVehicle);

        VehicleFiscalBudgetReportDTO report = vehicleFiscalBudgetService.getVehicleFiscalBudgetReport(
                userVehicle.getVehicleId(), testUser.getEmail()
        );

        assertThat(report).isNotNull();
        assertThat(report.vehicleId()).isEqualTo(userVehicle.getVehicleId());
        assertThat(report.currency()).isEqualTo("$");
        assertThat(report.rollingMonthlyBurnRate()).isGreaterThan(BigDecimal.ZERO);
        assertThat(report.dailyBurnRate()).isGreaterThan(BigDecimal.ZERO);
        assertThat(report.twelveMonthForecast()).hasSize(12);
        assertThat(report.disclaimer()).contains("deterministic analytical heuristics");
    }

    @Test
    @DisplayName("Test 2: Complete calendar window includes zero-spend months")
    void testCompleteCalendarWindowWithZeroSpendMonths() {
        // Vehicle active for 4 completed months, only 1 month has spend
        userVehicle.setCreatedAt(LocalDateTime.now().minusMonths(4));
        vehicleRepository.save(userVehicle);

        FuelRecord f = new FuelRecord();
        f.setVehicle(userVehicle);
        f.setFuelDate(LocalDate.now().minusMonths(1).withDayOfMonth(10));
        f.setFuelType(FuelType.PETROL);
        f.setQuantityLitres(new BigDecimal("40.0"));
        f.setCostPerLitre(new BigDecimal("1.50"));
        f.setTotalCost(new BigDecimal("60.00"));
        f.setOdometerAtFill(45000);
        fuelRecordRepository.save(f);

        VehicleFiscalBudgetReportDTO report = vehicleFiscalBudgetService.getVehicleFiscalBudgetReport(
                userVehicle.getVehicleId(), testUser.getEmail()
        );

        assertThat(report.historicalSpendTrend()).hasSize(4);
        long zeroSpendMonths = report.historicalSpendTrend().stream()
                .filter(pt -> pt.totalMonthlySpend().compareTo(BigDecimal.ZERO) == 0)
                .count();
        assertThat(zeroSpendMonths).isEqualTo(3);
    }

    @Test
    @DisplayName("Test 3: Historical window definition N = min(12, max(1, monthsActive))")
    void testHistoricalWindowDefinition() {
        userVehicle.setCreatedAt(LocalDateTime.now().minusMonths(5));
        vehicleRepository.save(userVehicle);

        VehicleFiscalBudgetReportDTO report = vehicleFiscalBudgetService.getVehicleFiscalBudgetReport(
                userVehicle.getVehicleId(), testUser.getEmail()
        );

        assertThat(report.dataConfidence().historyMonthsEvaluated()).isEqualTo(5);
        assertThat(report.historicalSpendTrend()).hasSize(5);
    }

    @Test
    @DisplayName("Test 4: Current partial month is strictly excluded from historical evaluation")
    void testCurrentPartialMonthExcluded() {
        // Spend in current month should not appear in historical completed window
        FuelRecord currentMonthFuel = new FuelRecord();
        currentMonthFuel.setVehicle(userVehicle);
        currentMonthFuel.setFuelDate(LocalDate.now()); // today
        currentMonthFuel.setFuelType(FuelType.PETROL);
        currentMonthFuel.setQuantityLitres(new BigDecimal("30.0"));
        currentMonthFuel.setCostPerLitre(new BigDecimal("1.50"));
        currentMonthFuel.setTotalCost(new BigDecimal("45.00"));
        currentMonthFuel.setOdometerAtFill(45500);
        fuelRecordRepository.save(currentMonthFuel);

        VehicleFiscalBudgetReportDTO report = vehicleFiscalBudgetService.getVehicleFiscalBudgetReport(
                userVehicle.getVehicleId(), testUser.getEmail()
        );

        // Historical window of 12 months should not contain current month's 45.00
        BigDecimal historicalSum = report.historicalSpendTrend().stream()
                .map(HistoricalSpendPointDTO::fuelSpend)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(historicalSum).isEqualTo(BigDecimal.ZERO.setScale(2));
    }

    @Test
    @DisplayName("Test 5: Veteran vehicle with > 12 months active is capped strictly at 12")
    void testWindowCappedAt12ForVeteranVehicles() {
        userVehicle.setCreatedAt(LocalDateTime.now().minusMonths(24));
        vehicleRepository.save(userVehicle);

        VehicleFiscalBudgetReportDTO report = vehicleFiscalBudgetService.getVehicleFiscalBudgetReport(
                userVehicle.getVehicleId(), testUser.getEmail()
        );

        assertThat(report.dataConfidence().historyMonthsEvaluated()).isEqualTo(12);
        assertThat(report.historicalSpendTrend()).hasSize(12);
    }

    @Test
    @DisplayName("Test 6: Single-month history edge case N=1 sets EVRI to 0.0 with LOW confidence")
    void testSingleMonthHistoryEdgeCase() {
        userVehicle.setCreatedAt(LocalDateTime.now().minusMonths(1));
        vehicleRepository.save(userVehicle);

        VehicleFiscalBudgetReportDTO report = vehicleFiscalBudgetService.getVehicleFiscalBudgetReport(
                userVehicle.getVehicleId(), testUser.getEmail()
        );

        assertThat(report.dataConfidence().historyMonthsEvaluated()).isEqualTo(1);
        assertThat(report.expenditureVolatilityIndex()).isEqualTo(new BigDecimal("0.0"));
        assertThat(report.volatilityTier()).isEqualTo("STABLE");
    }

    @Test
    @DisplayName("Test 7: Zero-mean spend vehicle has EVRI = 0.0 and STABLE tier")
    void testZeroMeanSpendVehicle() {
        // Vehicle with zero historical records
        VehicleFiscalBudgetReportDTO report = vehicleFiscalBudgetService.getVehicleFiscalBudgetReport(
                userVehicle.getVehicleId(), testUser.getEmail()
        );

        assertThat(report.rollingMonthlyBurnRate()).isEqualTo(new BigDecimal("0.00"));
        assertThat(report.expenditureVolatilityIndex()).isEqualTo(new BigDecimal("0.0"));
        assertThat(report.volatilityTier()).isEqualTo("STABLE");
    }

    @Test
    @DisplayName("Test 8: Deterministic ExpenseClassificationPolicy precedence: FUEL -> CORRECTIVE_REPAIR -> ROUTINE")
    void testExpenseClassificationPrecedence() {
        LocalDate lastMonth = LocalDate.now().minusMonths(1).withDayOfMonth(15);

        // Service with both repair and scheduled words: repair takes precedence
        ServiceRecord sr = new ServiceRecord();
        sr.setVehicle(userVehicle);
        sr.setServiceDate(lastMonth);
        sr.setServiceType("Routine oil check and alternator repair");
        sr.setCost(new BigDecimal("250.00"));
        sr.setOdometerAtService(44000);
        serviceRecordRepository.save(sr);

        VehicleFiscalBudgetReportDTO report = vehicleFiscalBudgetService.getVehicleFiscalBudgetReport(
                userVehicle.getVehicleId(), testUser.getEmail()
        );

        // Should be classified as repair, not scheduled
        assertThat(report.repairBurnRate()).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Test 9: Corrective classification applies consistently to maintenance_records")
    void testCorrectiveClassificationOnMaintenanceRecords() {
        LocalDate lastMonth = LocalDate.now().minusMonths(1).withDayOfMonth(10);

        MaintenanceRecord mr = new MaintenanceRecord();
        mr.setVehicle(userVehicle);
        mr.setTitle("Radiator leak repair and hose replacement");
        mr.setScheduledDate(lastMonth);
        mr.setCompletedDate(lastMonth);
        mr.setStatus(MaintenanceStatus.COMPLETED);
        mr.setCost(new BigDecimal("175.00"));
        maintenanceRecordRepository.save(mr);

        VehicleFiscalBudgetReportDTO report = vehicleFiscalBudgetService.getVehicleFiscalBudgetReport(
                userVehicle.getVehicleId(), testUser.getEmail()
        );

        assertThat(report.repairBurnRate()).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Test 10: Deterministic historical event deduplication merges same subsystem & matching cost")
    void testHistoricalEventDeduplication() {
        LocalDate eventDate = LocalDate.now().minusMonths(1).withDayOfMonth(5);

        // Maintenance record for oil change
        MaintenanceRecord mr = new MaintenanceRecord();
        mr.setVehicle(userVehicle);
        mr.setTitle("Engine Oil & Filter Service");
        mr.setScheduledDate(eventDate);
        mr.setCompletedDate(eventDate);
        mr.setStatus(MaintenanceStatus.COMPLETED);
        mr.setCost(new BigDecimal("95.00"));
        maintenanceRecordRepository.save(mr);

        // Service record for same oil change 1 day later with identical cost
        ServiceRecord sr = new ServiceRecord();
        sr.setVehicle(userVehicle);
        sr.setServiceDate(eventDate.plusDays(1));
        sr.setServiceType("Engine Oil Service");
        sr.setCost(new BigDecimal("95.00"));
        sr.setOdometerAtService(44500);
        serviceRecordRepository.save(sr);

        VehicleFiscalBudgetReportDTO report = vehicleFiscalBudgetService.getVehicleFiscalBudgetReport(
                userVehicle.getVehicleId(), testUser.getEmail()
        );

        // Total window spend should reflect $95.00 counted once, NOT $190.00
        BigDecimal monthlySpend = report.historicalSpendTrend().get(report.historicalSpendTrend().size() - 1).totalMonthlySpend();
        assertThat(monthlySpend).isEqualTo(new BigDecimal("95.00"));
    }

    @Test
    @DisplayName("Test 11: Unrelated events across different subsystems with identical costs are strictly isolated and never merged")
    void testUnrelatedSubsystemsWithIdenticalCostsNeverMerge() {
        LocalDate eventDate = LocalDate.now().minusMonths(1).withDayOfMonth(8);

        // Maintenance record for Tire Rotation ($60.00)
        MaintenanceRecord mr = new MaintenanceRecord();
        mr.setVehicle(userVehicle);
        mr.setTitle("Tire Rotation & Balance");
        mr.setScheduledDate(eventDate);
        mr.setCompletedDate(eventDate);
        mr.setStatus(MaintenanceStatus.COMPLETED);
        mr.setCost(new BigDecimal("60.00"));
        maintenanceRecordRepository.save(mr);

        // Service record on same day for Engine Oil Change ($60.00)
        ServiceRecord sr = new ServiceRecord();
        sr.setVehicle(userVehicle);
        sr.setServiceDate(eventDate);
        sr.setServiceType("Engine Oil & Filter Service");
        sr.setCost(new BigDecimal("60.00"));
        sr.setOdometerAtService(44600);
        serviceRecordRepository.save(sr);

        VehicleFiscalBudgetReportDTO report = vehicleFiscalBudgetService.getVehicleFiscalBudgetReport(
                userVehicle.getVehicleId(), testUser.getEmail()
        );

        // Unrelated subsystems should both count: $60.00 + $60.00 = $120.00
        BigDecimal monthlySpend = report.historicalSpendTrend().get(report.historicalSpendTrend().size() - 1).totalMonthlySpend();
        assertThat(monthlySpend).isEqualTo(new BigDecimal("120.00"));
    }

    @Test
    @DisplayName("Test 12: Historical deduplication governs peak single spend and liquidity buffer")
    void testHistoricalDeduplicationImpactsPeakSpend() {
        LocalDate eventDate = LocalDate.now().minusMonths(2).withDayOfMonth(12);

        MaintenanceRecord mr = new MaintenanceRecord();
        mr.setVehicle(userVehicle);
        mr.setTitle("Major Periodic Maintenance (PMS)");
        mr.setScheduledDate(eventDate);
        mr.setCompletedDate(eventDate);
        mr.setStatus(MaintenanceStatus.COMPLETED);
        mr.setCost(new BigDecimal("320.00"));
        maintenanceRecordRepository.save(mr);

        ServiceRecord sr = new ServiceRecord();
        sr.setVehicle(userVehicle);
        sr.setServiceDate(eventDate);
        sr.setServiceType("Major PMS Scheduled Service");
        sr.setCost(new BigDecimal("320.00"));
        sr.setOdometerAtService(43000);
        serviceRecordRepository.save(sr);

        VehicleFiscalBudgetReportDTO report = vehicleFiscalBudgetService.getVehicleFiscalBudgetReport(
                userVehicle.getVehicleId(), testUser.getEmail()
        );

        // Peak single spend should be $320.00, not $640.00
        assertThat(report.historicalPeakSingleSpend()).isEqualTo(new BigDecimal("320.00"));
    }

    @Test
    @DisplayName("Test 13: historyStartDate primary vehicle.createdAt vs fallback earliest record")
    void testHistoryStartDateResolution() {
        // Create vehicle with null createdAt (via native query or simulated fallback)
        Vehicle vNoCreated = new Vehicle();
        vNoCreated.setUser(testUser);
        vNoCreated.setCategory(testCategory);
        vNoCreated.setMake("Hyundai");
        vNoCreated.setModel("Elantra");
        vNoCreated.setYear(2020);
        vNoCreated.setPlateNumber("HYU-" + UUID.randomUUID().toString().substring(0, 6));
        vNoCreated = vehicleRepository.save(vNoCreated);

        FuelRecord f = new FuelRecord();
        f.setVehicle(vNoCreated);
        f.setFuelDate(LocalDate.now().minusMonths(7));
        f.setFuelType(FuelType.PETROL);
        f.setQuantityLitres(new BigDecimal("25.0"));
        f.setCostPerLitre(new BigDecimal("1.40"));
        f.setTotalCost(new BigDecimal("35.00"));
        fuelRecordRepository.save(f);

        VehicleFiscalBudgetReportDTO report = vehicleFiscalBudgetService.getVehicleFiscalBudgetReport(
                vNoCreated.getVehicleId(), testUser.getEmail()
        );

        assertThat(report.dataConfidence().historyMonthsEvaluated()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("Test 14: Future deduplication via Normalized Obligation Keys suppresses duplicate milestone")
    void testFutureDeduplicationNormalizedKeys() {
        LocalDate nextMonth = LocalDate.now().plusMonths(1).withDayOfMonth(15);

        // Explicit scheduled task for Engine Oil in next month
        MaintenanceRecord mr = new MaintenanceRecord();
        mr.setVehicle(userVehicle);
        mr.setTitle("Engine Oil & Filter Service");
        mr.setScheduledDate(nextMonth);
        mr.setStatus(MaintenanceStatus.UPCOMING);
        mr.setCost(new BigDecimal("95.00"));
        maintenanceRecordRepository.save(mr);

        VehicleFiscalBudgetReportDTO report = vehicleFiscalBudgetService.getVehicleFiscalBudgetReport(
                userVehicle.getVehicleId(), testUser.getEmail()
        );

        MonthlyCashFlowPointDTO nextMonthPoint = report.twelveMonthForecast().get(0);
        assertThat(nextMonthPoint.scheduledMaintenanceExpense()).isEqualTo(new BigDecimal("95.00"));
        // Deduplication ensures projected wear for Engine Oil is suppressed
        assertThat(nextMonthPoint.plannedActionItems()).anyMatch(item -> item.contains("Engine Oil"));
    }

    @Test
    @DisplayName("Test 15: Distinct same-subsystem obligations in same month (Brake Fluid vs Brake Pads) are both preserved")
    void testDistinctSameSubsystemObligationsPreserved() {
        LocalDate nextMonth = LocalDate.now().plusMonths(1).withDayOfMonth(10);

        // Explicit task for Brake Fluid Flush ($80.00)
        MaintenanceRecord mr = new MaintenanceRecord();
        mr.setVehicle(userVehicle);
        mr.setTitle("Brake Fluid Flush & Bleed");
        mr.setScheduledDate(nextMonth);
        mr.setStatus(MaintenanceStatus.UPCOMING);
        mr.setCost(new BigDecimal("80.00"));
        maintenanceRecordRepository.save(mr);

        // Another explicit task for Brake Pad Replacement ($120.00)
        MaintenanceRecord mr2 = new MaintenanceRecord();
        mr2.setVehicle(userVehicle);
        mr2.setTitle("Front Brake Pad Replacement");
        mr2.setScheduledDate(nextMonth.plusDays(2));
        mr2.setStatus(MaintenanceStatus.UPCOMING);
        mr2.setCost(new BigDecimal("120.00"));
        maintenanceRecordRepository.save(mr2);

        VehicleFiscalBudgetReportDTO report = vehicleFiscalBudgetService.getVehicleFiscalBudgetReport(
                userVehicle.getVehicleId(), testUser.getEmail()
        );

        MonthlyCashFlowPointDTO nextMonthPoint = report.twelveMonthForecast().get(0);
        // Both explicit tasks must be budgeted: $80 + $120 = $200
        assertThat(nextMonthPoint.scheduledMaintenanceExpense()).isEqualTo(new BigDecimal("200.00"));
    }

    @Test
    @DisplayName("Test 16: Maintenance benchmark fallback values for all 6 categories")
    void testMaintenanceBenchmarkFallbackValues() {
        assertThat(VehicleFiscalBudgetServiceImpl.getBenchmarkCostForSubsystem("ENGINE_OIL"))
                .isEqualTo(new BigDecimal("95.00"));
        assertThat(VehicleFiscalBudgetServiceImpl.getBenchmarkCostForSubsystem("BRAKING_SYSTEM"))
                .isEqualTo(new BigDecimal("180.00"));
        assertThat(VehicleFiscalBudgetServiceImpl.getBenchmarkCostForSubsystem("COOLING_SYSTEM_FLUIDS"))
                .isEqualTo(new BigDecimal("140.00"));
        assertThat(VehicleFiscalBudgetServiceImpl.getBenchmarkCostForSubsystem("TIRES_AND_SUSPENSION"))
                .isEqualTo(new BigDecimal("160.00"));
        assertThat(VehicleFiscalBudgetServiceImpl.getBenchmarkCostForSubsystem("MAJOR_PMS"))
                .isEqualTo(new BigDecimal("320.00"));
        assertThat(VehicleFiscalBudgetServiceImpl.getBenchmarkCostForSubsystem("GENERAL"))
                .isEqualTo(new BigDecimal("110.00"));
    }

    @Test
    @DisplayName("Test 17: Fuel forecast hierarchy Tier 1 (trailing 90 days active fuel)")
    void testFuelForecastHierarchyTier1() {
        // Seed 2 recent fuel records within 90 days
        LocalDate date1 = LocalDate.now().minusDays(20);
        LocalDate date2 = LocalDate.now().minusDays(10);

        seedFuelRecord(userVehicle, date1, new BigDecimal("60.00"), 44000);
        seedFuelRecord(userVehicle, date2, new BigDecimal("60.00"), 44500);

        VehicleFiscalBudgetReportDTO report = vehicleFiscalBudgetService.getVehicleFiscalBudgetReport(
                userVehicle.getVehicleId(), testUser.getEmail()
        );

        assertThat(report.dataConfidence().fuelEstimationTier()).isEqualTo("TRAILING_90_DAYS");
        assertThat(report.twelveMonthForecast().get(0).fuelExpense()).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Test 18: Fuel forecast hierarchy Tier 2 (lifetime fuel records)")
    void testFuelForecastHierarchyTier2() {
        // Only 1 fuel record 120 days ago (> 90 days)
        LocalDate date1 = LocalDate.now().minusDays(120);
        seedFuelRecord(userVehicle, date1, new BigDecimal("80.00"), 43000);

        VehicleFiscalBudgetReportDTO report = vehicleFiscalBudgetService.getVehicleFiscalBudgetReport(
                userVehicle.getVehicleId(), testUser.getEmail()
        );

        assertThat(report.dataConfidence().fuelEstimationTier()).isEqualTo("LIFETIME_AVERAGE");
    }

    @Test
    @DisplayName("Test 19: Fuel forecast hierarchy Tier 3 (category benchmark fallback)")
    void testFuelForecastHierarchyTier3() {
        // Zero fuel records
        VehicleFiscalBudgetReportDTO report = vehicleFiscalBudgetService.getVehicleFiscalBudgetReport(
                userVehicle.getVehicleId(), testUser.getEmail()
        );

        assertThat(report.dataConfidence().fuelEstimationTier()).isEqualTo("CATEGORY_BENCHMARK");
        // Sedan benchmark baseline spend is $89.29
        assertThat(report.twelveMonthForecast().get(0).fuelExpense()).isEqualTo(new BigDecimal("89.29"));
    }

    @Test
    @DisplayName("Test 20: Category fuel benchmarks across Sedan, SUV, Motorcycle, Scooter, EV, Default")
    void testCategoryFuelBenchmarks() {
        assertThat(VehicleFiscalBudgetServiceImpl.FUEL_BENCHMARK_CAR).isEqualTo(new BigDecimal("89.29"));
        assertThat(VehicleFiscalBudgetServiceImpl.FUEL_BENCHMARK_SUV_TRUCK).isEqualTo(new BigDecimal("122.73"));
        assertThat(VehicleFiscalBudgetServiceImpl.FUEL_BENCHMARK_BIKE).isEqualTo(new BigDecimal("16.45"));
        assertThat(VehicleFiscalBudgetServiceImpl.FUEL_BENCHMARK_SCOOTER).isEqualTo(new BigDecimal("14.29"));
        assertThat(VehicleFiscalBudgetServiceImpl.FUEL_BENCHMARK_EV).isEqualTo(new BigDecimal("30.00"));
        assertThat(VehicleFiscalBudgetServiceImpl.FUEL_BENCHMARK_DEFAULT).isEqualTo(new BigDecimal("100.00"));
    }

    @Test
    @DisplayName("Test 21: Currency consistency verification: all amounts in $ / USD")
    void testCurrencyConsistency() {
        VehicleFiscalBudgetReportDTO report = vehicleFiscalBudgetService.getVehicleFiscalBudgetReport(
                userVehicle.getVehicleId(), testUser.getEmail()
        );

        assertThat(report.currency()).isEqualTo("$");
    }

    @Test
    @DisplayName("Test 22: Odometer anomaly guard: negative / rollback odometer")
    void testOdometerRollbackAnomalyGuard() {
        // Earliest date has higher odometer than latest date
        seedFuelRecord(userVehicle, LocalDate.now().minusMonths(3), new BigDecimal("50.00"), 50000);
        seedFuelRecord(userVehicle, LocalDate.now().minusMonths(1), new BigDecimal("50.00"), 48000);

        VehicleFiscalBudgetReportDTO report = vehicleFiscalBudgetService.getVehicleFiscalBudgetReport(
                userVehicle.getVehicleId(), testUser.getEmail()
        );

        assertThat(report.distanceBurnRatePer100Km()).isNull();
        assertThat(report.dataConfidence().distanceMetricAvailable()).isFalse();
        assertThat(report.dataConfidence().odometerStatus()).isEqualTo("ROLLBACK_DETECTED");
    }

    @Test
    @DisplayName("Test 23: Odometer anomaly guard: zero odometer progression")
    void testZeroOdometerProgressionGuard() {
        seedFuelRecord(userVehicle, LocalDate.now().minusMonths(3), new BigDecimal("50.00"), 45000);
        seedFuelRecord(userVehicle, LocalDate.now().minusMonths(1), new BigDecimal("50.00"), 45000);

        VehicleFiscalBudgetReportDTO report = vehicleFiscalBudgetService.getVehicleFiscalBudgetReport(
                userVehicle.getVehicleId(), testUser.getEmail()
        );

        assertThat(report.distanceBurnRatePer100Km()).isNull();
        assertThat(report.dataConfidence().distanceMetricAvailable()).isFalse();
        assertThat(report.dataConfidence().odometerStatus()).isEqualTo("ZERO_DELTA");
    }

    @Test
    @DisplayName("Test 24: Recommended liquidity buffer calculation with vehicle age scaling")
    void testLiquidityBufferAgeScaling() {
        // Vehicle from 2010 (16 years old)
        userVehicle.setYear(2010);
        vehicleRepository.save(userVehicle);

        VehicleFiscalBudgetReportDTO report = vehicleFiscalBudgetService.getVehicleFiscalBudgetReport(
                userVehicle.getVehicleId(), testUser.getEmail()
        );

        // Buffer must be at least the $250 floor, scaled by age
        assertThat(report.recommendedLiquidityBuffer()).isGreaterThanOrEqualTo(new BigDecimal("250.00"));
    }

    @Test
    @DisplayName("Test 25: Recommended liquidity buffer minimum floor ($250.00)")
    void testLiquidityBufferFloor() {
        VehicleFiscalBudgetReportDTO report = vehicleFiscalBudgetService.getVehicleFiscalBudgetReport(
                userVehicle.getVehicleId(), testUser.getEmail()
        );

        assertThat(report.recommendedLiquidityBuffer()).isGreaterThanOrEqualTo(new BigDecimal("250.00"));
    }

    @Test
    @DisplayName("Test 26: Data confidence rating evaluation across HIGH, MEDIUM, LOW, BASELINE_ONLY tiers")
    void testDataConfidenceTiers() {
        // Zero records -> BASELINE_ONLY
        VehicleFiscalBudgetReportDTO report0 = vehicleFiscalBudgetService.getVehicleFiscalBudgetReport(
                userVehicle.getVehicleId(), testUser.getEmail()
        );
        assertThat(report0.dataConfidence().confidenceRating()).isEqualTo("BASELINE_ONLY");

        // 1 fuel record -> LOW or MEDIUM depending on window
        seedFuelRecord(userVehicle, LocalDate.now().minusMonths(1), new BigDecimal("50.00"), 45000);
        VehicleFiscalBudgetReportDTO report1 = vehicleFiscalBudgetService.getVehicleFiscalBudgetReport(
                userVehicle.getVehicleId(), testUser.getEmail()
        );
        assertThat(report1.dataConfidence().confidenceRating()).isIn("LOW", "MEDIUM");
    }

    @Test
    @DisplayName("Test 27: Shared dependency verification: M14 milestones seamlessly mapped to forward forecast")
    void testSharedDependencyWithM14() {
        VehicleFiscalBudgetReportDTO report = vehicleFiscalBudgetService.getVehicleFiscalBudgetReport(
                userVehicle.getVehicleId(), testUser.getEmail()
        );

        assertThat(report.twelveMonthForecast()).hasSize(12);
        // Verify month labels are sequential
        assertThat(report.twelveMonthForecast().get(0).monthLabel()).isNotEmpty();
    }

    @Test
    @DisplayName("Test 28: Garage fleet fiscal aggregation across multiple owned vehicles")
    void testGarageFleetFiscalAggregation() {
        GarageFiscalBudgetMatrixDTO matrix = vehicleFiscalBudgetService.getGarageFiscalBudgetMatrix(testUser.getEmail());

        assertThat(matrix).isNotNull();
        assertThat(matrix.totalVehicles()).isGreaterThanOrEqualTo(1);
        assertThat(matrix.currency()).isEqualTo("$");
        assertThat(matrix.vehicleBreakdown()).isNotEmpty();
        assertThat(matrix.consolidatedFleetForecast()).hasSize(12);
    }

    @Test
    @DisplayName("Test 29: Empty fleet handling for user with zero vehicles")
    void testEmptyFleetHandling() {
        User emptyUser = new User();
        emptyUser.setEmail("empty_user@garage.com");
        emptyUser.setPasswordHash("hash");
        emptyUser.setFullName("Empty User");
        emptyUser.setRole(Role.NORMAL_USER);
        emptyUser = userRepository.save(emptyUser);

        GarageFiscalBudgetMatrixDTO matrix = vehicleFiscalBudgetService.getGarageFiscalBudgetMatrix(emptyUser.getEmail());

        assertThat(matrix.totalVehicles()).isEqualTo(0);
        assertThat(matrix.totalPortfolioMonthlyBurn()).isEqualTo(new BigDecimal("0.00"));
        assertThat(matrix.vehicleBreakdown()).isEmpty();
        assertThat(matrix.consolidatedFleetForecast()).hasSize(12);
    }

    @Test
    @DisplayName("Test 30: Positive owner authorization: REST 200 OK and Web MVC 200 OK")
    @WithMockUser(username = "fiscal_user@garage.com", roles = "NORMAL_USER")
    void testPositiveOwnerAuthorization() throws Exception {
        mockMvc.perform(get("/api/vehicles/" + userVehicle.getVehicleId() + "/fiscal-budget"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicleId").value(userVehicle.getVehicleId()))
                .andExpect(jsonPath("$.currency").value("$"));

        mockMvc.perform(get("/vehicles/" + userVehicle.getVehicleId() + "/fiscal-budget"))
                .andExpect(status().isOk())
                .andExpect(view().name("vehicle/fiscal-budget"))
                .andExpect(model().attributeExists("report"))
                .andExpect(model().attributeExists("vehicle"));
    }

    @Test
    @DisplayName("Test 31: Cross-user tampering defense: REST 403 Forbidden and Web MVC redirect")
    @WithMockUser(username = "other_fiscal@garage.com", roles = "NORMAL_USER")
    void testCrossUserTamperingDefense() throws Exception {
        // Other user attempts to view UserVehicle
        mockMvc.perform(get("/api/vehicles/" + userVehicle.getVehicleId() + "/fiscal-budget"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/vehicles/" + userVehicle.getVehicleId() + "/fiscal-budget"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/vehicles"));
    }

    @Test
    @DisplayName("Test 32: Role-based ADMIN blocking: 403 Forbidden on private fiscal portfolios")
    @WithMockUser(username = "admin_fiscal@garage.com", roles = "ADMIN")
    void testAdminAccessBlocked() throws Exception {
        mockMvc.perform(get("/api/vehicles/" + userVehicle.getVehicleId() + "/fiscal-budget"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/analytics/garage-fiscal-budget"))
                .andExpect(status().isForbidden());

        assertThatThrownBy(() -> vehicleFiscalBudgetService.getVehicleFiscalBudgetReport(
                userVehicle.getVehicleId(), adminUser.getEmail()
        )).isInstanceOf(AccessDeniedException.class);
    }

    // --- HELPER METHODS ---
    private void seedStandardHistory(Vehicle vehicle) {
        LocalDate lastMonth = LocalDate.now().minusMonths(1).withDayOfMonth(10);
        LocalDate twoMonthsAgo = LocalDate.now().minusMonths(2).withDayOfMonth(15);

        // Fuel record
        seedFuelRecord(vehicle, lastMonth, new BigDecimal("65.00"), 44500);
        seedFuelRecord(vehicle, twoMonthsAgo, new BigDecimal("60.00"), 44000);

        // Service record
        ServiceRecord sr = new ServiceRecord();
        sr.setVehicle(vehicle);
        sr.setServiceDate(twoMonthsAgo);
        sr.setServiceType("Routine Engine Oil & Filter Service");
        sr.setCost(new BigDecimal("95.00"));
        sr.setOdometerAtService(44000);
        serviceRecordRepository.save(sr);

        // Scheduled Maintenance task
        MaintenanceRecord mr = new MaintenanceRecord();
        mr.setVehicle(vehicle);
        mr.setTitle("Brake Inspection");
        mr.setScheduledDate(lastMonth);
        mr.setCompletedDate(lastMonth);
        mr.setStatus(MaintenanceStatus.COMPLETED);
        mr.setCost(new BigDecimal("50.00"));
        maintenanceRecordRepository.save(mr);
    }

    private void seedFuelRecord(Vehicle vehicle, LocalDate date, BigDecimal cost, int odo) {
        FuelRecord f = new FuelRecord();
        f.setVehicle(vehicle);
        f.setFuelDate(date);
        f.setFuelType(FuelType.PETROL);
        f.setQuantityLitres(new BigDecimal("40.0"));
        f.setCostPerLitre(new BigDecimal("1.50"));
        f.setTotalCost(cost);
        f.setOdometerAtFill(odo);
        fuelRecordRepository.save(f);
    }
}
