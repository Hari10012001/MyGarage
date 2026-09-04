package com.mygarage.vehicle;

import com.mygarage.dto.response.FleetComparisonReportDTO;
import com.mygarage.dto.response.FleetExpenseBreakdownDTO;
import com.mygarage.dto.response.VehicleComparisonDTO;
import com.mygarage.model.*;
import com.mygarage.model.enums.FuelType;
import com.mygarage.model.enums.MaintenanceStatus;
import com.mygarage.model.enums.Role;
import com.mygarage.repository.*;
import com.mygarage.service.VehicleComparisonService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Milestone 13 (M13) — Multi-Vehicle Comparative Analytics & Fleet Efficiency Benchmarking Test Suite.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class VehicleComparisonModuleTest {

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
    private VehicleComparisonService vehicleComparisonService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User userA;
    private User userB;
    private Vehicle vehicleA1; // Car
    private Vehicle vehicleA2; // Motorcycle
    private Vehicle vehicleA3; // SUV
    private Vehicle vehicleB1; // Other user's vehicle

    @BeforeEach
    void setUp() {
        maintenanceRecordRepository.deleteAll();
        serviceRecordRepository.deleteAll();
        fuelRecordRepository.deleteAll();
        vehicleRepository.deleteAll();

        // Admin User
        User admin = userRepository.findByEmail("admin@mygarage.com").orElseGet(User::new);
        admin.setFullName("System Administrator");
        admin.setEmail("admin@mygarage.com");
        admin.setPasswordHash(passwordEncoder.encode("Admin@123"));
        admin.setRole(Role.ADMIN);
        admin.setActive(true);
        userRepository.save(admin);

        // User A
        userA = userRepository.findByEmail("m13_user_a@test.com").orElseGet(User::new);
        userA.setFullName("Arun Kumar");
        userA.setEmail("m13_user_a@test.com");
        userA.setPasswordHash(passwordEncoder.encode("Pass@123"));
        userA.setPhone("9876543210");
        userA.setRole(Role.NORMAL_USER);
        userA.setActive(true);
        userA = userRepository.save(userA);

        // User B
        userB = userRepository.findByEmail("m13_user_b@test.com").orElseGet(User::new);
        userB.setFullName("Bala Murugan");
        userB.setEmail("m13_user_b@test.com");
        userB.setPasswordHash(passwordEncoder.encode("Pass@123"));
        userB.setPhone("9876543211");
        userB.setRole(Role.NORMAL_USER);
        userB.setActive(true);
        userB = userRepository.save(userB);

        VehicleCategory cat = categoryRepository.findByNameIgnoreCase("Sedan")
                .orElseGet(() -> {
                    VehicleCategory c = new VehicleCategory();
                    c.setName("Sedan");
                    c.setIcon("car");
                    return categoryRepository.save(c);
                });

        // Vehicle A1: 2022 Honda City (Sedan, Petrol, 30,000 km)
        vehicleA1 = new Vehicle();
        vehicleA1.setUser(userA);
        vehicleA1.setCategory(cat);
        vehicleA1.setPlateNumber("TN01AA1111");
        vehicleA1.setMake("Honda");
        vehicleA1.setModel("City");
        vehicleA1.setYear(2022);
        vehicleA1.setColor("Taffeta White");
        vehicleA1.setFuelType("Petrol");
        vehicleA1.setCurrentOdometer(30000);
        vehicleA1 = vehicleRepository.save(vehicleA1);

        // Vehicle A2: 2023 Yamaha R15 (Bike, Petrol, 10,000 km)
        vehicleA2 = new Vehicle();
        vehicleA2.setUser(userA);
        vehicleA2.setCategory(cat);
        vehicleA2.setPlateNumber("TN01AA2222");
        vehicleA2.setMake("Yamaha");
        vehicleA2.setModel("R15");
        vehicleA2.setYear(2023);
        vehicleA2.setColor("Racing Blue");
        vehicleA2.setFuelType("Petrol");
        vehicleA2.setCurrentOdometer(10000);
        vehicleA2 = vehicleRepository.save(vehicleA2);

        // Vehicle A3: 2021 Toyota Fortuner (SUV, Diesel, 60,000 km)
        vehicleA3 = new Vehicle();
        vehicleA3.setUser(userA);
        vehicleA3.setCategory(cat);
        vehicleA3.setPlateNumber("TN01AA3333");
        vehicleA3.setMake("Toyota");
        vehicleA3.setModel("Fortuner");
        vehicleA3.setYear(2021);
        vehicleA3.setColor("Super White");
        vehicleA3.setFuelType("Diesel");
        vehicleA3.setCurrentOdometer(60000);
        vehicleA3 = vehicleRepository.save(vehicleA3);

        // Vehicle B1: 2020 Hyundai i20 (Owned by User B)
        vehicleB1 = new Vehicle();
        vehicleB1.setUser(userB);
        vehicleB1.setCategory(cat);
        vehicleB1.setPlateNumber("TN02BB9999");
        vehicleB1.setMake("Hyundai");
        vehicleB1.setModel("i20");
        vehicleB1.setYear(2020);
        vehicleB1.setColor("Polar White");
        vehicleB1.setFuelType("Petrol");
        vehicleB1.setCurrentOdometer(40000);
        vehicleB1 = vehicleRepository.save(vehicleB1);

        // Seed records for Vehicle A1 (Honda City)
        // Service: Rs 4000
        ServiceRecord s1 = new ServiceRecord();
        s1.setVehicle(vehicleA1);
        s1.setServiceDate(LocalDate.now().minusMonths(3));
        s1.setServiceType("Major Service");
        s1.setCost(new BigDecimal("4000.00"));
        s1.setOdometerAtService(25000);
        serviceRecordRepository.save(s1);

        // Fuel: Rs 3000, 30L, 16.5 km/L
        FuelRecord f1 = new FuelRecord();
        f1.setVehicle(vehicleA1);
        f1.setFuelDate(LocalDate.now().minusDays(10));
        f1.setFuelType(FuelType.PETROL);
        f1.setQuantityLitres(new BigDecimal("30.00"));
        f1.setCostPerLitre(new BigDecimal("100.00"));
        f1.setTotalCost(new BigDecimal("3000.00"));
        f1.setEstimatedMileageKmpl(new BigDecimal("16.50"));
        fuelRecordRepository.save(f1);

        // Maintenance: Rs 1000, COMPLETED
        MaintenanceRecord m1 = new MaintenanceRecord();
        m1.setVehicle(vehicleA1);
        m1.setTitle("Wheel Alignment");
        m1.setScheduledDate(LocalDate.now().minusMonths(1));
        m1.setCompletedDate(LocalDate.now().minusMonths(1));
        m1.setStatus(MaintenanceStatus.COMPLETED);
        m1.setCost(new BigDecimal("1000.00"));
        maintenanceRecordRepository.save(m1);

        // Seed records for Vehicle A2 (Yamaha R15 - Higher Mileage 45.0 km/L)
        // Service: Rs 1500
        ServiceRecord s2 = new ServiceRecord();
        s2.setVehicle(vehicleA2);
        s2.setServiceDate(LocalDate.now().minusMonths(2));
        s2.setServiceType("First Service");
        s2.setCost(new BigDecimal("1500.00"));
        s2.setOdometerAtService(5000);
        serviceRecordRepository.save(s2);

        // Fuel: Rs 1000, 10L, 45.0 km/L
        FuelRecord f2 = new FuelRecord();
        f2.setVehicle(vehicleA2);
        f2.setFuelDate(LocalDate.now().minusDays(5));
        f2.setFuelType(FuelType.PETROL);
        f2.setQuantityLitres(new BigDecimal("10.00"));
        f2.setCostPerLitre(new BigDecimal("100.00"));
        f2.setTotalCost(new BigDecimal("1000.00"));
        f2.setEstimatedMileageKmpl(new BigDecimal("45.00"));
        fuelRecordRepository.save(f2);

        // Maintenance: Rs 500, COMPLETED
        MaintenanceRecord m2 = new MaintenanceRecord();
        m2.setVehicle(vehicleA2);
        m2.setTitle("Chain Lube & Tightening");
        m2.setScheduledDate(LocalDate.now().minusDays(20));
        m2.setCompletedDate(LocalDate.now().minusDays(20));
        m2.setStatus(MaintenanceStatus.COMPLETED);
        m2.setCost(new BigDecimal("500.00"));
        maintenanceRecordRepository.save(m2);

        // Seed records for Vehicle A3 (Toyota Fortuner - Workhorse 60,000 km, Higher Spend)
        ServiceRecord s3 = new ServiceRecord();
        s3.setVehicle(vehicleA3);
        s3.setServiceDate(LocalDate.now().minusMonths(4));
        s3.setServiceType("Full Inspection");
        s3.setCost(new BigDecimal("8000.00"));
        s3.setOdometerAtService(55000);
        serviceRecordRepository.save(s3);
    }

    // =========================================================================
    // SECTION 1: SERVICE LAYER TESTS
    // =========================================================================

    @Test
    @DisplayName("1. Service: compareVehicles successfully compares 2 owned vehicles")
    void testCompareTwoVehicles_Success() {
        FleetComparisonReportDTO report = vehicleComparisonService.compareVehicles(
                Arrays.asList(vehicleA1.getVehicleId(), vehicleA2.getVehicleId()),
                userA.getUserId()
        );

        assertNotNull(report);
        assertEquals(2, report.getComparedVehicleCount());
        assertEquals(2, report.getVehicles().size());

        VehicleComparisonDTO v1 = report.getVehicles().get(0);
        assertEquals("TN01AA1111", v1.getPlateNumber());
        assertEquals(new BigDecimal("8000.00"), v1.getTotalOwnershipCost()); // 4000 + 3000 + 1000

        VehicleComparisonDTO v2 = report.getVehicles().get(1);
        assertEquals("TN01AA2222", v2.getPlateNumber());
        assertEquals(new BigDecimal("3000.00"), v2.getTotalOwnershipCost()); // 1500 + 1000 + 500

        // Fleet Total = 8000 + 3000 = 11000
        assertEquals(new BigDecimal("11000.00"), report.getFleetTotalSpend());
    }

    @Test
    @DisplayName("2. Service: compareVehicles successfully compares 3 owned vehicles")
    void testCompareThreeVehicles_Success() {
        FleetComparisonReportDTO report = vehicleComparisonService.compareVehicles(
                Arrays.asList(vehicleA1.getVehicleId(), vehicleA2.getVehicleId(), vehicleA3.getVehicleId()),
                userA.getUserId()
        );

        assertNotNull(report);
        assertEquals(3, report.getComparedVehicleCount());
        assertEquals(3, report.getVehicles().size());
    }

    @Test
    @DisplayName("3. Service: Badging logic awards Most Fuel Efficient to vehicle with highest km/L")
    void testBadgingLogic_MostFuelEfficient() {
        FleetComparisonReportDTO report = vehicleComparisonService.compareVehicles(
                Arrays.asList(vehicleA1.getVehicleId(), vehicleA2.getVehicleId()),
                userA.getUserId()
        );

        VehicleComparisonDTO bike = report.getVehicles().stream()
                .filter(v -> v.getPlateNumber().equals("TN01AA2222"))
                .findFirst().orElseThrow();
        VehicleComparisonDTO car = report.getVehicles().stream()
                .filter(v -> v.getPlateNumber().equals("TN01AA1111"))
                .findFirst().orElseThrow();

        assertTrue(bike.isMostFuelEfficient(), "Bike with 45.0 km/L should be marked most fuel efficient");
        assertFalse(car.isMostFuelEfficient(), "Car with 16.5 km/L should not be most fuel efficient");
        assertEquals("TN01AA2222", report.getMostEfficientVehiclePlate());
    }

    @Test
    @DisplayName("4. Service: Badging logic awards Fleet Workhorse to vehicle with highest odometer")
    void testBadgingLogic_FleetWorkhorse() {
        FleetComparisonReportDTO report = vehicleComparisonService.compareVehicles(
                Arrays.asList(vehicleA1.getVehicleId(), vehicleA2.getVehicleId(), vehicleA3.getVehicleId()),
                userA.getUserId()
        );

        VehicleComparisonDTO fortuner = report.getVehicles().stream()
                .filter(v -> v.getPlateNumber().equals("TN01AA3333"))
                .findFirst().orElseThrow();

        assertTrue(fortuner.isFleetWorkhorse(), "Fortuner with 60,000 km should be Fleet Workhorse");
        assertEquals("TN01AA3333", report.getFleetWorkhorsePlate());
    }

    @Test
    @DisplayName("5. Service: Cross-user vehicle comparison attempt throws AccessDeniedException")
    void testCompareVehicles_CrossUserTampering_Forbidden() {
        assertThrows(AccessDeniedException.class, () ->
                vehicleComparisonService.compareVehicles(
                        Arrays.asList(vehicleA1.getVehicleId(), vehicleB1.getVehicleId()),
                        userA.getUserId()
                )
        );
    }

    @Test
    @DisplayName("6. Service: Comparison with less than 2 vehicles throws IllegalArgumentException")
    void testCompareVehicles_LessThanTwoVehicles_BadRequest() {
        assertThrows(IllegalArgumentException.class, () ->
                vehicleComparisonService.compareVehicles(
                        Collections.singletonList(vehicleA1.getVehicleId()),
                        userA.getUserId()
                )
        );
    }

    @Test
    @DisplayName("7. Service: Comparison with more than 4 vehicles throws IllegalArgumentException")
    void testCompareVehicles_MoreThanFourVehicles_BadRequest() {
        assertThrows(IllegalArgumentException.class, () ->
                vehicleComparisonService.compareVehicles(
                        Arrays.asList(1L, 2L, 3L, 4L, 5L),
                        userA.getUserId()
                )
        );
    }

    @Test
    @DisplayName("8. Service: getFleetExpenseBreakdown computes percentage distribution accurately")
    void testFleetExpenseBreakdown_Success() {
        FleetExpenseBreakdownDTO breakdown = vehicleComparisonService.getFleetExpenseBreakdown(userA.getUserId());

        assertNotNull(breakdown);
        assertEquals(3, breakdown.getTotalVehicles());
        assertTrue(breakdown.getTotalFleetSpend().compareTo(BigDecimal.ZERO) > 0);
        assertNotNull(breakdown.getVehicleShares());
        assertEquals(3, breakdown.getVehicleShares().size());
        assertTrue(breakdown.getServiceCostPercentage() >= 0);
        assertTrue(breakdown.getFuelCostPercentage() >= 0);
        assertTrue(breakdown.getMaintenanceCostPercentage() >= 0);
    }

    // =========================================================================
    // SECTION 2: WEB MVC CONTROLLER TESTS
    // =========================================================================

    @Test
    @DisplayName("9. Web: GET /vehicles/compare without params renders vehicle selection page")
    @WithMockUser(username = "m13_user_a@test.com", roles = {"NORMAL_USER"})
    void testWebComparePage_SelectionForm() throws Exception {
        mockMvc.perform(get("/vehicles/compare"))
                .andExpect(status().isOk())
                .andExpect(view().name("vehicle/compare"))
                .andExpect(model().attributeExists("ownedVehicles"))
                .andExpect(model().attributeExists("fleetBreakdown"))
                .andExpect(content().string(containsString("Fleet Efficiency & Multi-Vehicle Comparison")));
    }

    @Test
    @DisplayName("10. Web: GET /vehicles/compare with valid vehicleIds renders side-by-side table")
    @WithMockUser(username = "m13_user_a@test.com", roles = {"NORMAL_USER"})
    void testWebComparePage_WithVehicles_Success() throws Exception {
        mockMvc.perform(get("/vehicles/compare")
                        .param("vehicleIds", vehicleA1.getVehicleId().toString())
                        .param("vehicleIds", vehicleA2.getVehicleId().toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("vehicle/compare"))
                .andExpect(model().attributeExists("report"))
                .andExpect(content().string(containsString("TN01AA1111")))
                .andExpect(content().string(containsString("TN01AA2222")))
                .andExpect(content().string(containsString("Lifetime Financial Expenditures")));
    }

    @Test
    @DisplayName("11. Web: GET /vehicles/compare with cross-user tampering redirects with flash error")
    @WithMockUser(username = "m13_user_a@test.com", roles = {"NORMAL_USER"})
    void testWebComparePage_CrossUserTampering_Redirects() throws Exception {
        mockMvc.perform(get("/vehicles/compare")
                        .param("vehicleIds", vehicleA1.getVehicleId().toString())
                        .param("vehicleIds", vehicleB1.getVehicleId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/vehicles/compare"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    @DisplayName("12. Web: GET /vehicles/compare with single vehicle renders error message")
    @WithMockUser(username = "m13_user_a@test.com", roles = {"NORMAL_USER"})
    void testWebComparePage_SingleVehicle_ShowsError() throws Exception {
        mockMvc.perform(get("/vehicles/compare")
                        .param("vehicleIds", vehicleA1.getVehicleId().toString()))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("errorMessage"))
                .andExpect(content().string(containsString("Please select at least 2 vehicles")));
    }

    // =========================================================================
    // SECTION 3: REST API TESTS
    // =========================================================================

    @Test
    @DisplayName("13. REST: GET /api/analytics/compare returns 200 OK with report JSON")
    @WithMockUser(username = "m13_user_a@test.com", roles = {"NORMAL_USER"})
    void testRestApiCompareVehicles_Success() throws Exception {
        mockMvc.perform(get("/api/analytics/compare")
                        .param("vehicleIds", vehicleA1.getVehicleId().toString(), vehicleA2.getVehicleId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comparedVehicleCount").value(2))
                .andExpect(jsonPath("$.fleetTotalSpend").value(11000.00))
                .andExpect(jsonPath("$.vehicles", hasSize(2)))
                .andExpect(jsonPath("$.vehicles[0].plateNumber").value("TN01AA1111"))
                .andExpect(jsonPath("$.vehicles[1].plateNumber").value("TN01AA2222"));
    }

    @Test
    @DisplayName("14. REST: GET /api/analytics/compare cross-user tampering returns 403 Forbidden")
    @WithMockUser(username = "m13_user_a@test.com", roles = {"NORMAL_USER"})
    void testRestApiCompareVehicles_CrossUser_Forbidden() throws Exception {
        mockMvc.perform(get("/api/analytics/compare")
                        .param("vehicleIds", vehicleA1.getVehicleId().toString(), vehicleB1.getVehicleId().toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("15. REST: GET /api/analytics/fleet-breakdown returns 200 OK with breakdown JSON")
    @WithMockUser(username = "m13_user_a@test.com", roles = {"NORMAL_USER"})
    void testRestApiFleetBreakdown_Success() throws Exception {
        mockMvc.perform(get("/api/analytics/fleet-breakdown"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVehicles").value(3))
                .andExpect(jsonPath("$.totalFleetSpend").isNumber())
                .andExpect(jsonPath("$.vehicleShares", hasSize(3)));
    }

    @Test
    @DisplayName("16. REST: GET /api/vehicles/{id1}/compare/{id2} pairwise comparison returns 200 OK")
    @WithMockUser(username = "m13_user_a@test.com", roles = {"NORMAL_USER"})
    void testRestApiPairwiseCompare_Success() throws Exception {
        mockMvc.perform(get("/api/vehicles/" + vehicleA1.getVehicleId() + "/compare/" + vehicleA2.getVehicleId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comparedVehicleCount").value(2))
                .andExpect(jsonPath("$.vehicles[0].plateNumber").value("TN01AA1111"))
                .andExpect(jsonPath("$.vehicles[1].plateNumber").value("TN01AA2222"));
    }

    @Test
    @DisplayName("17. REST: GET /api/vehicles/{id1}/compare/{id2} cross-user pairwise returns 403 Forbidden")
    @WithMockUser(username = "m13_user_a@test.com", roles = {"NORMAL_USER"})
    void testRestApiPairwiseCompare_CrossUser_Forbidden() throws Exception {
        mockMvc.perform(get("/api/vehicles/" + vehicleA1.getVehicleId() + "/compare/" + vehicleB1.getVehicleId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("18. REST: GET /api/vehicles/{id}/analytics single vehicle metrics returns 200 OK")
    @WithMockUser(username = "m13_user_a@test.com", roles = {"NORMAL_USER"})
    void testRestApiSingleVehicleAnalytics_Success() throws Exception {
        mockMvc.perform(get("/api/vehicles/" + vehicleA1.getVehicleId() + "/analytics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plateNumber").value("TN01AA1111"))
                .andExpect(jsonPath("$.totalOwnershipCost").value(8000.00))
                .andExpect(jsonPath("$.avgMileageKmpl").value(16.50));
    }

    @Test
    @DisplayName("19. REST: GET /api/vehicles/{id}/analytics cross-user access returns 403 Forbidden")
    @WithMockUser(username = "m13_user_a@test.com", roles = {"NORMAL_USER"})
    void testRestApiSingleVehicleAnalytics_CrossUser_Forbidden() throws Exception {
        mockMvc.perform(get("/api/vehicles/" + vehicleB1.getVehicleId() + "/analytics"))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // SECTION 4: RBAC & UNAUTHENTICATED PROTECTION
    // =========================================================================

    @Test
    @DisplayName("20. RBAC: ADMIN blocked from /vehicles/compare (403 Forbidden)")
    @WithMockUser(username = "admin@mygarage.com", roles = {"ADMIN"})
    void testAdminBlockedFromCompareWeb() throws Exception {
        mockMvc.perform(get("/vehicles/compare"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("21. RBAC: ADMIN blocked from /api/analytics/compare (403 Forbidden)")
    @WithMockUser(username = "admin@mygarage.com", roles = {"ADMIN"})
    void testAdminBlockedFromCompareApi() throws Exception {
        mockMvc.perform(get("/api/analytics/compare")
                        .param("vehicleIds", vehicleA1.getVehicleId().toString(), vehicleA2.getVehicleId().toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("22. RBAC: ADMIN blocked from /api/analytics/fleet-breakdown (403 Forbidden)")
    @WithMockUser(username = "admin@mygarage.com", roles = {"ADMIN"})
    void testAdminBlockedFromFleetBreakdownApi() throws Exception {
        mockMvc.perform(get("/api/analytics/fleet-breakdown"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("23. Security: Unauthenticated access to /vehicles/compare redirects to /login")
    void testUnauthenticatedCompareWebRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/vehicles/compare"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("24. Security: Unauthenticated access to /api/analytics/compare blocked")
    void testUnauthenticatedCompareApiBlocked() throws Exception {
        mockMvc.perform(get("/api/analytics/compare")
                        .param("vehicleIds", "1", "2"))
                .andExpect(status().is3xxRedirection());
    }
}
