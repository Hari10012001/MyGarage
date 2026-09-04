package com.mygarage.tco;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mygarage.dto.response.GarageTcoSummaryDTO;
import com.mygarage.dto.response.VehicleTcoReportDTO;
import com.mygarage.model.*;
import com.mygarage.model.enums.FuelType;
import com.mygarage.model.enums.MaintenanceStatus;
import com.mygarage.model.enums.Role;
import com.mygarage.repository.*;
import com.mygarage.service.VehicleTcoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Milestone 15 (M15) — Total Cost of Ownership (TCO) Lifecycle Modeling, Depreciation Valuation
 * & Economic Replacement Advisory Module Test Suite.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class VehicleTcoLifecycleModuleTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VehicleCategoryRepository categoryRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private ServiceRecordRepository serviceRecordRepository;

    @Autowired
    private FuelRecordRepository fuelRecordRepository;

    @Autowired
    private MaintenanceRecordRepository maintenanceRecordRepository;

    @Autowired
    private VehicleTcoService vehicleTcoService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private User userA;
    private User userB;
    private User adminUser;
    private VehicleCategory sedanCategory;
    private VehicleCategory suvCategory;
    private VehicleCategory bikeCategory;
    private Vehicle vehicleA1; // Sedan
    private Vehicle vehicleA2; // Bike
    private Vehicle vehicleB1; // SUV

    @BeforeEach
    void setUp() {
        maintenanceRecordRepository.deleteAll();
        fuelRecordRepository.deleteAll();
        serviceRecordRepository.deleteAll();
        vehicleRepository.deleteAll();
        userRepository.deleteAll();
        categoryRepository.deleteAll();

        // 1. Categories
        sedanCategory = categoryRepository.save(new VehicleCategory(null, "Sedan", "🚗", "Sedan Cars", null));
        suvCategory = categoryRepository.save(new VehicleCategory(null, "SUV", "🚙", "Sport Utility Vehicles", null));
        bikeCategory = categoryRepository.save(new VehicleCategory(null, "Motorcycle", "🏍️", "Motorcycles", null));

        // 2. Users
        userA = new User();
        userA.setEmail("m15_user_a@test.com");
        userA.setPasswordHash(passwordEncoder.encode("Password@123"));
        userA.setFullName("M15 User Alpha");
        userA.setRole(Role.NORMAL_USER);
        userA.setActive(true);
        userA = userRepository.save(userA);

        userB = new User();
        userB.setEmail("m15_user_b@test.com");
        userB.setPasswordHash(passwordEncoder.encode("Password@123"));
        userB.setFullName("M15 User Beta");
        userB.setRole(Role.NORMAL_USER);
        userB.setActive(true);
        userB = userRepository.save(userB);

        adminUser = new User();
        adminUser.setEmail("m15_admin@test.com");
        adminUser.setPasswordHash(passwordEncoder.encode("Admin@123"));
        adminUser.setFullName("M15 Admin");
        adminUser.setRole(Role.ADMIN);
        adminUser.setActive(true);
        adminUser = userRepository.save(adminUser);

        // 3. Vehicles
        int currentYear = LocalDate.now().getYear();

        // Vehicle A1: 2-year-old Sedan, 30,000 km
        vehicleA1 = new Vehicle();
        vehicleA1.setUser(userA);
        vehicleA1.setCategory(sedanCategory);
        vehicleA1.setMake("Honda");
        vehicleA1.setModel("City");
        vehicleA1.setYear(currentYear - 2);
        vehicleA1.setColor("White");
        vehicleA1.setPlateNumber("TN15A1001");
        vehicleA1.setFuelType("PETROL");
        vehicleA1.setCurrentOdometer(30000);
        vehicleA1 = vehicleRepository.save(vehicleA1);

        // Vehicle A2: 1-year-old Bike, 5,000 km
        vehicleA2 = new Vehicle();
        vehicleA2.setUser(userA);
        vehicleA2.setCategory(bikeCategory);
        vehicleA2.setMake("Yamaha");
        vehicleA2.setModel("R15");
        vehicleA2.setYear(currentYear - 1);
        vehicleA2.setColor("Blue");
        vehicleA2.setPlateNumber("TN15A1002");
        vehicleA2.setFuelType("PETROL");
        vehicleA2.setCurrentOdometer(5000);
        vehicleA2 = vehicleRepository.save(vehicleA2);

        // Vehicle B1: 3-year-old SUV, 45,000 km (User B)
        vehicleB1 = new Vehicle();
        vehicleB1.setUser(userB);
        vehicleB1.setCategory(suvCategory);
        vehicleB1.setMake("Hyundai");
        vehicleB1.setModel("Creta");
        vehicleB1.setYear(currentYear - 3);
        vehicleB1.setColor("Black");
        vehicleB1.setPlateNumber("TN15B2001");
        vehicleB1.setFuelType("DIESEL");
        vehicleB1.setCurrentOdometer(45000);
        vehicleB1 = vehicleRepository.save(vehicleB1);
    }

    // ==========================================
    // 1. Service Layer Tests: Formulas, Ratios & Safeguards
    // ==========================================

    @Test
    @DisplayName("1. Happy Path: Calculate Vehicle TCO, OPEX, Run-Rate & Residual Equity")
    void testCalculateVehicleTco_HappyPath() {
        // Seed service: 5000
        ServiceRecord s1 = new ServiceRecord();
        s1.setVehicle(vehicleA1);
        s1.setServiceDate(LocalDate.now().minusMonths(3));
        s1.setServiceType("Major Service");
        s1.setCost(new BigDecimal("5000.00"));
        s1.setOdometerAtService(25000);
        serviceRecordRepository.save(s1);

        // Seed fuel: 2000 (20 L)
        FuelRecord f1 = new FuelRecord();
        f1.setVehicle(vehicleA1);
        f1.setFuelDate(LocalDate.now().minusMonths(2));
        f1.setFuelType(FuelType.PETROL);
        f1.setQuantityLitres(new BigDecimal("20.00"));
        f1.setCostPerLitre(new BigDecimal("100.00"));
        f1.setTotalCost(new BigDecimal("2000.00"));
        f1.setOdometerAtFill(26000);
        fuelRecordRepository.save(f1);

        // Seed maintenance: 3000 completed
        MaintenanceRecord m1 = new MaintenanceRecord();
        m1.setVehicle(vehicleA1);
        m1.setTitle("Brake Pad Replacement");
        m1.setScheduledDate(LocalDate.now().minusMonths(1));
        m1.setCompletedDate(LocalDate.now().minusMonths(1));
        m1.setStatus(MaintenanceStatus.COMPLETED);
        m1.setCost(new BigDecimal("3000.00"));
        maintenanceRecordRepository.save(m1);

        VehicleTcoReportDTO report = vehicleTcoService.calculateVehicleTco(vehicleA1.getVehicleId(), userA.getUserId());

        assertNotNull(report);
        assertEquals(vehicleA1.getVehicleId(), report.getVehicleId());
        assertEquals("TN15A1001", report.getPlateNumber());
        assertEquals(new BigDecimal("5000.00"), report.getServiceCost());
        assertEquals(new BigDecimal("2000.00"), report.getFuelCost());
        assertEquals(new BigDecimal("3000.00"), report.getMaintenanceCost());
        assertEquals(new BigDecimal("10000.00"), report.getTotalLifetimeOpex());

        // 2 years old -> Annualized = 5000.00, Monthly = 416.67
        assertEquals(new BigDecimal("5000.00"), report.getAnnualizedOperatingCost());
        assertEquals(new BigDecimal("416.67"), report.getMonthlyOperatingCost());

        // 30,000 km -> Cost/km = 10000 / 30000 = 0.33
        assertEquals(new BigDecimal("0.33"), report.getOperatingCostPerKm());

        // Shares
        assertEquals(50.0, report.getServiceSharePercentage(), 0.01);
        assertEquals(20.0, report.getFuelSharePercentage(), 0.01);
        assertEquals(30.0, report.getMaintenanceSharePercentage(), 0.01);

        // Sedan benchmark = 15,00,000
        assertEquals(new BigDecimal("1500000.00"), report.getEstimatedInitialBenchmarkValue());
        assertTrue(report.getEstimatedResidualValue().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @DisplayName("2. Boundary: Brand New Vehicle with 0 Logs Handled Gracefully")
    void testZeroExpenseVehicle_Boundary() {
        VehicleTcoReportDTO report = vehicleTcoService.calculateVehicleTco(vehicleA2.getVehicleId(), userA.getUserId());

        assertNotNull(report);
        assertEquals(BigDecimal.ZERO.setScale(2), report.getTotalLifetimeOpex());
        assertEquals(BigDecimal.ZERO.setScale(2), report.getAnnualizedOperatingCost());
        assertEquals(BigDecimal.ZERO.setScale(2), report.getMonthlyOperatingCost());
        assertEquals(BigDecimal.ZERO.setScale(2), report.getOperatingCostPerKm());
        assertEquals(0.0, report.getServiceSharePercentage());
        assertEquals(0.0, report.getFuelSharePercentage());
        assertEquals(0.0, report.getMaintenanceSharePercentage());
        assertEquals(0.0, report.getRepairToResidualValueRatio());
        assertEquals("HEALTHY_RETENTION", report.getAdvisoryStatus());
    }

    @Test
    @DisplayName("3. Boundary: Zero Odometer Vehicle does not throw Division by Zero")
    void testZeroOdometerVehicle_Boundary() {
        vehicleA2.setCurrentOdometer(0);
        vehicleRepository.save(vehicleA2);

        ServiceRecord s1 = new ServiceRecord();
        s1.setVehicle(vehicleA2);
        s1.setServiceDate(LocalDate.now());
        s1.setServiceType("PDI Inspection");
        s1.setCost(new BigDecimal("1200.00"));
        s1.setOdometerAtService(0);
        serviceRecordRepository.save(s1);

        VehicleTcoReportDTO report = vehicleTcoService.calculateVehicleTco(vehicleA2.getVehicleId(), userA.getUserId());

        assertNotNull(report);
        assertEquals(BigDecimal.ZERO.setScale(2), report.getOperatingCostPerKm());
        assertEquals(BigDecimal.ZERO.setScale(2), report.getCarbonIntensityGramsPerKm());
    }

    @Test
    @DisplayName("4. Boundary: Vintage Vehicle Depreciation respects 10% Salvage Floor")
    void testVintageVehicleDepreciation_Flooring() {
        Vehicle vintage = new Vehicle();
        vintage.setUser(userA);
        vintage.setCategory(sedanCategory);
        vintage.setMake("Maruti");
        vintage.setModel("800");
        vintage.setYear(1975);
        vintage.setColor("White");
        vintage.setPlateNumber("TN15V1975");
        vintage.setFuelType("PETROL");
        vintage.setCurrentOdometer(200000);
        vintage = vehicleRepository.save(vintage);

        VehicleTcoReportDTO report = vehicleTcoService.calculateVehicleTco(vintage.getVehicleId(), userA.getUserId());

        assertNotNull(report);
        // Base = 15,00,000 -> 10% floor = 1,50,000
        assertEquals(new BigDecimal("150000.00"), report.getEstimatedResidualValue());
        assertEquals(90.0, report.getDepreciationPercentage(), 0.1);
    }

    @Test
    @DisplayName("5. Valuation: High Annual Mileage (>20,000 km/yr) Accelerates Depreciation")
    void testHighMileageDepreciationAcceleration() {
        // Vehicle with standard mileage: 2 yrs, 30,000 km (15k/yr)
        Vehicle standard = vehicleA1;

        // Vehicle with extreme mileage: 2 yrs, 70,000 km (35k/yr)
        Vehicle heavyMileage = new Vehicle();
        heavyMileage.setUser(userA);
        heavyMileage.setCategory(sedanCategory);
        heavyMileage.setMake("Honda");
        heavyMileage.setModel("City");
        heavyMileage.setYear(LocalDate.now().getYear() - 2);
        heavyMileage.setColor("Silver");
        heavyMileage.setPlateNumber("TN15H7000");
        heavyMileage.setFuelType("PETROL");
        heavyMileage.setCurrentOdometer(70000);
        heavyMileage = vehicleRepository.save(heavyMileage);

        VehicleTcoReportDTO reportStandard = vehicleTcoService.calculateVehicleTco(standard.getVehicleId(), userA.getUserId());
        VehicleTcoReportDTO reportHeavy = vehicleTcoService.calculateVehicleTco(heavyMileage.getVehicleId(), userA.getUserId());

        assertTrue(reportHeavy.getEstimatedResidualValue().compareTo(reportStandard.getEstimatedResidualValue()) < 0,
                "Heavy mileage vehicle must have lower residual value due to accelerated mileage depreciation");
    }

    @Test
    @DisplayName("6. Trailing 12-Month Filtering: Old records (>365 days) excluded from T12M burden")
    void testTrailing12MonthMaintenanceFiltering() {
        // Old service from 450 days ago: 8000
        ServiceRecord oldS = new ServiceRecord();
        oldS.setVehicle(vehicleA1);
        oldS.setServiceDate(LocalDate.now().minusDays(450));
        oldS.setServiceType("Engine Overhaul");
        oldS.setCost(new BigDecimal("8000.00"));
        oldS.setOdometerAtService(10000);
        serviceRecordRepository.save(oldS);

        // Recent service from 60 days ago: 2500
        ServiceRecord recentS = new ServiceRecord();
        recentS.setVehicle(vehicleA1);
        recentS.setServiceDate(LocalDate.now().minusDays(60));
        recentS.setServiceType("Minor Service");
        recentS.setCost(new BigDecimal("2500.00"));
        recentS.setOdometerAtService(28000);
        serviceRecordRepository.save(recentS);

        VehicleTcoReportDTO report = vehicleTcoService.calculateVehicleTco(vehicleA1.getVehicleId(), userA.getUserId());

        // Total lifetime service cost = 10500.00
        assertEquals(new BigDecimal("10500.00"), report.getServiceCost());
        // Trailing 12-Month cost = only 2500.00
        assertEquals(new BigDecimal("2500.00"), report.getTrailing12MonthsMaintenanceCost());
    }

    @Test
    @DisplayName("7. RRVR Advisory: RRVR < 15% yields HEALTHY_RETENTION")
    void testRepairToValueRatio_HealthyRetention() {
        // Residual value of sedan is ~11.47 Lakhs. Repair of 5,000 is < 1%
        ServiceRecord s = new ServiceRecord();
        s.setVehicle(vehicleA1);
        s.setServiceDate(LocalDate.now().minusDays(30));
        s.setServiceType("Oil Change");
        s.setCost(new BigDecimal("5000.00"));
        s.setOdometerAtService(29000);
        serviceRecordRepository.save(s);

        VehicleTcoReportDTO report = vehicleTcoService.calculateVehicleTco(vehicleA1.getVehicleId(), userA.getUserId());

        assertTrue(report.getRepairToResidualValueRatio() < 15.0);
        assertEquals("HEALTHY_RETENTION", report.getAdvisoryStatus());
        assertFalse(report.getKeyRecommendationPoints().isEmpty());
    }

    @Test
    @DisplayName("8. RRVR Advisory: 15% <= RRVR < 30% yields MODERATE_EXPENSE")
    void testRepairToValueRatio_ModerateExpense() {
        // Bike base = 2,00,000; 1 yr old residual = ~1,70,000.
        // 20% of 1,70,000 = ~34,000
        ServiceRecord s = new ServiceRecord();
        s.setVehicle(vehicleA2);
        s.setServiceDate(LocalDate.now().minusDays(20));
        s.setServiceType("Major Transmission Service");
        s.setCost(new BigDecimal("35000.00"));
        s.setOdometerAtService(4500);
        serviceRecordRepository.save(s);

        VehicleTcoReportDTO report = vehicleTcoService.calculateVehicleTco(vehicleA2.getVehicleId(), userA.getUserId());

        assertTrue(report.getRepairToResidualValueRatio() >= 15.0 && report.getRepairToResidualValueRatio() < 30.0);
        assertEquals("MODERATE_EXPENSE", report.getAdvisoryStatus());
    }

    @Test
    @DisplayName("9. RRVR Advisory: 30% <= RRVR < 50% yields REPLACEMENT_WATCHLIST")
    void testRepairToValueRatio_ReplacementWatchlist() {
        // Bike residual = ~1,70,000. 40% of 1,70,000 = ~68,000
        ServiceRecord s = new ServiceRecord();
        s.setVehicle(vehicleA2);
        s.setServiceDate(LocalDate.now().minusDays(20));
        s.setServiceType("Engine & Chassis Rebuild");
        s.setCost(new BigDecimal("68000.00"));
        s.setOdometerAtService(4500);
        serviceRecordRepository.save(s);

        VehicleTcoReportDTO report = vehicleTcoService.calculateVehicleTco(vehicleA2.getVehicleId(), userA.getUserId());

        assertTrue(report.getRepairToResidualValueRatio() >= 30.0 && report.getRepairToResidualValueRatio() < 50.0);
        assertEquals("REPLACEMENT_WATCHLIST", report.getAdvisoryStatus());
    }

    @Test
    @DisplayName("10. RRVR Advisory: RRVR >= 50% yields DISPOSAL_RECOMMENDED")
    void testRepairToValueRatio_DisposalRecommended() {
        // Bike residual = ~1,70,000. Repairs = 1,00,000 (> 58%)
        ServiceRecord s = new ServiceRecord();
        s.setVehicle(vehicleA2);
        s.setServiceDate(LocalDate.now().minusDays(10));
        s.setServiceType("Catastrophic Engine & Electrical Failure");
        s.setCost(new BigDecimal("100000.00"));
        s.setOdometerAtService(4800);
        serviceRecordRepository.save(s);

        VehicleTcoReportDTO report = vehicleTcoService.calculateVehicleTco(vehicleA2.getVehicleId(), userA.getUserId());

        assertTrue(report.getRepairToResidualValueRatio() >= 50.0);
        assertEquals("DISPOSAL_RECOMMENDED", report.getAdvisoryStatus());
    }

    @Test
    @DisplayName("11. Direct Tailpipe Carbon: Petrol factor 2.31 kg/L")
    void testCarbonFootprint_Petrol() {
        FuelRecord f = new FuelRecord();
        f.setVehicle(vehicleA1);
        f.setFuelDate(LocalDate.now().minusDays(5));
        f.setFuelType(FuelType.PETROL);
        f.setQuantityLitres(new BigDecimal("100.00"));
        f.setCostPerLitre(new BigDecimal("100.00"));
        f.setTotalCost(new BigDecimal("10000.00"));
        f.setOdometerAtFill(30000);
        fuelRecordRepository.save(f);

        VehicleTcoReportDTO report = vehicleTcoService.calculateVehicleTco(vehicleA1.getVehicleId(), userA.getUserId());

        // 100 * 2.31 = 231.00 kg = 0.231 tonnes
        assertEquals(new BigDecimal("231.00"), report.getTotalDirectCarbonEmissionsKg());
        assertEquals(new BigDecimal("0.231"), report.getTotalDirectCarbonEmissionsTonnes());
    }

    @Test
    @DisplayName("12. Direct Tailpipe Carbon: Diesel factor 2.68 kg/L")
    void testCarbonFootprint_Diesel() {
        FuelRecord f = new FuelRecord();
        f.setVehicle(vehicleB1);
        f.setFuelDate(LocalDate.now().minusDays(5));
        f.setFuelType(FuelType.DIESEL);
        f.setQuantityLitres(new BigDecimal("50.00"));
        f.setCostPerLitre(new BigDecimal("90.00"));
        f.setTotalCost(new BigDecimal("4500.00"));
        f.setOdometerAtFill(45000);
        fuelRecordRepository.save(f);

        VehicleTcoReportDTO report = vehicleTcoService.calculateVehicleTco(vehicleB1.getVehicleId(), userB.getUserId());

        // 50 * 2.68 = 134.00 kg
        assertEquals(new BigDecimal("134.00"), report.getTotalDirectCarbonEmissionsKg());
        assertEquals(new BigDecimal("0.134"), report.getTotalDirectCarbonEmissionsTonnes());
    }

    @Test
    @DisplayName("13. Direct Tailpipe Carbon: Electric EV returns 0.00 emissions & ECO_EXCELLENT")
    void testCarbonFootprint_EV() {
        Vehicle evVehicle = new Vehicle();
        evVehicle.setUser(userA);
        evVehicle.setCategory(suvCategory);
        evVehicle.setMake("Tata");
        evVehicle.setModel("Nexon EV");
        evVehicle.setYear(LocalDate.now().getYear() - 1);
        evVehicle.setColor("Teal");
        evVehicle.setPlateNumber("TN15EV001");
        evVehicle.setFuelType("ELECTRIC");
        evVehicle.setCurrentOdometer(12000);
        evVehicle = vehicleRepository.save(evVehicle);

        VehicleTcoReportDTO report = vehicleTcoService.calculateVehicleTco(evVehicle.getVehicleId(), userA.getUserId());

        assertEquals(new BigDecimal("0.00"), report.getTotalDirectCarbonEmissionsKg());
        assertEquals(new BigDecimal("0.000"), report.getTotalDirectCarbonEmissionsTonnes());
        assertEquals(new BigDecimal("0.00"), report.getCarbonIntensityGramsPerKm());
        assertEquals("ECO_EXCELLENT", report.getEcoTailpipeRating());
    }

    // ==========================================
    // 2. Web MVC Controller Tests
    // ==========================================

    @Test
    @DisplayName("14. Web MVC: Owner can access /vehicles/{id}/tco (HTTP 200 OK)")
    @WithMockUser(username = "m15_user_a@test.com", roles = {"NORMAL_USER"})
    void testWebMvc_TcoView_Owner_Success() throws Exception {
        mockMvc.perform(get("/vehicles/{id}/tco", vehicleA1.getVehicleId()))
                .andExpect(status().isOk())
                .andExpect(view().name("vehicle/tco"))
                .andExpect(model().attributeExists("report"))
                .andExpect(model().attributeExists("vehicle"))
                .andExpect(model().attributeExists("userVehicles"));
    }

    @Test
    @DisplayName("15. Web MVC: Cross-User Tampering redirects to /vehicles with flash error")
    @WithMockUser(username = "m15_user_a@test.com", roles = {"NORMAL_USER"})
    void testWebMvc_TcoView_CrossUser_Redirect() throws Exception {
        // User A tries to view Vehicle B1
        mockMvc.perform(get("/vehicles/{id}/tco", vehicleB1.getVehicleId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/vehicles"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    @DisplayName("16. Web MVC: ADMIN is blocked from /vehicles/{id}/tco with HTTP 403 Forbidden")
    @WithMockUser(username = "m15_admin@test.com", roles = {"ADMIN"})
    void testWebMvc_TcoView_Admin_Forbidden() throws Exception {
        mockMvc.perform(get("/vehicles/{id}/tco", vehicleA1.getVehicleId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("17. Web MVC: Unauthenticated user is redirected to /login (HTTP 302)")
    void testWebMvc_TcoView_Unauthenticated_Redirect() throws Exception {
        mockMvc.perform(get("/vehicles/{id}/tco", vehicleA1.getVehicleId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("18. Web MVC: Dispatcher /vehicles/tco redirects to first owned vehicle")
    @WithMockUser(username = "m15_user_a@test.com", roles = {"NORMAL_USER"})
    void testWebMvc_TcoDispatcher_RedirectToFirst() throws Exception {
        Long expectedFirstId = vehicleRepository.findByUserUserIdOrderByCreatedAtDesc(userA.getUserId()).get(0).getVehicleId();
        mockMvc.perform(get("/vehicles/tco"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/vehicles/" + expectedFirstId + "/tco"));
    }

    @Test
    @DisplayName("19. Web MVC: Dispatcher with 0 vehicles redirects with warning message")
    @WithMockUser(username = "m15_user_zero@test.com", roles = {"NORMAL_USER"})
    void testWebMvc_TcoDispatcher_NoVehicles() throws Exception {
        User zeroUser = new User();
        zeroUser.setEmail("m15_user_zero@test.com");
        zeroUser.setPasswordHash(passwordEncoder.encode("Password@123"));
        zeroUser.setFullName("M15 Zero User");
        zeroUser.setRole(Role.NORMAL_USER);
        zeroUser.setActive(true);
        userRepository.save(zeroUser);

        mockMvc.perform(get("/vehicles/tco"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/vehicles"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    // ==========================================
    // 3. REST API Controller Tests
    // ==========================================

    @Test
    @DisplayName("20. REST API: Owner gets HTTP 200 OK from /api/vehicles/{id}/tco")
    @WithMockUser(username = "m15_user_a@test.com", roles = {"NORMAL_USER"})
    void testRestApi_Tco_Owner_Success() throws Exception {
        mockMvc.perform(get("/api/vehicles/{id}/tco", vehicleA1.getVehicleId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicleId", is(vehicleA1.getVehicleId().intValue())))
                .andExpect(jsonPath("$.plateNumber", is("TN15A1001")))
                .andExpect(jsonPath("$.advisoryStatus", notNullValue()))
                .andExpect(jsonPath("$.estimatedResidualValue", notNullValue()))
                .andExpect(jsonPath("$.totalLifetimeOpex", notNullValue()));
    }

    @Test
    @DisplayName("21. REST API: Cross-User Tampering returns HTTP 403 Forbidden")
    @WithMockUser(username = "m15_user_a@test.com", roles = {"NORMAL_USER"})
    void testRestApi_Tco_CrossUser_Forbidden() throws Exception {
        // User A attempts to query Vehicle B1 via REST API
        mockMvc.perform(get("/api/vehicles/{id}/tco", vehicleB1.getVehicleId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("22. REST API: ADMIN is blocked from /api/vehicles/{id}/tco with HTTP 403 Forbidden")
    @WithMockUser(username = "m15_admin@test.com", roles = {"ADMIN"})
    void testRestApi_Tco_Admin_Forbidden() throws Exception {
        mockMvc.perform(get("/api/vehicles/{id}/tco", vehicleA1.getVehicleId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("23. REST API: Non-existent vehicle returns HTTP 400 Bad Request")
    @WithMockUser(username = "m15_user_a@test.com", roles = {"NORMAL_USER"})
    void testRestApi_Tco_NotFound() throws Exception {
        mockMvc.perform(get("/api/vehicles/999999/tco")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("24. REST API: Owner gets HTTP 200 OK from /api/analytics/garage-tco")
    @WithMockUser(username = "m15_user_a@test.com", roles = {"NORMAL_USER"})
    void testRestApi_GarageTco_Owner_Success() throws Exception {
        mockMvc.perform(get("/api/analytics/garage-tco")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVehicles", is(2)))
                .andExpect(jsonPath("$.totalFleetResidualValue", notNullValue()))
                .andExpect(jsonPath("$.vehicles", hasSize(2)));
    }

    @Test
    @DisplayName("25. REST API: ADMIN is blocked from /api/analytics/garage-tco with HTTP 403 Forbidden")
    @WithMockUser(username = "m15_admin@test.com", roles = {"ADMIN"})
    void testRestApi_GarageTco_Admin_Forbidden() throws Exception {
        mockMvc.perform(get("/api/analytics/garage-tco")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }
}
