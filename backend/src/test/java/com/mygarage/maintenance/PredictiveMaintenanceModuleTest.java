package com.mygarage.maintenance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mygarage.dto.request.ScheduleForecastRequestDTO;
import com.mygarage.dto.response.ExpenseForecastDTO;
import com.mygarage.dto.response.PredictiveMaintenanceMilestoneDTO;
import com.mygarage.dto.response.VehiclePredictiveReportDTO;
import com.mygarage.model.*;
import com.mygarage.model.enums.FuelType;
import com.mygarage.model.enums.MaintenanceStatus;
import com.mygarage.model.enums.Role;
import com.mygarage.repository.*;
import com.mygarage.service.PredictiveMaintenanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Milestone 14 (M14) — Predictive Maintenance Forecasting, Vehicle Health Scoring & Smart Service Planner Test Suite.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PredictiveMaintenanceModuleTest {

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
    private PredictiveMaintenanceService predictiveMaintenanceService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private User userA;
    private User userB;
    private User adminUser;
    private VehicleCategory carCategory;
    private VehicleCategory bikeCategory;
    private Vehicle vehicleA1; // Honda City (User A)
    private Vehicle vehicleA2; // Yamaha R15 (User A)
    private Vehicle vehicleB1; // Hyundai Creta (User B)

    @BeforeEach
    void setUp() {
        maintenanceRecordRepository.deleteAll();
        fuelRecordRepository.deleteAll();
        serviceRecordRepository.deleteAll();
        vehicleRepository.deleteAll();
        userRepository.deleteAll();
        categoryRepository.deleteAll();

        // 1. Seed Categories
        carCategory = categoryRepository.save(new VehicleCategory(null, "Sedan", "🚗", "Sedan Cars", null));
        bikeCategory = categoryRepository.save(new VehicleCategory(null, "Motorcycle", "🏍️", "Two Wheeler Bikes", null));

        // 2. Seed Users
        userA = new User();
        userA.setEmail("m14_user_a@test.com");
        userA.setPasswordHash(passwordEncoder.encode("Password@123"));
        userA.setFullName("M14 Normal User A");
        userA.setPhone("9876543210");
        userA.setRole(Role.NORMAL_USER);
        userA.setActive(true);
        userA = userRepository.save(userA);

        userB = new User();
        userB.setEmail("m14_user_b@test.com");
        userB.setPasswordHash(passwordEncoder.encode("Password@123"));
        userB.setFullName("M14 Normal User B");
        userB.setPhone("9876543211");
        userB.setRole(Role.NORMAL_USER);
        userB.setActive(true);
        userB = userRepository.save(userB);

        adminUser = new User();
        adminUser.setEmail("m14_admin@mygarage.com");
        adminUser.setPasswordHash(passwordEncoder.encode("Admin@123"));
        adminUser.setFullName("M14 System Admin");
        adminUser.setRole(Role.ADMIN);
        adminUser.setActive(true);
        adminUser = userRepository.save(adminUser);

        // 3. Seed Vehicles for User A
        vehicleA1 = new Vehicle();
        vehicleA1.setUser(userA);
        vehicleA1.setCategory(carCategory);
        vehicleA1.setMake("Honda");
        vehicleA1.setModel("City");
        vehicleA1.setYear(2022);
        vehicleA1.setColor("Silver");
        vehicleA1.setPlateNumber("TN14AA1111");
        vehicleA1.setFuelType("PETROL");
        vehicleA1.setCurrentOdometer(23500);
        vehicleA1 = vehicleRepository.save(vehicleA1);

        vehicleA2 = new Vehicle();
        vehicleA2.setUser(userA);
        vehicleA2.setCategory(bikeCategory);
        vehicleA2.setMake("Yamaha");
        vehicleA2.setModel("R15");
        vehicleA2.setYear(2023);
        vehicleA2.setColor("Blue");
        vehicleA2.setPlateNumber("TN14AA2222");
        vehicleA2.setFuelType("PETROL");
        vehicleA2.setCurrentOdometer(8000);
        vehicleA2 = vehicleRepository.save(vehicleA2);

        // Seed Vehicle for User B (Isolation testing)
        vehicleB1 = new Vehicle();
        vehicleB1.setUser(userB);
        vehicleB1.setCategory(carCategory);
        vehicleB1.setMake("Hyundai");
        vehicleB1.setModel("Creta");
        vehicleB1.setYear(2021);
        vehicleB1.setColor("White");
        vehicleB1.setPlateNumber("TN14BB9999");
        vehicleB1.setFuelType("DIESEL");
        vehicleB1.setCurrentOdometer(45000);
        vehicleB1 = vehicleRepository.save(vehicleB1);

        // Seed Chronological Odometer History for Vehicle A1 (Honda City)
        // Service 1: 60 days ago at 20,000 km
        ServiceRecord s1 = new ServiceRecord();
        s1.setVehicle(vehicleA1);
        s1.setServiceDate(LocalDate.now().minusDays(60));
        s1.setServiceType("General Service");
        s1.setCost(new BigDecimal("4000.00"));
        s1.setOdometerAtService(20000);
        serviceRecordRepository.save(s1);

        // Fuel 1: 40 days ago at 21,000 km (1000 km in 20 days = 50 km/day)
        FuelRecord f1 = new FuelRecord();
        f1.setVehicle(vehicleA1);
        f1.setFuelDate(LocalDate.now().minusDays(40));
        f1.setFuelType(FuelType.PETROL);
        f1.setQuantityLitres(new BigDecimal("35.00"));
        f1.setCostPerLitre(new BigDecimal("100.00"));
        f1.setTotalCost(new BigDecimal("3500.00"));
        f1.setOdometerAtFill(21000);
        fuelRecordRepository.save(f1);

        // Fuel 2: 10 days ago at 22,500 km (1500 km in 30 days = 50 km/day)
        FuelRecord f2 = new FuelRecord();
        f2.setVehicle(vehicleA1);
        f2.setFuelDate(LocalDate.now().minusDays(10));
        f2.setFuelType(FuelType.PETROL);
        f2.setQuantityLitres(new BigDecimal("40.00"));
        f2.setCostPerLitre(new BigDecimal("100.00"));
        f2.setTotalCost(new BigDecimal("4000.00"));
        f2.setOdometerAtFill(22500);
        fuelRecordRepository.save(f2);
    }

    // =========================================================================
    // SECTION 1: SERVICE LAYER TESTS (BUSINESS LOGIC & SAFEGUARDS)
    // =========================================================================

    @Test
    @DisplayName("1. Service: Daily velocity calculation accurately evaluates chronological odometer progression")
    void testForecast_VelocityCalculation_PositiveProgression() {
        VehiclePredictiveReportDTO report = predictiveMaintenanceService.getVehicleForecast(vehicleA1.getVehicleId(), userA.getUserId());

        assertNotNull(report);
        // Distance = 22,500 - 20,000 = 2,500 km over 50 elapsed days (day -60 to day -10) -> 50.0 km/day
        assertEquals(50.0, report.getDailyDrivingVelocityKm(), 1.0);
        assertEquals("HIGH", report.getVelocityConfidence());
        assertTrue(report.getOdometerSamplePoints() >= 3);
    }

    @Test
    @DisplayName("2. Service: Safeguard ignores negative odometer rollback and never converts rollback to positive distance")
    void testForecast_VelocityRollbackSafeguard_AnomalousReadingIgnored() {
        // Inject an anomalous odometer rollback entry (e.g. 15,000 km recorded 5 days ago, after 22,500 km)
        FuelRecord rollbackFuel = new FuelRecord();
        rollbackFuel.setVehicle(vehicleA1);
        rollbackFuel.setFuelDate(LocalDate.now().minusDays(5));
        rollbackFuel.setFuelType(FuelType.PETROL);
        rollbackFuel.setQuantityLitres(new BigDecimal("20.00"));
        rollbackFuel.setCostPerLitre(new BigDecimal("100.00"));
        rollbackFuel.setTotalCost(new BigDecimal("2000.00"));
        rollbackFuel.setOdometerAtFill(15000); // Rollback anomaly!
        fuelRecordRepository.save(rollbackFuel);

        VehiclePredictiveReportDTO report = predictiveMaintenanceService.getVehicleForecast(vehicleA1.getVehicleId(), userA.getUserId());

        // Velocity should still be based solely on valid positive intervals (approx 50.0 km/day) without getting corrupted
        assertNotNull(report);
        assertTrue(report.getDailyDrivingVelocityKm() > 0);
        assertTrue(report.getDailyDrivingVelocityKm() <= 60.0);
    }

    @Test
    @DisplayName("3. Service: Safeguard safely falls back to category default velocity when insufficient records exist")
    void testForecast_DefaultVelocityFallback() {
        // Vehicle A2 has no service or fuel history
        VehiclePredictiveReportDTO report = predictiveMaintenanceService.getVehicleForecast(vehicleA2.getVehicleId(), userA.getUserId());

        assertNotNull(report);
        assertEquals("DEFAULT_ESTIMATE", report.getVelocityConfidence());
        // For motorcycle/two-wheeler, fallback is 15.0 km/day
        assertEquals(15.0, report.getDailyDrivingVelocityKm());
    }

    @Test
    @DisplayName("4. Service: Repeating PMS milestones compute correct target odometer, remaining km, and projected dates")
    void testForecast_RepeatingPMSMilestones_Calculation() {
        // Current odometer for vehicleA1 is 23,500 km
        VehiclePredictiveReportDTO report = predictiveMaintenanceService.getVehicleForecast(vehicleA1.getVehicleId(), userA.getUserId());

        assertNotNull(report.getMilestones());
        assertEquals(5, report.getMilestones().size());

        // Check 5,000 km repeating interval: Target = floor(23500/5000 + 1) * 5000 = 25,000 km
        PredictiveMaintenanceMilestoneDTO oilService = report.getMilestones().stream()
                .filter(m -> m.getIntervalKm() == 5000)
                .findFirst().orElseThrow();
        assertEquals(25000, oilService.getTargetOdometer());
        assertEquals(1500, oilService.getKilometersRemaining()); // 25,000 - 23,500
        assertNotNull(oilService.getEstimatedDueDate());
        assertTrue(oilService.getDaysRemaining() > 0);

        // Check 40,000 km repeating interval: Target = 40,000 km
        PredictiveMaintenanceMilestoneDTO transmission = report.getMilestones().stream()
                .filter(m -> m.getIntervalKm() == 40000)
                .findFirst().orElseThrow();
        assertEquals(40000, transmission.getTargetOdometer());
        assertEquals(16500, transmission.getKilometersRemaining());
    }

    @Test
    @DisplayName("5. Service: Vehicle Health Index strictly clamped to 0-100 and applies overdue penalties")
    void testForecast_VehicleHealthIndex_StrictClamping() {
        // Initially without overdue tasks, VHI should be GOOD or EXCELLENT
        VehiclePredictiveReportDTO cleanReport = predictiveMaintenanceService.getVehicleForecast(vehicleA1.getVehicleId(), userA.getUserId());
        int cleanScore = cleanReport.getHealthIndex().getHealthScore();
        assertTrue(cleanScore >= 75 && cleanScore <= 100);

        // Inject 5 overdue maintenance tasks (-25 pts each = -125 pts)
        for (int i = 1; i <= 5; i++) {
            MaintenanceRecord overdueTask = new MaintenanceRecord();
            overdueTask.setVehicle(vehicleA1);
            overdueTask.setTitle("Overdue Critical Repair " + i);
            overdueTask.setScheduledDate(LocalDate.now().minusDays(10 * i));
            overdueTask.setStatus(MaintenanceStatus.OVERDUE);
            overdueTask.setCost(new BigDecimal("1000.00"));
            maintenanceRecordRepository.save(overdueTask);
        }

        VehiclePredictiveReportDTO penalizedReport = predictiveMaintenanceService.getVehicleForecast(vehicleA1.getVehicleId(), userA.getUserId());
        int penalizedScore = penalizedReport.getHealthIndex().getHealthScore();

        // Must be strictly clamped at 0 (never negative)
        assertEquals(0, penalizedScore);
        assertEquals("ATTENTION_REQUIRED", penalizedReport.getHealthIndex().getHealthRating());
        assertEquals("danger", penalizedReport.getHealthIndex().getBadgeColor());
        assertEquals(5, penalizedReport.getHealthIndex().getOverdueTasksCount());
    }

    @Test
    @DisplayName("6. Service: Expense forecasts include only milestones whose projected due date falls within horizon")
    void testForecast_ExpenseForecast_HorizonsOnlyIncludeDueMilestones() {
        VehiclePredictiveReportDTO report = predictiveMaintenanceService.getVehicleForecast(vehicleA1.getVehicleId(), userA.getUserId());
        List<ExpenseForecastDTO> forecasts = report.getExpenseForecasts();

        assertNotNull(forecasts);
        assertEquals(4, forecasts.size()); // 30, 60, 90, 180 days

        ExpenseForecastDTO f30 = forecasts.stream().filter(f -> f.getPeriodDays() == 30).findFirst().orElseThrow();
        ExpenseForecastDTO f180 = forecasts.stream().filter(f -> f.getPeriodDays() == 180).findFirst().orElseThrow();

        // 180 days horizon should cover equal or more due milestones than 30 days
        assertTrue(f180.getDueMilestonesCount() >= f30.getDueMilestonesCount());
        assertTrue(f180.getTotalProjectedSpend().compareTo(f30.getTotalProjectedSpend()) >= 0);
    }

    @Test
    @DisplayName("7. Service: scheduleForecastedMilestone successfully creates active maintenance record")
    void testScheduleForecastedMilestone_Success() {
        ScheduleForecastRequestDTO request = ScheduleForecastRequestDTO.builder()
                .title("Tire Rotation, Alignment & Inspection")
                .description("Rotate tires across all axles")
                .scheduledDate(LocalDate.now().plusDays(20))
                .estimatedCost(new BigDecimal("1200.00"))
                .intervalKm(10000)
                .build();

        MaintenanceRecord created = predictiveMaintenanceService.scheduleForecastedMilestone(
                vehicleA1.getVehicleId(), request, userA.getUserId()
        );

        assertNotNull(created);
        assertNotNull(created.getMaintenanceId());
        assertEquals("Tire Rotation, Alignment & Inspection", created.getTitle());
        assertEquals(new BigDecimal("1200.00"), created.getCost());
        assertEquals(MaintenanceStatus.UPCOMING, created.getStatus());
    }

    @Test
    @DisplayName("8. Service: Safeguard prevents duplicate scheduling of the same pending forecast milestone")
    void testScheduleForecastedMilestone_DuplicatePrevention_ThrowsIllegalState() {
        ScheduleForecastRequestDTO request = ScheduleForecastRequestDTO.builder()
                .title("Engine Oil & Filter Service")
                .description("Engine oil change")
                .scheduledDate(LocalDate.now().plusDays(15))
                .estimatedCost(new BigDecimal("3500.00"))
                .intervalKm(5000)
                .build();

        // First schedule -> success
        predictiveMaintenanceService.scheduleForecastedMilestone(vehicleA1.getVehicleId(), request, userA.getUserId());

        // Second schedule with identical title while first is pending -> throws IllegalStateException
        assertThrows(IllegalStateException.class, () ->
                predictiveMaintenanceService.scheduleForecastedMilestone(vehicleA1.getVehicleId(), request, userA.getUserId())
        );
    }

    @Test
    @DisplayName("9. Service: Cross-user forecast access throws AccessDeniedException")
    void testForecast_CrossUserIsolation_ThrowsAccessDenied() {
        assertThrows(AccessDeniedException.class, () ->
                predictiveMaintenanceService.getVehicleForecast(vehicleB1.getVehicleId(), userA.getUserId())
        );
    }

    // =========================================================================
    // SECTION 2: WEB MVC CONTROLLER TESTS
    // =========================================================================

    @Test
    @DisplayName("10. Web: GET /vehicles/{id}/forecast renders page with health index and milestones")
    @WithMockUser(username = "m14_user_a@test.com", roles = {"NORMAL_USER"})
    void testWebForecast_RendersPageWithHealthAndMilestones() throws Exception {
        mockMvc.perform(get("/vehicles/{id}/forecast", vehicleA1.getVehicleId()))
                .andExpect(status().isOk())
                .andExpect(view().name("vehicle/forecast"))
                .andExpect(model().attributeExists("report"))
                .andExpect(model().attributeExists("vehicle"))
                .andExpect(content().string(containsString("Predictive Maintenance & Smart Service Planner")))
                .andExpect(content().string(containsString("Vehicle Health Index (VHI)")))
                .andExpect(content().string(containsString("Periodic Maintenance Schedule (PMS) Forecasting")));
    }

    @Test
    @DisplayName("11. Web: GET /vehicles/planner redirects to first user vehicle forecast")
    @WithMockUser(username = "m14_user_a@test.com", roles = {"NORMAL_USER"})
    void testWebPlanner_RedirectsToFirstVehicle() throws Exception {
        mockMvc.perform(get("/vehicles/planner"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/vehicles/*/forecast"));
    }

    @Test
    @DisplayName("12. Web: POST /vehicles/{id}/forecast/schedule creates task and redirects with success flash")
    @WithMockUser(username = "m14_user_a@test.com", roles = {"NORMAL_USER"})
    void testWebScheduleMilestone_Success_RedirectsWithFlash() throws Exception {
        mockMvc.perform(post("/vehicles/{id}/forecast/schedule", vehicleA1.getVehicleId())
                        .with(csrf())
                        .param("title", "Engine Oil & Filter Service")
                        .param("description", "Replace oil and filter")
                        .param("scheduledDate", LocalDate.now().plusDays(25).toString())
                        .param("estimatedCost", "3200.00")
                        .param("intervalKm", "5000"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/vehicles/" + vehicleA1.getVehicleId() + "/forecast"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    @DisplayName("13. Web: POST /vehicles/{id}/forecast/schedule duplicate milestone redirects with error flash")
    @WithMockUser(username = "m14_user_a@test.com", roles = {"NORMAL_USER"})
    void testWebScheduleMilestone_Duplicate_RedirectsWithFlashError() throws Exception {
        // Pre-create pending task
        ScheduleForecastRequestDTO request = ScheduleForecastRequestDTO.builder()
                .title("Engine Oil & Filter Service")
                .scheduledDate(LocalDate.now().plusDays(10))
                .estimatedCost(new BigDecimal("3000.00"))
                .build();
        predictiveMaintenanceService.scheduleForecastedMilestone(vehicleA1.getVehicleId(), request, userA.getUserId());

        mockMvc.perform(post("/vehicles/{id}/forecast/schedule", vehicleA1.getVehicleId())
                        .with(csrf())
                        .param("title", "Engine Oil & Filter Service")
                        .param("description", "Duplicate attempt")
                        .param("scheduledDate", LocalDate.now().plusDays(20).toString())
                        .param("estimatedCost", "3200.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/vehicles/" + vehicleA1.getVehicleId() + "/forecast"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    @DisplayName("14. Web: Cross-user forecast tampering redirects with flash error")
    @WithMockUser(username = "m14_user_a@test.com", roles = {"NORMAL_USER"})
    void testWebForecast_CrossUserTampering_RedirectsWithFlash() throws Exception {
        mockMvc.perform(get("/vehicles/{id}/forecast", vehicleB1.getVehicleId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/vehicles"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    @DisplayName("15. Web: Unauthenticated access to forecast redirects to /login")
    void testWebForecast_Unauthenticated_RedirectsToLogin() throws Exception {
        mockMvc.perform(get("/vehicles/{id}/forecast", vehicleA1.getVehicleId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    // =========================================================================
    // SECTION 3: REST API CONTROLLER TESTS
    // =========================================================================

    @Test
    @DisplayName("16. REST: GET /api/vehicles/{id}/forecast returns 200 OK with complete predictive report")
    @WithMockUser(username = "m14_user_a@test.com", roles = {"NORMAL_USER"})
    void testApiForecast_Success() throws Exception {
        mockMvc.perform(get("/api/vehicles/{id}/forecast", vehicleA1.getVehicleId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.vehicleId").value(vehicleA1.getVehicleId()))
                .andExpect(jsonPath("$.plateNumber").value("TN14AA1111"))
                .andExpect(jsonPath("$.healthIndex.healthScore").isNumber())
                .andExpect(jsonPath("$.healthIndex.healthRating").isString())
                .andExpect(jsonPath("$.milestones", hasSize(5)))
                .andExpect(jsonPath("$.expenseForecasts", hasSize(4)));
    }

    @Test
    @DisplayName("17. REST: GET /api/analytics/garage-forecast returns 200 OK array of fleet forecasts")
    @WithMockUser(username = "m14_user_a@test.com", roles = {"NORMAL_USER"})
    void testApiGarageFleetForecast_Success() throws Exception {
        mockMvc.perform(get("/api/analytics/garage-forecast"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].vehicleId").exists())
                .andExpect(jsonPath("$[1].vehicleId").exists());
    }

    @Test
    @DisplayName("18. REST: POST /api/vehicles/{id}/forecast/schedule creates task and returns 201 Created")
    @WithMockUser(username = "m14_user_a@test.com", roles = {"NORMAL_USER"})
    void testApiScheduleMilestone_Success_Returns201() throws Exception {
        ScheduleForecastRequestDTO req = ScheduleForecastRequestDTO.builder()
                .title("Comprehensive Suspension & Spark Plugs")
                .description("Suspension bushing replacement")
                .scheduledDate(LocalDate.now().plusDays(45))
                .estimatedCost(new BigDecimal("7500.00"))
                .intervalKm(60000)
                .build();

        mockMvc.perform(post("/api/vehicles/{id}/forecast/schedule", vehicleA1.getVehicleId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.maintenanceId").exists())
                .andExpect(jsonPath("$.title").value("Comprehensive Suspension & Spark Plugs"))
                .andExpect(jsonPath("$.cost").value(7500.00));
    }

    @Test
    @DisplayName("19. REST: POST /api/vehicles/{id}/forecast/schedule duplicate milestone returns 409 Conflict")
    @WithMockUser(username = "m14_user_a@test.com", roles = {"NORMAL_USER"})
    void testApiScheduleMilestone_Duplicate_Returns409Conflict() throws Exception {
        ScheduleForecastRequestDTO req = ScheduleForecastRequestDTO.builder()
                .title("Brake System & Filters Overhaul")
                .description("Brake pads replacement")
                .scheduledDate(LocalDate.now().plusDays(30))
                .estimatedCost(new BigDecimal("4500.00"))
                .intervalKm(20000)
                .build();

        // First schedule -> success
        predictiveMaintenanceService.scheduleForecastedMilestone(vehicleA1.getVehicleId(), req, userA.getUserId());

        // Second schedule -> 409 Conflict
        mockMvc.perform(post("/api/vehicles/{id}/forecast/schedule", vehicleA1.getVehicleId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    @DisplayName("20. REST: Cross-user vehicle forecast request returns 403 Forbidden")
    @WithMockUser(username = "m14_user_a@test.com", roles = {"NORMAL_USER"})
    void testApiForecast_CrossUserTampering_Returns403() throws Exception {
        mockMvc.perform(get("/api/vehicles/{id}/forecast", vehicleB1.getVehicleId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("21. REST: Boundary validation failure on blank title returns 400 Bad Request")
    @WithMockUser(username = "m14_user_a@test.com", roles = {"NORMAL_USER"})
    void testApiScheduleMilestone_BoundaryValidation_BlankTitle_Returns400() throws Exception {
        ScheduleForecastRequestDTO invalidReq = ScheduleForecastRequestDTO.builder()
                .title("") // Blank title violated @NotBlank
                .scheduledDate(LocalDate.now().plusDays(10))
                .estimatedCost(new BigDecimal("1000.00"))
                .build();

        mockMvc.perform(post("/api/vehicles/{id}/forecast/schedule", vehicleA1.getVehicleId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReq)))
                .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // SECTION 4: SECURITY & ADMIN PRIVACY ISOLATION TESTS
    // =========================================================================

    @Test
    @DisplayName("22. Security: Admin access to GET /vehicles/{id}/forecast returns 403 Forbidden")
    @WithMockUser(username = "m14_admin@mygarage.com", roles = {"ADMIN"})
    void testAdminAccess_WebForecast_Blocked403() throws Exception {
        mockMvc.perform(get("/vehicles/{id}/forecast", vehicleA1.getVehicleId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("23. Security: Admin access to GET /api/vehicles/{id}/forecast returns 403 Forbidden")
    @WithMockUser(username = "m14_admin@mygarage.com", roles = {"ADMIN"})
    void testAdminAccess_ApiForecast_Blocked403() throws Exception {
        mockMvc.perform(get("/api/vehicles/{id}/forecast", vehicleA1.getVehicleId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("24. Security: Admin access to GET /api/analytics/garage-forecast returns 403 Forbidden")
    @WithMockUser(username = "m14_admin@mygarage.com", roles = {"ADMIN"})
    void testAdminAccess_ApiGarageForecast_Blocked403() throws Exception {
        mockMvc.perform(get("/api/analytics/garage-forecast"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("25. Security: Admin access to POST /api/vehicles/{id}/forecast/schedule returns 403 Forbidden")
    @WithMockUser(username = "m14_admin@mygarage.com", roles = {"ADMIN"})
    void testAdminAccess_ApiScheduleMilestone_Blocked403() throws Exception {
        ScheduleForecastRequestDTO req = ScheduleForecastRequestDTO.builder()
                .title("Admin Illegal Schedule")
                .scheduledDate(LocalDate.now().plusDays(5))
                .estimatedCost(new BigDecimal("2000.00"))
                .build();

        mockMvc.perform(post("/api/vehicles/{id}/forecast/schedule", vehicleA1.getVehicleId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }
}
