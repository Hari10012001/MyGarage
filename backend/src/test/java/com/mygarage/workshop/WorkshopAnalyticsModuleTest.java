package com.mygarage.workshop;

import com.mygarage.dto.response.GarageWorkshopEcosystemMatrixDTO;
import com.mygarage.dto.response.WorkshopAnalyticsReportDTO;
import com.mygarage.model.ServiceRecord;
import com.mygarage.model.User;
import com.mygarage.model.Vehicle;
import com.mygarage.model.VehicleCategory;
import com.mygarage.model.enums.Role;
import com.mygarage.repository.ServiceRecordRepository;
import com.mygarage.repository.UserRepository;
import com.mygarage.repository.VehicleCategoryRepository;
import com.mygarage.repository.VehicleRepository;
import com.mygarage.service.WorkshopAnalyticsService;
import com.mygarage.service.impl.WorkshopAnalyticsServiceImpl;
import com.mygarage.service.impl.WorkshopAnalyticsServiceImpl.GarageNameNormalizer;
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
public class WorkshopAnalyticsModuleTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WorkshopAnalyticsService workshopAnalyticsService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private VehicleCategoryRepository vehicleCategoryRepository;

    @Autowired
    private ServiceRecordRepository serviceRecordRepository;

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

        testUser = userRepository.findByEmail("workshop_user@garage.com").orElseGet(() -> {
            User u = new User();
            u.setEmail("workshop_user@garage.com");
            u.setPasswordHash("hash");
            u.setFullName("Workshop Tester");
            u.setRole(Role.NORMAL_USER);
            return userRepository.save(u);
        });

        otherUser = userRepository.findByEmail("other_workshop@garage.com").orElseGet(() -> {
            User u = new User();
            u.setEmail("other_workshop@garage.com");
            u.setPasswordHash("hash");
            u.setFullName("Other Workshop Tester");
            u.setRole(Role.NORMAL_USER);
            return userRepository.save(u);
        });

        adminUser = userRepository.findByEmail("admin_workshop@garage.com").orElseGet(() -> {
            User u = new User();
            u.setEmail("admin_workshop@garage.com");
            u.setPasswordHash("hash");
            u.setFullName("Admin Workshop Tester");
            u.setRole(Role.ADMIN);
            return userRepository.save(u);
        });

        userVehicle = new Vehicle();
        userVehicle.setUser(testUser);
        userVehicle.setCategory(testCategory);
        userVehicle.setMake("Toyota");
        userVehicle.setModel("Camry");
        userVehicle.setYear(2022);
        userVehicle.setPlateNumber("WKS-" + UUID.randomUUID().toString().substring(0, 6));
        userVehicle.setCurrentOdometer(30000);
        userVehicle = vehicleRepository.save(userVehicle);

        otherVehicle = new Vehicle();
        otherVehicle.setUser(otherUser);
        otherVehicle.setCategory(testCategory);
        otherVehicle.setMake("Honda");
        otherVehicle.setModel("Civic");
        otherVehicle.setYear(2021);
        otherVehicle.setPlateNumber("OTH-" + UUID.randomUUID().toString().substring(0, 6));
        otherVehicle.setCurrentOdometer(40000);
        otherVehicle = vehicleRepository.save(otherVehicle);
    }

    private ServiceRecord createService(Vehicle v, String garage, String type, String desc, BigDecimal cost, LocalDate date, Integer odo) {
        ServiceRecord sr = new ServiceRecord();
        sr.setVehicle(v);
        sr.setGarageName(garage);
        sr.setServiceType(type);
        sr.setDescription(desc);
        sr.setCost(cost);
        sr.setServiceDate(date);
        sr.setOdometerAtService(odo);
        return serviceRecordRepository.save(sr);
    }

    // 1. GarageNameNormalizer clean standardization
    @Test
    @DisplayName("1. GarageNameNormalizer collapses whitespace and strips punctuation")
    void testGarageNameNormalizer_CleanStandardization() {
        String raw = "  Apex   Auto   Care,   LLC  ";
        String key = GarageNameNormalizer.normalizeKey(raw);
        assertThat(key).isEqualTo("APEX_AUTO_CARE");

        String title = GarageNameNormalizer.formatCanonicalTitle(raw);
        assertThat(title).isEqualTo("Apex Auto Care");
    }

    // 2. GarageNameNormalizer location preserved distinct workshops
    @Test
    @DisplayName("2. GarageNameNormalizer preserves locations as distinct workshops")
    void testGarageNameNormalizer_LocationPreserved_DistinctWorkshops() {
        String shop1 = "Toyota Service Center, Anna Nagar Pvt Ltd";
        String shop2 = "Toyota Service Center, Guindy Ltd";

        String key1 = GarageNameNormalizer.normalizeKey(shop1);
        String key2 = GarageNameNormalizer.normalizeKey(shop2);

        assertThat(key1).isEqualTo("TOYOTA_SERVICE_CENTER_ANNA_NAGAR");
        assertThat(key2).isEqualTo("TOYOTA_SERVICE_CENTER_GUINDY");
        assertThat(key1).isNotEqualTo(key2);
    }

    // 3. GarageNameNormalizer trailing suffix pruned only
    @Test
    @DisplayName("3. GarageNameNormalizer prunes trailing legal suffixes only")
    void testGarageNameNormalizer_TrailingSuffixPruned() {
        String raw = "Precision Motors Private Limited";
        String key = GarageNameNormalizer.normalizeKey(raw);
        assertThat(key).isEqualTo("PRECISION_MOTORS");

        // Intermediate occurrence of "Co" or "Ltd" not at end
        String raw2 = "Colorado Auto Care Co";
        assertThat(GarageNameNormalizer.normalizeKey(raw2)).isEqualTo("COLORADO_AUTO_CARE");
    }

    // 4. GarageNameNormalizer null/blank defaults to INDEPENDENT_UNSPECIFIED
    @Test
    @DisplayName("4. GarageNameNormalizer handles null, empty, N/A strings")
    void testGarageNameNormalizer_NullOrBlank_DefaultsToUnspecified() {
        assertThat(GarageNameNormalizer.normalizeKey(null)).isEqualTo("INDEPENDENT_UNSPECIFIED");
        assertThat(GarageNameNormalizer.normalizeKey("")).isEqualTo("INDEPENDENT_UNSPECIFIED");
        assertThat(GarageNameNormalizer.normalizeKey("   ")).isEqualTo("INDEPENDENT_UNSPECIFIED");
        assertThat(GarageNameNormalizer.normalizeKey("N/A")).isEqualTo("INDEPENDENT_UNSPECIFIED");
        assertThat(GarageNameNormalizer.normalizeKey("null")).isEqualTo("INDEPENDENT_UNSPECIFIED");
    }

    // 5. Canonical display name representative selection
    @Test
    @DisplayName("5. Canonical display name deterministic representative selection")
    void testGarageNameNormalizer_CanonicalDisplayName_RepresentativeSelection() {
        List<String> rawList = List.of(
                "Apex Auto Care",
                "apex auto care pvt ltd",
                "Apex Auto Care",
                "Apex Auto Care - Downtown"
        );
        String display = GarageNameNormalizer.selectCanonicalDisplayName(rawList);
        assertThat(display).isEqualTo("Apex Auto Care");

        // Tie-breaker on frequency: longest string wins
        List<String> tieList = List.of(
                "Apex Auto",
                "Apex Auto Care Center"
        );
        String tieDisplay = GarageNameNormalizer.selectCanonicalDisplayName(tieList);
        assertThat(tieDisplay).isEqualTo("Apex Auto Care Center");
    }

    // 6. Subsystem classification: Engine Oil
    @Test
    @DisplayName("6. Subsystem classification matches Engine Oil & Filter")
    void testSubsystemClassification_EngineOil() {
        ServiceRecord sr = new ServiceRecord();
        sr.setServiceType("Synthetic Oil Change");
        sr.setDescription("Replaced 0w20 synthetic engine oil and filter");
        assertThat(WorkshopAnalyticsServiceImpl.classifySubsystem(sr)).isEqualTo("ENGINE_OIL_AND_FILTER");
    }

    // 7. Subsystem classification: Braking
    @Test
    @DisplayName("7. Subsystem classification matches Braking System")
    void testSubsystemClassification_Braking() {
        ServiceRecord sr = new ServiceRecord();
        sr.setServiceType("Brake Service");
        sr.setDescription("Front ceramic pads and rotor resurfacing");
        assertThat(WorkshopAnalyticsServiceImpl.classifySubsystem(sr)).isEqualTo("BRAKING_SYSTEM");
    }

    // 8. Subsystem classification: Cooling
    @Test
    @DisplayName("8. Subsystem classification matches Cooling System & Fluids")
    void testSubsystemClassification_Cooling() {
        ServiceRecord sr = new ServiceRecord();
        sr.setServiceType("Radiator Flush");
        sr.setDescription("Replaced water pump and antifreeze coolant");
        assertThat(WorkshopAnalyticsServiceImpl.classifySubsystem(sr)).isEqualTo("COOLING_SYSTEM_FLUIDS");
    }

    // 9. Subsystem classification: Tires & Suspension
    @Test
    @DisplayName("9. Subsystem classification matches Tires & Suspension")
    void testSubsystemClassification_Tires() {
        ServiceRecord sr = new ServiceRecord();
        sr.setServiceType("Wheel Alignment");
        sr.setDescription("4-wheel tire rotation and shock strut inspection");
        assertThat(WorkshopAnalyticsServiceImpl.classifySubsystem(sr)).isEqualTo("TIRES_AND_SUSPENSION");
    }

    // 10. Subsystem classification: Major PMS
    @Test
    @DisplayName("10. Subsystem classification matches Major PMS")
    void testSubsystemClassification_MajorPms() {
        ServiceRecord sr = new ServiceRecord();
        sr.setServiceType("Periodic Maintenance");
        sr.setDescription("Major PMS 60k service with timing belt replacement");
        assertThat(WorkshopAnalyticsServiceImpl.classifySubsystem(sr)).isEqualTo("MAJOR_PMS");
    }

    // 11. Subsystem classification fallback to General Maintenance
    @Test
    @DisplayName("11. Subsystem classification falls back to General Maintenance")
    void testSubsystemClassification_FallbackGeneral() {
        ServiceRecord sr = new ServiceRecord();
        sr.setServiceType("Safety Inspection");
        sr.setDescription("Annual state emission certificate and wiper blade replacement");
        assertThat(WorkshopAnalyticsServiceImpl.classifySubsystem(sr)).isEqualTo("GENERAL_MAINTENANCE");
    }

    // 12. Excludes future dated service records
    @Test
    @DisplayName("12. Completed service visit definition excludes future dates")
    void testCompletedServiceVisit_ExcludesFutureDates() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        createService(userVehicle, "Future Auto", "Oil Change", "Booked oil", new BigDecimal("100.00"), tomorrow, 31000);

        GarageWorkshopEcosystemMatrixDTO ecosystem = workshopAnalyticsService.getGarageWorkshopEcosystem(testUser);
        assertThat(ecosystem.totalUniqueWorkshops()).isEqualTo(0);
        assertThat(ecosystem.totalFleetServiceVisits()).isEqualTo(0);
    }

    // 13. Authoritative eligible cost policy: null and negative excluded
    @Test
    @DisplayName("13. Authoritative eligible cost policy excludes null and negative costs")
    void testAuthoritativeEligibleCost_NullAndNegativeExcluded() {
        LocalDate today = LocalDate.now();
        createService(userVehicle, "Valid Garage", "Inspection", "Check 1", BigDecimal.ZERO, today.minusDays(10), 20000);
        createService(userVehicle, "Valid Garage", "Inspection", "Check 2", null, today.minusDays(5), 20500);
        createService(userVehicle, "Valid Garage", "Inspection", "Check 3", new BigDecimal("110.00"), today.minusDays(1), 21000);

        WorkshopAnalyticsReportDTO report = workshopAnalyticsService.getWorkshopDetail(testUser, "VALID_GARAGE");
        assertThat(report.totalVisits()).isEqualTo(3);
        assertThat(report.totalSpend()).isEqualByComparingTo("110.00");
    }

    // 14. WPI calculation: Exact parity ($180 actual on $180 benchmark = 100.0)
    @Test
    @DisplayName("14. WPI calculation yields exact 100.0 market parity")
    void testWpiCalculation_ExactParity() {
        LocalDate today = LocalDate.now();
        createService(userVehicle, "Parity Brakes", "Brake Pad Replacement", "Front pads", new BigDecimal("180.00"), today.minusDays(5), 25000);
        createService(userVehicle, "Parity Brakes", "Brake Rotor Replacement", "Rear rotors", new BigDecimal("180.00"), today.minusDays(2), 25500);

        WorkshopAnalyticsReportDTO report = workshopAnalyticsService.getWorkshopDetail(testUser, "PARITY_BRAKES");
        assertThat(report.workshopPriceIndex()).isEqualByComparingTo("100.0");
        assertThat(report.pricingCategory()).isEqualTo("MARKET_PARITY");
    }

    // 15. WPI calculation: Warranty only workshop
    @Test
    @DisplayName("15. WPI calculation handles warranty only workshop with baseline 100.0")
    void testWpiCalculation_WarrantyOnlyWorkshop() {
        LocalDate today = LocalDate.now();
        createService(userVehicle, "Dealer Free Warranty", "Recall Inspection", "Airbag check", BigDecimal.ZERO, today.minusDays(20), 10000);
        createService(userVehicle, "Dealer Free Warranty", "Warranty Fix", "Sensor swap", BigDecimal.ZERO, today.minusDays(10), 10500);

        WorkshopAnalyticsReportDTO report = workshopAnalyticsService.getWorkshopDetail(testUser, "DEALER_FREE_WARRANTY");
        assertThat(report.warrantyVisitCount()).isEqualTo(2);
        assertThat(report.workshopPriceIndex()).isEqualByComparingTo("100.0");
        assertThat(report.totalSpend()).isEqualByComparingTo("0.00");
    }

    // 16. WPI calculation: Zero cost excluded from priced benchmark denominator
    @Test
    @DisplayName("16. Zero cost warranty visit excluded from WPI denominator")
    void testWpiCalculation_ZeroCostExcludedFromDenominator() {
        LocalDate today = LocalDate.now();
        // Benchmark for oil is 95.00. Paid 95.00 -> 100.0 WPI.
        createService(userVehicle, "Hybrid Dealer", "Synthetic Oil Change", "Oil service", new BigDecimal("95.00"), today.minusDays(20), 10000);
        // Warranty visit ($0.00) on brake (benchmark 180.00). If included, WPI would be 95 / 275 = 34.5%.
        // Correct behavior: excluded from denominator, so WPI remains 95 / 95 = 100.0%.
        createService(userVehicle, "Hybrid Dealer", "Warranty Recall", "Brake recall check", BigDecimal.ZERO, today.minusDays(5), 10200);

        WorkshopAnalyticsReportDTO report = workshopAnalyticsService.getWorkshopDetail(testUser, "HYBRID_DEALER");
        assertThat(report.workshopPriceIndex()).isEqualByComparingTo("100.0");
        assertThat(report.warrantyVisitCount()).isEqualTo(1);
    }

    // 17. VRP calculation: Clean service history yields 0.0%
    @Test
    @DisplayName("17. VRP calculation on clean history yields 0.0%")
    void testVrpCalculation_NoRework_ZeroPercent() {
        LocalDate today = LocalDate.now();
        createService(userVehicle, "Clean Auto", "Oil Change", "Routine oil", new BigDecimal("95.00"), today.minusDays(100), 10000);
        createService(userVehicle, "Clean Auto", "Brake Service", "Routine pads", new BigDecimal("180.00"), today.minusDays(20), 15000);

        WorkshopAnalyticsReportDTO report = workshopAnalyticsService.getWorkshopDetail(testUser, "CLEAN_AUTO");
        assertThat(report.vendorReworkProbability()).isEqualByComparingTo("0.0");
        assertThat(report.reworkEventCount()).isEqualTo(0);
    }

    // 18. VRP calculation: Planned routine recurrence NOT flagged as rework (false-positive suppression)
    @Test
    @DisplayName("18. Planned routine maintenance recurrence within 30 days is NOT flagged as rework")
    void testVrpCalculation_PlannedRoutineRecurrence_NotFlaggedAsRework() {
        LocalDate today = LocalDate.now();
        // Visit A: Oil change
        createService(userVehicle, "Quick Lube", "Synthetic Oil Change", "Regular oil service", new BigDecimal("95.00"), today.minusDays(20), 20000);
        // Visit B: 10 days later, scheduled tire rotation (routine, no defect/corrective keywords)
        createService(userVehicle, "Quick Lube", "Scheduled Tire Rotation", "Periodic maintenance wheel rotation", new BigDecimal("50.00"), today.minusDays(10), 20300);

        WorkshopAnalyticsReportDTO report = workshopAnalyticsService.getWorkshopDetail(testUser, "QUICK_LUBE");
        assertThat(report.reworkEventCount()).isEqualTo(0);
        assertThat(report.vendorReworkProbability()).isEqualByComparingTo("0.0");
    }

    // 19. VRP calculation boundary: 60 days eligible, 61 days excluded
    @Test
    @DisplayName("19. VRP exact 60-day boundary: day 60 triggers rework, day 61 excluded")
    void testVrpCalculation_Boundary_60DaysEligible_61DaysExcluded() {
        LocalDate baseDate = LocalDate.now().minusDays(150);

        // Workshop 1: Rework at exactly 60 days -> Rework detected
        createService(userVehicle, "Border Sixty", "Brake Pad Install", "Front pads", new BigDecimal("180.00"), baseDate, 10000);
        createService(userVehicle, "Border Sixty", "Brake Noise Repair", "Fix front brake noise leak", new BigDecimal("100.00"), baseDate.plusDays(60), 11000);

        WorkshopAnalyticsReportDTO report60 = workshopAnalyticsService.getWorkshopDetail(testUser, "BORDER_SIXTY");
        assertThat(report60.reworkEventCount()).isEqualTo(1);
        assertThat(report60.vendorReworkProbability()).isEqualByComparingTo("50.0");

        // Workshop 2: Evaluated on separate vehicle to test 61-day boundary in isolation
        Vehicle vehicle61 = new Vehicle();
        vehicle61.setUser(testUser);
        vehicle61.setCategory(testCategory);
        vehicle61.setMake("Toyota");
        vehicle61.setModel("Corolla");
        vehicle61.setYear(2023);
        vehicle61.setPlateNumber("W61-" + UUID.randomUUID().toString().substring(0, 6));
        vehicle61.setCurrentOdometer(10000);
        vehicle61 = vehicleRepository.save(vehicle61);

        createService(vehicle61, "Border SixtyOne", "Brake Pad Install", "Front pads", new BigDecimal("180.00"), baseDate, 10000);
        createService(vehicle61, "Border SixtyOne", "Brake Noise Repair", "Fix front brake noise leak", new BigDecimal("100.00"), baseDate.plusDays(61), 11000);

        WorkshopAnalyticsReportDTO report61 = workshopAnalyticsService.getWorkshopDetail(testUser, "BORDER_SIXTYONE");
        assertThat(report61.reworkEventCount()).isEqualTo(0);
        assertThat(report61.vendorReworkProbability()).isEqualByComparingTo("0.0");
    }

    // 20. VRP calculation boundary: 3000 km eligible, 3001 km excluded
    @Test
    @DisplayName("20. VRP exact 3,000 km boundary: 3,000 km eligible, 3,001 km excluded")
    void testVrpCalculation_Boundary_3000KmEligible_3001KmExcluded() {
        LocalDate dateA = LocalDate.now().minusDays(30);
        LocalDate dateB = LocalDate.now().minusDays(10); // 20 days apart

        // Workshop 1: Exactly 3000 km -> Rework
        createService(userVehicle, "Dist Exact", "Oil Change", "Engine oil", new BigDecimal("95.00"), dateA, 10000);
        createService(userVehicle, "Dist Exact", "Oil Leak Repair", "Fix engine oil leak", new BigDecimal("80.00"), dateB, 13000);

        WorkshopAnalyticsReportDTO report3000 = workshopAnalyticsService.getWorkshopDetail(testUser, "DIST_EXACT");
        assertThat(report3000.reworkEventCount()).isEqualTo(1);

        // Workshop 2: Evaluated on separate vehicle to test 3001 km boundary in isolation
        Vehicle vehicle3001 = new Vehicle();
        vehicle3001.setUser(testUser);
        vehicle3001.setCategory(testCategory);
        vehicle3001.setMake("Toyota");
        vehicle3001.setModel("RAV4");
        vehicle3001.setYear(2023);
        vehicle3001.setPlateNumber("W3K-" + UUID.randomUUID().toString().substring(0, 6));
        vehicle3001.setCurrentOdometer(10000);
        vehicle3001 = vehicleRepository.save(vehicle3001);

        createService(vehicle3001, "Dist Over", "Oil Change", "Engine oil", new BigDecimal("95.00"), dateA, 10000);
        createService(vehicle3001, "Dist Over", "Oil Leak Repair", "Fix engine oil leak", new BigDecimal("80.00"), dateB, 13001);

        WorkshopAnalyticsReportDTO report3001 = workshopAnalyticsService.getWorkshopDetail(testUser, "DIST_OVER");
        assertThat(report3001.reworkEventCount()).isEqualTo(0);
    }

    // 21. VRP calculation: Odometer rollback fallback to 60-day window
    @Test
    @DisplayName("21. VRP odometer rollback (odoB < odoA) bypasses distance check")
    void testVrpCalculation_OdometerRollbackFallback() {
        LocalDate dateA = LocalDate.now().minusDays(20);
        LocalDate dateB = LocalDate.now().minusDays(5);

        // Odometer rollback: 25000 -> 24000
        createService(userVehicle, "Rollback Auto", "Suspension Strut", "Front struts", new BigDecimal("160.00"), dateA, 25000);
        createService(userVehicle, "Rollback Auto", "Suspension Noise Repair", "Fix strut noise", new BigDecimal("90.00"), dateB, 24000);

        WorkshopAnalyticsReportDTO report = workshopAnalyticsService.getWorkshopDetail(testUser, "ROLLBACK_AUTO");
        assertThat(report.reworkEventCount()).isEqualTo(1);
        assertThat(report.reworkEvents().get(0).kmBetweenServices()).isNull();
    }

    // 22. VRP calculation: Missing odometer fallback
    @Test
    @DisplayName("22. VRP null odometer on either record falls back to 60-day temporal window")
    void testVrpCalculation_MissingOdometerFallback() {
        LocalDate dateA = LocalDate.now().minusDays(20);
        LocalDate dateB = LocalDate.now().minusDays(5);

        createService(userVehicle, "Null Odo Garage", "Brake Pad Change", "Brake work", new BigDecimal("180.00"), dateA, null);
        createService(userVehicle, "Null Odo Garage", "Brake Caliper Repair", "Fix defective caliper", new BigDecimal("150.00"), dateB, 22000);

        WorkshopAnalyticsReportDTO report = workshopAnalyticsService.getWorkshopDetail(testUser, "NULL_ODO_GARAGE");
        assertThat(report.reworkEventCount()).isEqualTo(1);
        assertThat(report.reworkEvents().get(0).kmBetweenServices()).isNull();
    }

    // 23. Tier Precedence Rule 1: Single visit assigns TIER_3_EVALUATING
    @Test
    @DisplayName("23. Rule 1: Single visit assigns TIER_3_EVALUATING even with high WPI")
    void testTierPrecedence_Rule1_SingleVisitEvaluating() {
        LocalDate today = LocalDate.now();
        // High cost oil change: 190 on 95 benchmark -> WPI = 200.0%
        createService(userVehicle, "Single Shot", "Oil Change", "Expensive oil", new BigDecimal("190.00"), today.minusDays(5), 10000);

        WorkshopAnalyticsReportDTO report = workshopAnalyticsService.getWorkshopDetail(testUser, "SINGLE_SHOT");
        assertThat(report.totalVisits()).isEqualTo(1);
        assertThat(report.valueTier()).isEqualTo("TIER_3_EVALUATING");
        assertThat(report.isPreliminaryData()).isTrue();
    }

    // 24. Tier Precedence Rule 2: High rework trumps high WPI
    @Test
    @DisplayName("24. Rule 2: VRP > 25% trumps WPI (assigns TIER_5_CAUTION_HIGH_REWORK)")
    void testTierPrecedence_Rule2_HighReworkTrumpsHighWpi() {
        LocalDate today = LocalDate.now();
        // 2 visits, 1 rework event -> VRP = 50.0%
        // WPI is also high (150.0)
        createService(userVehicle, "Dangerous Shop", "Brake Overhaul", "Pads and rotors", new BigDecimal("270.00"), today.minusDays(30), 15000);
        createService(userVehicle, "Dangerous Shop", "Brake Defect Repair", "Fix broken brake line leak", new BigDecimal("270.00"), today.minusDays(10), 15500);

        WorkshopAnalyticsReportDTO report = workshopAnalyticsService.getWorkshopDetail(testUser, "DANGEROUS_SHOP");
        assertThat(report.totalVisits()).isEqualTo(2);
        assertThat(report.vendorReworkProbability()).isEqualByComparingTo("50.0");
        assertThat(report.valueTier()).isEqualTo("TIER_5_CAUTION_HIGH_REWORK");
    }

    // 25. Tier Precedence Rule 3: Expensive tier
    @Test
    @DisplayName("25. Rule 3: WPI > 135 with low rework assigns TIER_4_CAUTION_EXPENSIVE")
    void testTierPrecedence_Rule3_ExpensiveTier() {
        LocalDate today = LocalDate.now();
        // 2 oil changes at $142.50 on $95 benchmark -> WPI = 150.0%, 0 rework
        createService(userVehicle, "Luxury Dealer", "Oil Change", "Routine synthetic oil", new BigDecimal("142.50"), today.minusDays(80), 20000);
        createService(userVehicle, "Luxury Dealer", "Oil Change", "Routine synthetic oil", new BigDecimal("142.50"), today.minusDays(10), 25000);

        WorkshopAnalyticsReportDTO report = workshopAnalyticsService.getWorkshopDetail(testUser, "LUXURY_DEALER");
        assertThat(report.workshopPriceIndex()).isEqualByComparingTo("150.0");
        assertThat(report.vendorReworkProbability()).isEqualByComparingTo("0.0");
        assertThat(report.valueTier()).isEqualTo("TIER_4_CAUTION_EXPENSIVE");
    }

    // 26. Tier Precedence Rule 4: Preferred tier
    @Test
    @DisplayName("26. Rule 4: Low rework (<=10%), WPI <= 135, WVS >= 80 assigns TIER_1_PREFERRED")
    void testTierPrecedence_Rule4_PreferredTier() {
        LocalDate today = LocalDate.now();
        // 2 visits at exact benchmark ($95 oil), 0 rework -> WPI = 100.0, VRP = 0.0%, WVS = 100.0
        createService(userVehicle, "Golden Auto", "Oil Change", "Routine oil", new BigDecimal("95.00"), today.minusDays(100), 20000);
        createService(userVehicle, "Golden Auto", "Oil Change", "Routine oil", new BigDecimal("95.00"), today.minusDays(20), 26000);

        WorkshopAnalyticsReportDTO report = workshopAnalyticsService.getWorkshopDetail(testUser, "GOLDEN_AUTO");
        assertThat(report.workshopPriceIndex()).isEqualByComparingTo("100.0");
        assertThat(report.vendorReworkProbability()).isEqualByComparingTo("0.0");
        assertThat(report.workshopValueScore()).isEqualByComparingTo("100.0");
        assertThat(report.valueTier()).isEqualTo("TIER_1_PREFERRED");
    }

    // 27. HHI calculation: Single workshop = 10000.0
    @Test
    @DisplayName("27. HHI calculation: 100% spend with single garage yields 10,000.0")
    void testHhiCalculation_SingleWorkshop_HighlyConcentrated() {
        LocalDate today = LocalDate.now();
        createService(userVehicle, "Monopoly Motors", "Routine PMS", "Annual PMS", new BigDecimal("500.00"), today.minusDays(20), 30000);

        GarageWorkshopEcosystemMatrixDTO matrix = workshopAnalyticsService.getGarageWorkshopEcosystem(testUser);
        assertThat(matrix.fleetHhiIndex()).isEqualByComparingTo("10000.0");
        assertThat(matrix.concentrationTier()).isEqualTo("HIGHLY_CONCENTRATED");
    }

    // 28. HHI calculation: Equal spend across 4 workshops = 2500.0
    @Test
    @DisplayName("28. HHI calculation: Equal spend across 4 garages yields 2,500.0")
    void testHhiCalculation_EqualSpendMultiWorkshop() {
        LocalDate today = LocalDate.now();
        createService(userVehicle, "Shop Alpha", "Oil", "Oil", new BigDecimal("100.00"), today.minusDays(40), 10000);
        createService(userVehicle, "Shop Beta", "Brakes", "Brakes", new BigDecimal("100.00"), today.minusDays(30), 11000);
        createService(userVehicle, "Shop Gamma", "Coolant", "Coolant", new BigDecimal("100.00"), today.minusDays(20), 12000);
        createService(userVehicle, "Shop Delta", "Tires", "Tires", new BigDecimal("100.00"), today.minusDays(10), 13000);

        GarageWorkshopEcosystemMatrixDTO matrix = workshopAnalyticsService.getGarageWorkshopEcosystem(testUser);
        assertThat(matrix.totalUniqueWorkshops()).isEqualTo(4);
        assertThat(matrix.totalFleetServiceSpend()).isEqualByComparingTo("400.00");
        assertThat(matrix.fleetHhiIndex()).isEqualByComparingTo("2500.0");
        assertThat(matrix.concentrationTier()).isEqualTo("MODERATELY_CONCENTRATED");
    }

    // 29. Empty fleet and non-preferred behavior
    @Test
    @DisplayName("29. Deterministic empty-fleet and no-preferred-workshop behavior")
    void testEmptyFleetAndNoPreferredWorkshopBehavior() {
        // Empty fleet test: User has no service records
        User freshUser = userRepository.findByEmail("fresh_user@garage.com").orElseGet(() -> {
            User u = new User();
            u.setEmail("fresh_user@garage.com");
            u.setPasswordHash("hash");
            u.setFullName("Fresh Tester");
            u.setRole(Role.NORMAL_USER);
            return userRepository.save(u);
        });

        GarageWorkshopEcosystemMatrixDTO emptyMatrix = workshopAnalyticsService.getGarageWorkshopEcosystem(freshUser);
        assertThat(emptyMatrix.totalUniqueWorkshops()).isEqualTo(0);
        assertThat(emptyMatrix.totalFleetServiceSpend()).isEqualByComparingTo("0.00");
        assertThat(emptyMatrix.topPreferredWorkshopName()).isEqualTo("None (No Workshop History)");
        assertThat(emptyMatrix.fleetHhiIndex()).isEqualByComparingTo("0.0");
        assertThat(emptyMatrix.concentrationTier()).isEqualTo("HIGHLY_FRAGMENTED");

        // Non-empty fleet with only TIER_3_EVALUATING (single visit)
        LocalDate today = LocalDate.now();
        createService(userVehicle, "Solo Shop", "Oil", "Oil change", new BigDecimal("95.00"), today.minusDays(5), 20000);
        GarageWorkshopEcosystemMatrixDTO noPrefMatrix = workshopAnalyticsService.getGarageWorkshopEcosystem(testUser);
        assertThat(noPrefMatrix.topPreferredWorkshopName()).isEqualTo("None (No Tier-1 Preferred Workshop Qualified)");
    }

    // 30. Deterministic multi-stage tie breaking
    @Test
    @DisplayName("30. Multi-stage tie breaking sorts Tier -> WVS -> Visits -> Spend -> Key")
    void testMultiStageTieBreaking_DeterministicSorting() {
        LocalDate today = LocalDate.now();
        // Shop 1: Preferred, WVS 100.0
        createService(userVehicle, "AAA Preferred", "Oil", "Routine oil", new BigDecimal("95.00"), today.minusDays(50), 10000);
        createService(userVehicle, "AAA Preferred", "Oil", "Routine oil", new BigDecimal("95.00"), today.minusDays(20), 15000);

        // Shop 2: Approved, WVS 90.0
        createService(userVehicle, "BBB Approved", "Brakes", "Pads", new BigDecimal("200.00"), today.minusDays(40), 12000);
        createService(userVehicle, "BBB Approved", "Brakes", "Pads", new BigDecimal("200.00"), today.minusDays(10), 16000);

        GarageWorkshopEcosystemMatrixDTO matrix = workshopAnalyticsService.getGarageWorkshopEcosystem(testUser);
        assertThat(matrix.rankedWorkshops().get(0).workshopName()).isEqualTo("Aaa Preferred");
        assertThat(matrix.rankedWorkshops().get(1).workshopName()).isEqualTo("Bbb Approved");
    }

    // 31. Security: Owner can access REST and Web
    @Test
    @WithMockUser(username = "workshop_user@garage.com", roles = "NORMAL_USER")
    @DisplayName("31. Owner receives 200 OK on REST API and Web endpoints")
    void testSecurity_OwnerCanAccessRestAndWeb() throws Exception {
        LocalDate today = LocalDate.now();
        createService(userVehicle, "City Auto", "Oil", "Routine", new BigDecimal("95.00"), today.minusDays(10), 20000);

        mockMvc.perform(get("/api/analytics/workshops"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUniqueWorkshops").value(1));

        mockMvc.perform(get("/api/analytics/workshops/CITY_AUTO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workshopKey").value("CITY_AUTO"));

        mockMvc.perform(get("/workshops"))
                .andExpect(status().isOk())
                .andExpect(view().name("workshop/ecosystem"));

        mockMvc.perform(get("/workshops/CITY_AUTO"))
                .andExpect(status().isOk())
                .andExpect(view().name("workshop/detail"));
    }

    // 32. Security: Exact REST status codes (401, 403, 404) & Admin blocking
    @Test
    @DisplayName("32. Security verifies 401 unauthenticated, 403 cross-user/admin, 404 nonexistent")
    void testSecurity_RestExactStatusCodes_401_403_404() throws Exception {
        LocalDate today = LocalDate.now();
        // Create a service for otherUser
        createService(otherVehicle, "Other Guy Garage", "Oil", "Oil", new BigDecimal("95.00"), today.minusDays(10), 20000);

        // 1. Unauthenticated -> blocked by security (redirect to login or unauthorized)
        mockMvc.perform(get("/api/analytics/workshops"))
                .andExpect(status().is3xxRedirection());

        // 2. Admin access -> 403 Forbidden
        assertThatThrownBy(() -> workshopAnalyticsService.getGarageWorkshopEcosystem(adminUser))
                .isInstanceOf(AccessDeniedException.class);

        // 3. Cross-user access: testUser attempts to fetch OTHER_GUY_GARAGE -> 403 Forbidden
        assertThatThrownBy(() -> workshopAnalyticsService.getWorkshopDetail(testUser, "OTHER_GUY_GARAGE"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Access denied");

        // 4. Nonexistent workshop key -> ResourceNotFoundException (maps to 404 in REST API)
        assertThatThrownBy(() -> workshopAnalyticsService.getWorkshopDetail(testUser, "COMPLETELY_NONEXISTENT_KEY"))
                .isInstanceOf(com.mygarage.exception.ResourceNotFoundException.class)
                .hasMessageContaining("Workshop not found");
    }
}
