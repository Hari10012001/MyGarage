package com.mygarage.vehicle;

import com.mygarage.dto.response.*;
import com.mygarage.model.*;
import com.mygarage.model.enums.MaintenanceStatus;
import com.mygarage.model.enums.Role;
import com.mygarage.repository.*;
import com.mygarage.service.VehicleMaintenanceDeficitService;
import com.mygarage.service.VehicleMaintenanceDeficitServiceImpl;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
public class VehicleMaintenanceDeficitModuleTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VehicleMaintenanceDeficitService vehicleMaintenanceDeficitService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private VehicleCategoryRepository vehicleCategoryRepository;

    @Autowired
    private ServiceRecordRepository serviceRecordRepository;

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
        testCategory.setName("Cat_" + UUID.randomUUID().toString().substring(0, 8));
        testCategory.setIcon("car-front");
        testCategory = vehicleCategoryRepository.save(testCategory);

        testUser = userRepository.findByEmail("deficit_user@garage.com").orElseGet(() -> {
            User u = new User();
            u.setEmail("deficit_user@garage.com");
            u.setPasswordHash("hash");
            u.setFullName("Deficit Tester");
            u.setRole(Role.NORMAL_USER);
            return userRepository.save(u);
        });

        otherUser = userRepository.findByEmail("other_deficit@garage.com").orElseGet(() -> {
            User u = new User();
            u.setEmail("other_deficit@garage.com");
            u.setPasswordHash("hash");
            u.setFullName("Other Tester");
            u.setRole(Role.NORMAL_USER);
            return userRepository.save(u);
        });

        adminUser = userRepository.findByEmail("admin_deficit@garage.com").orElseGet(() -> {
            User u = new User();
            u.setEmail("admin_deficit@garage.com");
            u.setPasswordHash("hash");
            u.setFullName("Admin Tester");
            u.setRole(Role.ADMIN);
            return userRepository.save(u);
        });

        userVehicle = new Vehicle();
        userVehicle.setUser(testUser);
        userVehicle.setCategory(testCategory);
        userVehicle.setMake("Honda");
        userVehicle.setModel("Civic");
        userVehicle.setYear(2022);
        userVehicle.setPlateNumber("TN09-MDI01");
        userVehicle.setFuelType("PETROL");
        userVehicle.setCurrentOdometer(15000);
        userVehicle = vehicleRepository.save(userVehicle);

        otherVehicle = new Vehicle();
        otherVehicle.setUser(otherUser);
        otherVehicle.setCategory(testCategory);
        otherVehicle.setMake("Ford");
        otherVehicle.setModel("Focus");
        otherVehicle.setYear(2020);
        otherVehicle.setPlateNumber("TN09-OTHERMDI");
        otherVehicle.setFuelType("PETROL");
        otherVehicle.setCurrentOdometer(40000);
        otherVehicle = vehicleRepository.save(otherVehicle);
    }

    private ServiceRecord createService(Vehicle v, String type, String desc, int odo, LocalDate date) {
        ServiceRecord sr = new ServiceRecord();
        sr.setVehicle(v);
        sr.setServiceType(type);
        sr.setDescription(desc);
        sr.setOdometerAtService(odo);
        sr.setServiceDate(date);
        sr.setCost(BigDecimal.valueOf(150.00));
        return serviceRecordRepository.save(sr);
    }

    private MaintenanceRecord createMaintenance(Vehicle v, String title, LocalDate schedDate, MaintenanceStatus status, BigDecimal cost) {
        MaintenanceRecord mr = new MaintenanceRecord();
        mr.setVehicle(v);
        mr.setTitle(title);
        mr.setDescription("Test maintenance obligation");
        mr.setScheduledDate(schedDate);
        mr.setStatus(status);
        mr.setCost(cost);
        return maintenanceRecordRepository.save(mr);
    }

    @Test
    @DisplayName("1. Pristine vehicle with recent services returns zero debt and PRISTINE status")
    void testEvaluateDeficit_PristineVehicle_ReturnsZeroDebtAndPristineBand() {
        LocalDate recent = LocalDate.now().minusDays(10);
        createService(userVehicle, "Oil Change", "Engine oil and filter replacement", 14500, recent);
        createService(userVehicle, "Brake Service", "Braking system pads inspection", 14500, recent);
        createService(userVehicle, "Cooling Flush", "Coolant system refill", 14500, recent);
        createService(userVehicle, "Tire Rotation", "Tires and suspension alignment", 14500, recent);

        VehicleMaintenanceDeficitReportDTO report = vehicleMaintenanceDeficitService.evaluateVehicleDeficit(
                userVehicle.getVehicleId(), testUser.getUserId()
        );

        assertThat(report.deferredMaintenanceDebt()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(report.maintenanceDeficitIndex()).isEqualTo(0.0);
        assertThat(report.deficitStatus()).isEqualTo("PRISTINE");
        assertThat(report.backlogItems()).isEmpty();
    }

    @Test
    @DisplayName("2. Overdue maintenance records aggregate accurate direct debt")
    void testEvaluateDeficit_OverdueMaintenance_AggregatesAccurateDebt() {
        LocalDate past = LocalDate.now().minusDays(20);
        createMaintenance(userVehicle, "Cabin Filter Replacement", past, MaintenanceStatus.OVERDUE, BigDecimal.valueOf(120.00));

        VehicleMaintenanceDeficitReportDTO report = vehicleMaintenanceDeficitService.evaluateVehicleDeficit(
                userVehicle.getVehicleId(), testUser.getUserId()
        );

        assertThat(report.deferredMaintenanceDebt()).isGreaterThanOrEqualTo(BigDecimal.valueOf(120.00));
        assertThat(report.backlogItems()).isNotEmpty();
    }

    @Test
    @DisplayName("3. Breached consumable interval adds authoritative benchmark cost to debt")
    void testEvaluateDeficit_BreachedConsumable_AddsBenchmarkCostToDebt() {
        LocalDate oldDate = LocalDate.now().minusDays(200); // Exceeds 180 days interval
        createService(userVehicle, "Oil Change", "Engine oil and filter", 2000, oldDate);

        // Also add fresh services for other subsystems to isolate engine oil breach
        createService(userVehicle, "Brake Service", "Brakes", 14500, LocalDate.now().minusDays(5));
        createService(userVehicle, "Coolant", "Cooling system", 14500, LocalDate.now().minusDays(5));
        createService(userVehicle, "Tires", "Tires alignment", 14500, LocalDate.now().minusDays(5));

        VehicleMaintenanceDeficitReportDTO report = vehicleMaintenanceDeficitService.evaluateVehicleDeficit(
                userVehicle.getVehicleId(), testUser.getUserId()
        );

        // Authoritative benchmark cost for ENGINE_OIL_AND_FILTER is $95.00
        assertThat(report.deferredMaintenanceDebt()).isEqualByComparingTo(BigDecimal.valueOf(95.00));
        assertThat(report.backlogItems()).hasSize(1);
        assertThat(report.backlogItems().get(0).subsystem()).contains("Engine Oil");
    }

    @Test
    @DisplayName("4. Duplicate obligation prevention does not double-count active task and consumable breach")
    void testEvaluateDeficit_DuplicateObligationPrevention_DoesNotDoubleCountTaskAndConsumable() {
        LocalDate past = LocalDate.now().minusDays(25);
        // Breached brake service in logbook
        createService(userVehicle, "Brake Service", "Brakes replacement", 2000, LocalDate.now().minusDays(400));
        // Active overdue task covering braking system
        createMaintenance(userVehicle, "Brake Pad Replacement", past, MaintenanceStatus.OVERDUE, BigDecimal.valueOf(180.00));

        // Satisfy other consumables
        createService(userVehicle, "Oil Change", "Engine oil", 14500, LocalDate.now().minusDays(5));
        createService(userVehicle, "Coolant", "Cooling system", 14500, LocalDate.now().minusDays(5));
        createService(userVehicle, "Tires", "Tires alignment", 14500, LocalDate.now().minusDays(5));

        VehicleMaintenanceDeficitReportDTO report = vehicleMaintenanceDeficitService.evaluateVehicleDeficit(
                userVehicle.getVehicleId(), testUser.getUserId()
        );

        // Should count ONLY ONCE ($180.00), not double counted to $360.00
        assertThat(report.deferredMaintenanceDebt()).isEqualByComparingTo(BigDecimal.valueOf(180.00));
        assertThat(report.backlogItems()).hasSize(1);
    }

    @Test
    @DisplayName("5. Near-term exposure strictly excludes upcoming tasks from deferred debt")
    void testEvaluateDeficit_NearTermExposureSeparation_ExcludesUpcomingTasksFromDeferredDebt() {
        LocalDate future = LocalDate.now().plusDays(15);
        createMaintenance(userVehicle, "Spark Plug Replacement", future, MaintenanceStatus.UPCOMING, BigDecimal.valueOf(110.00));

        // Satisfy all consumables
        createService(userVehicle, "Oil Change", "Engine oil", 14500, LocalDate.now().minusDays(5));
        createService(userVehicle, "Brakes", "Braking system", 14500, LocalDate.now().minusDays(5));
        createService(userVehicle, "Coolant", "Cooling system", 14500, LocalDate.now().minusDays(5));
        createService(userVehicle, "Tires", "Tires", 14500, LocalDate.now().minusDays(5));

        VehicleMaintenanceDeficitReportDTO report = vehicleMaintenanceDeficitService.evaluateVehicleDeficit(
                userVehicle.getVehicleId(), testUser.getUserId()
        );

        // Deferred debt must be 0; near-term exposure must capture $110.00
        assertThat(report.deferredMaintenanceDebt()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(report.totalNearTermExposure()).isEqualByComparingTo(BigDecimal.valueOf(110.00));
        assertThat(report.total30DayMaintenanceLiability()).isEqualByComparingTo(BigDecimal.valueOf(110.00));
        assertThat(report.nearTermItems()).hasSize(1);
    }

    @Test
    @DisplayName("6. MDI percentage is calculated accurately against residual asset value")
    void testEvaluateDeficit_MdiPercentCalculatedCorrectly() {
        LocalDate past = LocalDate.now().minusDays(10);
        createMaintenance(userVehicle, "General Inspection", past, MaintenanceStatus.OVERDUE, BigDecimal.valueOf(500.00));

        VehicleMaintenanceDeficitReportDTO report = vehicleMaintenanceDeficitService.evaluateVehicleDeficit(
                userVehicle.getVehicleId(), testUser.getUserId()
        );

        double expectedMdi = (report.deferredMaintenanceDebt().doubleValue() / report.replacementAssetValue().doubleValue()) * 100.0;
        assertThat(report.maintenanceDeficitIndex()).isCloseTo(expectedMdi, org.assertj.core.data.Offset.offset(0.1));
    }

    @Test
    @DisplayName("7. Extreme debt clamps MDI at exactly 100.0%")
    void testEvaluateDeficit_MdiScore_ClampedAtOneHundred() {
        LocalDate past = LocalDate.now().minusDays(10);
        // Astronomical maintenance cost
        createMaintenance(userVehicle, "Complete Engine & Chassis Rebuild", past, MaintenanceStatus.OVERDUE, BigDecimal.valueOf(500000.00));

        VehicleMaintenanceDeficitReportDTO report = vehicleMaintenanceDeficitService.evaluateVehicleDeficit(
                userVehicle.getVehicleId(), testUser.getUserId()
        );

        assertThat(report.maintenanceDeficitIndex()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("8. Powertrain cascade applies engine multiplier (5.5x)")
    void testEvaluateDeficit_PowertrainCascade_AppliesEngineMultiplier() {
        LocalDate past = LocalDate.now().minusDays(10);
        createMaintenance(userVehicle, "Engine Oil and Filter", past, MaintenanceStatus.OVERDUE, BigDecimal.valueOf(95.00));

        VehicleMaintenanceDeficitReportDTO report = vehicleMaintenanceDeficitService.evaluateVehicleDeficit(
                userVehicle.getVehicleId(), testUser.getUserId()
        );

        DeferredBacklogItemDTO oilItem = report.backlogItems().stream()
                .filter(i -> i.subsystem().contains("Engine Oil"))
                .findFirst().orElseThrow();

        assertThat(oilItem.neglectCascadeMultiplier()).isEqualTo(5.5);
        assertThat(oilItem.compoundNeglectCostExposure()).isEqualByComparingTo(BigDecimal.valueOf(522.50));
    }

    @Test
    @DisplayName("9. Braking cascade applies brake multiplier (3.2x)")
    void testEvaluateDeficit_BrakeCascade_AppliesBrakeMultiplier() {
        LocalDate past = LocalDate.now().minusDays(10);
        createMaintenance(userVehicle, "Braking System Pad Replacement", past, MaintenanceStatus.OVERDUE, BigDecimal.valueOf(180.00));

        VehicleMaintenanceDeficitReportDTO report = vehicleMaintenanceDeficitService.evaluateVehicleDeficit(
                userVehicle.getVehicleId(), testUser.getUserId()
        );

        DeferredBacklogItemDTO brakeItem = report.backlogItems().stream()
                .filter(i -> i.subsystem().contains("Braking"))
                .findFirst().orElseThrow();

        assertThat(brakeItem.neglectCascadeMultiplier()).isEqualTo(3.2);
        assertThat(brakeItem.compoundNeglectCostExposure()).isEqualByComparingTo(BigDecimal.valueOf(576.00));
    }

    @Test
    @DisplayName("10. Cooling cascade applies coolant multiplier (4.8x)")
    void testEvaluateDeficit_CoolingCascade_AppliesCoolantMultiplier() {
        LocalDate past = LocalDate.now().minusDays(10);
        createMaintenance(userVehicle, "Cooling System and Radiator Service", past, MaintenanceStatus.OVERDUE, BigDecimal.valueOf(140.00));

        VehicleMaintenanceDeficitReportDTO report = vehicleMaintenanceDeficitService.evaluateVehicleDeficit(
                userVehicle.getVehicleId(), testUser.getUserId()
        );

        DeferredBacklogItemDTO coolItem = report.backlogItems().stream()
                .filter(i -> i.subsystem().contains("Cooling"))
                .findFirst().orElseThrow();

        assertThat(coolItem.neglectCascadeMultiplier()).isEqualTo(4.8);
        assertThat(coolItem.compoundNeglectCostExposure()).isEqualByComparingTo(BigDecimal.valueOf(672.00));
    }

    @Test
    @DisplayName("11. Tires cascade applies tires/suspension multiplier (2.5x)")
    void testEvaluateDeficit_TiresCascade_AppliesTiresMultiplier() {
        LocalDate past = LocalDate.now().minusDays(10);
        createMaintenance(userVehicle, "Tires and Alignment Overhaul", past, MaintenanceStatus.OVERDUE, BigDecimal.valueOf(160.00));

        VehicleMaintenanceDeficitReportDTO report = vehicleMaintenanceDeficitService.evaluateVehicleDeficit(
                userVehicle.getVehicleId(), testUser.getUserId()
        );

        DeferredBacklogItemDTO tireItem = report.backlogItems().stream()
                .filter(i -> i.subsystem().contains("Tires"))
                .findFirst().orElseThrow();

        assertThat(tireItem.neglectCascadeMultiplier()).isEqualTo(2.5);
        assertThat(tireItem.compoundNeglectCostExposure()).isEqualByComparingTo(BigDecimal.valueOf(400.00));
    }

    @Test
    @DisplayName("12. Cost-of-inaction delta and net savings are calculated accurately")
    void testEvaluateDeficit_CostOfInactionPremium_CalculatedAccurately() {
        LocalDate past = LocalDate.now().minusDays(10);
        createMaintenance(userVehicle, "Engine Oil Overhaul", past, MaintenanceStatus.OVERDUE, BigDecimal.valueOf(100.00));

        VehicleMaintenanceDeficitReportDTO report = vehicleMaintenanceDeficitService.evaluateVehicleDeficit(
                userVehicle.getVehicleId(), testUser.getUserId()
        );

        assertThat(report.compoundNeglectCostExposure()).isGreaterThan(report.deferredMaintenanceDebt());
        assertThat(report.inactionMultiplier()).isGreaterThan(1.0);
    }

    @Test
    @DisplayName("13. RME scoring incorporates empirical days overdue urgency")
    void testEvaluateDeficit_RmeScoring_IncludesUrgencyAndPriorityWeights() {
        // Two overdue items of the same general subsystem ($110 benchmark)
        createMaintenance(userVehicle, "General Inspection A", LocalDate.now().minusDays(5), MaintenanceStatus.OVERDUE, BigDecimal.valueOf(110.00));
        createMaintenance(userVehicle, "General Inspection B", LocalDate.now().minusDays(60), MaintenanceStatus.OVERDUE, BigDecimal.valueOf(110.00));

        VehicleMaintenanceDeficitReportDTO report = vehicleMaintenanceDeficitService.evaluateVehicleDeficit(
                userVehicle.getVehicleId(), testUser.getUserId()
        );

        DeferredBacklogItemDTO itemRecent = report.backlogItems().stream()
                .filter(i -> i.taskTitle().equals("General Inspection A")).findFirst().orElseThrow();
        DeferredBacklogItemDTO itemOlder = report.backlogItems().stream()
                .filter(i -> i.taskTitle().equals("General Inspection B")).findFirst().orElseThrow();

        assertThat(itemOlder.riskMitigationEfficiency()).isGreaterThan(itemRecent.riskMitigationEfficiency());
    }

    @Test
    @DisplayName("14. Recovery roadmap executes deterministic multi-stage tie-breaking")
    void testEvaluateDeficit_RecoveryRoadmap_DeterministicTieBreaking() {
        LocalDate past = LocalDate.now().minusDays(20);
        createMaintenance(userVehicle, "Brake Fluid Flush", past, MaintenanceStatus.OVERDUE, BigDecimal.valueOf(180.00));
        createMaintenance(userVehicle, "Engine Oil Service", past, MaintenanceStatus.OVERDUE, BigDecimal.valueOf(95.00));

        VehicleMaintenanceDeficitReportDTO report = vehicleMaintenanceDeficitService.evaluateVehicleDeficit(
                userVehicle.getVehicleId(), testUser.getUserId()
        );

        List<BacklogTriageRoadmapItemDTO> roadmap = report.triageRoadmap();
        assertThat(roadmap).isNotEmpty();
        // Ranks must be strictly sequential 1, 2, ...
        for (int i = 0; i < roadmap.size(); i++) {
            assertThat(roadmap.get(i).triageRank()).isEqualTo(i + 1);
        }
    }

    @Test
    @DisplayName("15. Null, zero, or negative costs safely fallback to authoritative domain benchmarks")
    void testEvaluateDeficit_NullZeroNegativeCost_SafelyUsesAuthoritativeBenchmark() {
        LocalDate past = LocalDate.now().minusDays(10);
        createMaintenance(userVehicle, "Brake Inspection", past, MaintenanceStatus.OVERDUE, null);

        VehicleMaintenanceDeficitReportDTO report = vehicleMaintenanceDeficitService.evaluateVehicleDeficit(
                userVehicle.getVehicleId(), testUser.getUserId()
        );

        DeferredBacklogItemDTO brakeItem = report.backlogItems().stream()
                .filter(i -> i.subsystem().contains("Braking")).findFirst().orElseThrow();

        // Null cost defaults to $180.00 for BRAKING_SYSTEM
        assertThat(brakeItem.directRemediationCost()).isEqualByComparingTo(BigDecimal.valueOf(180.00));
    }

    @Test
    @DisplayName("16. Non-monotonic odometer readings are handled safely without negative mileage")
    void testEvaluateDeficit_NonMonotonicOdometer_SafelyHandledWithoutNegativeMileage() {
        // Vehicle odometer is 15000, service record with higher odometer 18000
        createService(userVehicle, "Service Visit", "Inspection", 18000, LocalDate.now().minusDays(10));

        VehicleMaintenanceDeficitReportDTO report = vehicleMaintenanceDeficitService.evaluateVehicleDeficit(
                userVehicle.getVehicleId(), testUser.getUserId()
        );

        assertThat(report).isNotNull();
    }

    @Test
    @DisplayName("17. Completed tasks are strictly excluded from backlog debt")
    void testEvaluateDeficit_CompletedTasks_StrictlyExcludedFromBacklog() {
        MaintenanceRecord mr = createMaintenance(userVehicle, "Past Completed Oil Service", LocalDate.now().minusDays(30), MaintenanceStatus.COMPLETED, BigDecimal.valueOf(95.00));
        mr.setCompletedDate(LocalDate.now().minusDays(29));
        maintenanceRecordRepository.save(mr);

        // Satisfy all consumables
        createService(userVehicle, "Oil", "Oil change", 14500, LocalDate.now().minusDays(5));
        createService(userVehicle, "Brakes", "Brake service", 14500, LocalDate.now().minusDays(5));
        createService(userVehicle, "Coolant", "Cooling flush", 14500, LocalDate.now().minusDays(5));
        createService(userVehicle, "Tires", "Tires", 14500, LocalDate.now().minusDays(5));

        VehicleMaintenanceDeficitReportDTO report = vehicleMaintenanceDeficitService.evaluateVehicleDeficit(
                userVehicle.getVehicleId(), testUser.getUserId()
        );

        assertThat(report.deferredMaintenanceDebt()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(report.backlogItems()).isEmpty();
    }

    @Test
    @DisplayName("18. Vehicle with zero history logs produces graceful report with conservative defaults")
    void testEvaluateDeficit_ZeroHistoryVehicle_HandlesGracefullyWithConservativeDefaults() {
        Vehicle cleanVeh = new Vehicle();
        cleanVeh.setUser(testUser);
        cleanVeh.setCategory(testCategory);
        cleanVeh.setMake("Mazda");
        cleanVeh.setModel("CX-5");
        cleanVeh.setYear(2023);
        cleanVeh.setPlateNumber("TN09-ZERO01");
        cleanVeh.setFuelType("PETROL");
        cleanVeh.setCurrentOdometer(5000);
        cleanVeh = vehicleRepository.save(cleanVeh);

        VehicleMaintenanceDeficitReportDTO report = vehicleMaintenanceDeficitService.evaluateVehicleDeficit(
                cleanVeh.getVehicleId(), testUser.getUserId()
        );

        assertThat(report).isNotNull();
        assertThat(report.plateNumber()).isEqualTo("TN09-ZERO01");
    }

    @Test
    @DisplayName("19. Maintenance bankruptcy triggers CRITICAL deficit status when MDI >= 25%")
    void testEvaluateDeficit_MaintenanceBankruptcy_TriggersWhenMdiExceeds25Percent() {
        LocalDate past = LocalDate.now().minusDays(30);
        // Residual value of vehicle is ~$15,000. $4,500 debt represents ~30% MDI
        createMaintenance(userVehicle, "Catastrophic Transmission Overhaul", past, MaintenanceStatus.OVERDUE, BigDecimal.valueOf(4500.00));

        VehicleMaintenanceDeficitReportDTO report = vehicleMaintenanceDeficitService.evaluateVehicleDeficit(
                userVehicle.getVehicleId(), testUser.getUserId()
        );

        assertThat(report.maintenanceDeficitIndex()).isGreaterThanOrEqualTo(25.0);
        assertThat(report.deficitStatus()).isEqualTo("CRITICAL");
        assertThat(report.executiveSummary()).contains("CRITICAL");
    }

    @Test
    @DisplayName("20. Garage deficit aggregates fleet-wide debts and rankings accurately")
    void testEvaluateGarageDeficit_AggregatesFleetDebtAccurately() {
        LocalDate past = LocalDate.now().minusDays(15);
        createMaintenance(userVehicle, "Suspension Bushings", past, MaintenanceStatus.OVERDUE, BigDecimal.valueOf(250.00));

        GarageMaintenanceDeficitMatrixDTO matrix = vehicleMaintenanceDeficitService.evaluateGarageDeficit(testUser.getUserId());

        assertThat(matrix.totalVehicles()).isGreaterThanOrEqualTo(1);
        assertThat(matrix.totalFleetDeferredDebt()).isGreaterThanOrEqualTo(BigDecimal.valueOf(250.00));
        assertThat(matrix.vehicleSummaries()).isNotEmpty();
    }

    @Test
    @DisplayName("21. Empty garage returns graceful empty deficit matrix")
    void testEvaluateGarageDeficit_EmptyGarage_ReturnsGracefulEmptyMatrix() {
        User emptyUser = new User();
        emptyUser.setEmail("empty_garage_" + UUID.randomUUID().toString().substring(0, 8) + "@garage.com");
        emptyUser.setPasswordHash("hash");
        emptyUser.setFullName("Empty Garage");
        emptyUser.setRole(Role.NORMAL_USER);
        emptyUser = userRepository.save(emptyUser);

        GarageMaintenanceDeficitMatrixDTO matrix = vehicleMaintenanceDeficitService.evaluateGarageDeficit(emptyUser.getUserId());

        assertThat(matrix.totalVehicles()).isEqualTo(0);
        assertThat(matrix.garageFleetMDI()).isEqualTo(0.0);
        assertThat(matrix.fleetDeficitStatus()).isEqualTo("PRISTINE");
        assertThat(matrix.vehicleSummaries()).isEmpty();
    }

    @Test
    @DisplayName("22. Security Web MVC: Unauthorized cross-user access redirects with errorMsg flash")
    @WithMockUser(username = "deficit_user@garage.com", roles = "NORMAL_USER")
    void testSecurity_WebMvc_UnauthorizedAccess_RedirectsWithFlashError() throws Exception {
        // Authenticated as testUser, but accessing otherVehicle
        mockMvc.perform(get("/vehicles/" + otherVehicle.getVehicleId() + "/maintenance-deficit"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/vehicles"))
                .andExpect(flash().attributeExists("errorMsg"));
    }

    @Test
    @DisplayName("23. Security Web MVC: Admin access is denied with 403")
    @WithMockUser(username = "admin_deficit@garage.com", roles = "ADMIN")
    void testSecurity_WebMvc_AdminAccess_Throws403() throws Exception {
        mockMvc.perform(get("/vehicles/" + userVehicle.getVehicleId() + "/maintenance-deficit"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("24. Security REST API: Unauthorized cross-user access returns 403 Forbidden")
    @WithMockUser(username = "deficit_user@garage.com", roles = "NORMAL_USER")
    void testSecurity_RestApi_UnauthorizedAccess_Returns403Forbidden() throws Exception {
        // Authenticated as testUser, but requesting otherVehicle via REST
        mockMvc.perform(get("/api/vehicles/" + otherVehicle.getVehicleId() + "/maintenance-deficit"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("25. Security REST API: Admin access returns 403 Forbidden")
    @WithMockUser(username = "admin_deficit@garage.com", roles = "ADMIN")
    void testSecurity_RestApi_AdminAccess_Returns403Forbidden() throws Exception {
        mockMvc.perform(get("/api/vehicles/" + userVehicle.getVehicleId() + "/maintenance-deficit"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("26. Security REST API: Authorized owner returns 200 OK with valid report payload")
    @WithMockUser(username = "deficit_user@garage.com", roles = "NORMAL_USER")
    void testSecurity_RestApi_AuthorizedOwner_Returns200WithValidReport() throws Exception {
        mockMvc.perform(get("/api/vehicles/" + userVehicle.getVehicleId() + "/maintenance-deficit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicleId").value(userVehicle.getVehicleId()))
                .andExpect(jsonPath("$.plateNumber").value("TN09-MDI01"))
                .andExpect(jsonPath("$.analyticalDisclaimer").isNotEmpty())
                .andExpect(jsonPath("$.benchmarkPolicyNote").isNotEmpty());
    }
}
