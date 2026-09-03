package com.mygarage.dashboard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mygarage.model.*;
import com.mygarage.model.enums.MaintenanceStatus;
import com.mygarage.model.enums.Role;
import com.mygarage.repository.*;
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
 * Milestone 8 (M8) — Comprehensive Dashboard & Reporting Module Test Suite.
 * Covers 27 test cases:
 * 1. Web: Authenticated NORMAL_USER dashboard access
 * 2. Web: Dashboard renders stat counts (vehicles, services, fuel, maintenance)
 * 3. Web: Dashboard renders maintenance urgency breakdown
 * 4. Web: Dashboard renders financial spend summary
 * 5. Web: Dashboard renders average fuel economy
 * 6. Web: Dashboard displays active maintenance alerts
 * 7. Web: Dashboard displays recent service history
 * 8. Web: Dashboard displays recent fuel activity
 * 9. Web: Dashboard displays multiple vehicles in vehicle summary
 * 10. Web: Empty-state behavior when user has zero vehicles
 * 11. Web: Empty-state behavior when user has vehicles but zero logs
 * 12. Security: Unauthenticated access to /dashboard redirects to /login
 * 13. Security: ADMIN role cannot access user /dashboard (403 Forbidden)
 * 14. Security: NORMAL_USER cannot access /admin/dashboard (403 Forbidden)
 * 15. Security: ADMIN role can access /admin/dashboard (200 OK)
 * 16. Security: ADMIN dashboard displays system-wide statistics only
 * 17. Ownership Isolation: User 1 sees only User 1's vehicles and data
 * 18. Ownership Isolation: User 2 sees only User 2's vehicles and data (zero leakage)
 * 19. REST: GET /api/dashboard returns 200 OK with user metrics
 * 20. REST: GET /api/dashboard/summary returns full financial and urgency breakdowns
 * 21. REST: GET /api/dashboard/alerts returns user maintenance alerts
 * 22. REST: GET /api/dashboard/recent-services returns latest user services
 * 23. REST: GET /api/dashboard/recent-fuel returns latest user fuel logs
 * 24. REST: Unauthenticated API access blocked
 * 25. REST: ADMIN role forbidden from /api/dashboard (403 Forbidden)
 * 26. REST: NORMAL_USER forbidden from /api/admin/statistics (403 Forbidden)
 * 27. REST: ADMIN role can access /api/admin/statistics (200 OK)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DashboardModuleTest {

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
    private PasswordEncoder passwordEncoder;

    private User user1;
    private User user2;
    private User admin;
    private VehicleCategory category;
    private Vehicle user1Vehicle1;
    private Vehicle user1Vehicle2;
    private Vehicle user2Vehicle;

    @BeforeEach
    void setUp() {
        maintenanceRecordRepository.deleteAll();
        serviceRecordRepository.deleteAll();
        fuelRecordRepository.deleteAll();
        vehicleRepository.deleteAll();

        // 1. User 1
        user1 = userRepository.findByEmail("dash_user1@test.com").orElseGet(User::new);
        user1.setFullName("Dashboard User One");
        user1.setEmail("dash_user1@test.com");
        user1.setPasswordHash(passwordEncoder.encode("Secret@123"));
        user1.setPhone("9876500001");
        user1.setRole(Role.NORMAL_USER);
        user1.setActive(true);
        user1 = userRepository.save(user1);

        // 2. User 2
        user2 = userRepository.findByEmail("dash_user2@test.com").orElseGet(User::new);
        user2.setFullName("Dashboard User Two");
        user2.setEmail("dash_user2@test.com");
        user2.setPasswordHash(passwordEncoder.encode("Secret@123"));
        user2.setPhone("9876500002");
        user2.setRole(Role.NORMAL_USER);
        user2.setActive(true);
        user2 = userRepository.save(user2);

        // 3. Admin
        admin = userRepository.findByEmail("dash_admin@test.com").orElseGet(User::new);
        admin.setFullName("Dashboard Admin");
        admin.setEmail("dash_admin@test.com");
        admin.setPasswordHash(passwordEncoder.encode("Admin@123"));
        admin.setPhone("9999900001");
        admin.setRole(Role.ADMIN);
        admin.setActive(true);
        admin = userRepository.save(admin);

        // 4. Category
        category = categoryRepository.findByNameIgnoreCase("SUV").orElseGet(() -> {
            VehicleCategory cat = new VehicleCategory();
            cat.setName("SUV");
            cat.setIcon("🚙");
            return categoryRepository.save(cat);
        });

        // 5. User 1 Vehicles (2 vehicles)
        user1Vehicle1 = new Vehicle();
        user1Vehicle1.setUser(user1);
        user1Vehicle1.setCategory(category);
        user1Vehicle1.setPlateNumber("DL01AB1111");
        user1Vehicle1.setMake("Hyundai");
        user1Vehicle1.setModel("Creta");
        user1Vehicle1.setYear(2023);
        user1Vehicle1.setCurrentOdometer(15000);
        user1Vehicle1 = vehicleRepository.save(user1Vehicle1);

        user1Vehicle2 = new Vehicle();
        user1Vehicle2.setUser(user1);
        user1Vehicle2.setCategory(category);
        user1Vehicle2.setPlateNumber("DL01AB2222");
        user1Vehicle2.setMake("Kia");
        user1Vehicle2.setModel("Seltos");
        user1Vehicle2.setYear(2024);
        user1Vehicle2.setCurrentOdometer(8000);
        user1Vehicle2 = vehicleRepository.save(user1Vehicle2);

        // User 2 Vehicle (1 vehicle)
        user2Vehicle = new Vehicle();
        user2Vehicle.setUser(user2);
        user2Vehicle.setCategory(category);
        user2Vehicle.setPlateNumber("MH01XY9999");
        user2Vehicle.setMake("Toyota");
        user2Vehicle.setModel("Fortuner");
        user2Vehicle.setYear(2022);
        user2Vehicle.setCurrentOdometer(30000);
        user2Vehicle = vehicleRepository.save(user2Vehicle);

        // User 1 Data Seeding:
        // Service Records (2)
        ServiceRecord svc1 = new ServiceRecord();
        svc1.setVehicle(user1Vehicle1);
        svc1.setServiceDate(LocalDate.now().minusMonths(2));
        svc1.setServiceType("Periodic Maintenance");
        svc1.setCost(new BigDecimal("4500.00"));
        svc1.setOdometerAtService(10000);
        serviceRecordRepository.save(svc1);

        ServiceRecord svc2 = new ServiceRecord();
        svc2.setVehicle(user1Vehicle2);
        svc2.setServiceDate(LocalDate.now().minusWeeks(2));
        svc2.setServiceType("Wheel Alignment");
        svc2.setCost(new BigDecimal("1200.00"));
        svc2.setOdometerAtService(7500);
        serviceRecordRepository.save(svc2);

        // Fuel Records (2)
        FuelRecord fuel1 = new FuelRecord();
        fuel1.setVehicle(user1Vehicle1);
        fuel1.setFuelDate(LocalDate.now().minusWeeks(3));
        fuel1.setFuelType(com.mygarage.model.enums.FuelType.PETROL);
        fuel1.setQuantityLitres(new BigDecimal("35.00"));
        fuel1.setCostPerLitre(new BigDecimal("100.00"));
        fuel1.setTotalCost(new BigDecimal("3500.00"));
        fuel1.setOdometerAtFill(14500);
        fuelRecordRepository.save(fuel1);

        FuelRecord fuel2 = new FuelRecord();
        fuel2.setVehicle(user1Vehicle1);
        fuel2.setFuelDate(LocalDate.now().minusDays(5));
        fuel2.setFuelType(com.mygarage.model.enums.FuelType.PETROL);
        fuel2.setQuantityLitres(new BigDecimal("25.00"));
        fuel2.setCostPerLitre(new BigDecimal("100.00"));
        fuel2.setTotalCost(new BigDecimal("2500.00"));
        fuel2.setOdometerAtFill(15000);
        fuel2.setEstimatedMileageKmpl(new BigDecimal("20.00"));
        fuelRecordRepository.save(fuel2);

        // Maintenance Records (3: 1 Overdue, 1 Due Today, 1 Upcoming)
        MaintenanceRecord m1 = new MaintenanceRecord();
        m1.setVehicle(user1Vehicle1);
        m1.setTitle("Brake Pad Inspection");
        m1.setScheduledDate(LocalDate.now().minusDays(2));
        m1.setStatus(MaintenanceStatus.OVERDUE);
        m1.setCost(new BigDecimal("800.00"));
        maintenanceRecordRepository.save(m1);

        MaintenanceRecord m2 = new MaintenanceRecord();
        m2.setVehicle(user1Vehicle2);
        m2.setTitle("Tyre Pressure & Rotation");
        m2.setScheduledDate(LocalDate.now());
        m2.setStatus(MaintenanceStatus.DUE_TODAY);
        m2.setCost(new BigDecimal("300.00"));
        maintenanceRecordRepository.save(m2);

        MaintenanceRecord m3 = new MaintenanceRecord();
        m3.setVehicle(user1Vehicle1);
        m3.setTitle("Air Filter Change");
        m3.setScheduledDate(LocalDate.now().plusMonths(2));
        m3.setStatus(MaintenanceStatus.UPCOMING);
        m3.setCost(new BigDecimal("600.00"));
        maintenanceRecordRepository.save(m3);
    }

    // =========================================================================
    // SECTION 1: WEB MVC DASHBOARD TESTS
    // =========================================================================

    @Test
    @DisplayName("1. Web: Authenticated NORMAL_USER Dashboard Access")
    @WithMockUser(username = "dash_user1@test.com", roles = {"NORMAL_USER"})
    void testUserDashboardAccess() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard/index"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("vehicles"));
    }

    @Test
    @DisplayName("2. Web: Dashboard Renders Stat Counts (Vehicles, Services, Fuel, Maintenance)")
    @WithMockUser(username = "dash_user1@test.com", roles = {"NORMAL_USER"})
    void testDashboardStatCounts() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("vehicleCount", 2L))
                .andExpect(model().attribute("serviceCount", 2L))
                .andExpect(model().attribute("fuelCount", 2L))
                .andExpect(model().attribute("maintenanceCount", 3L));
    }

    @Test
    @DisplayName("3. Web: Dashboard Renders Maintenance Urgency Breakdown")
    @WithMockUser(username = "dash_user1@test.com", roles = {"NORMAL_USER"})
    void testDashboardMaintenanceUrgencyBreakdown() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("overdueCount", 1L))
                .andExpect(model().attribute("dueTodayCount", 1L))
                .andExpect(model().attribute("upcomingCount", 1L))
                .andExpect(model().attribute("completedCount", 0L))
                .andExpect(model().attribute("alertCount", 2L)); // Overdue + Due Today
    }

    @Test
    @DisplayName("4. Web: Dashboard Renders Financial Spend Summary")
    @WithMockUser(username = "dash_user1@test.com", roles = {"NORMAL_USER"})
    void testDashboardFinancialSpendSummary() throws Exception {
        // Fuel = 3500 + 2500 = 6000.00
        // Service = 4500 + 1200 = 5700.00
        // Maintenance = 800 + 300 + 600 = 1700.00
        // Total = 13400.00
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("totalFuelCost", new BigDecimal("6000.00")))
                .andExpect(model().attribute("totalServiceCost", new BigDecimal("5700.00")))
                .andExpect(model().attribute("totalMaintenanceCost", new BigDecimal("1700.00")))
                .andExpect(model().attribute("totalGarageCost", new BigDecimal("13400.00")));
    }

    @Test
    @DisplayName("5. Web: Dashboard Renders Average Fuel Economy")
    @WithMockUser(username = "dash_user1@test.com", roles = {"NORMAL_USER"})
    void testDashboardAverageMileage() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("averageMileage", notNullValue()));
    }

    @Test
    @DisplayName("6. Web: Dashboard Displays Active Maintenance Alerts")
    @WithMockUser(username = "dash_user1@test.com", roles = {"NORMAL_USER"})
    void testDashboardDisplaysAlerts() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("maintenanceAlerts", hasSize(2)));
    }

    @Test
    @DisplayName("7. Web: Dashboard Displays Recent Service History")
    @WithMockUser(username = "dash_user1@test.com", roles = {"NORMAL_USER"})
    void testDashboardDisplaysRecentServices() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("recentServices", hasSize(2)));
    }

    @Test
    @DisplayName("8. Web: Dashboard Displays Recent Fuel Activity")
    @WithMockUser(username = "dash_user1@test.com", roles = {"NORMAL_USER"})
    void testDashboardDisplaysRecentFuel() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("recentFuelRecords", hasSize(2)));
    }

    @Test
    @DisplayName("9. Web: Dashboard Displays Multiple Vehicles in Summary")
    @WithMockUser(username = "dash_user1@test.com", roles = {"NORMAL_USER"})
    void testDashboardDisplaysMultipleVehicles() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("vehicles", hasSize(2)));
    }

    @Test
    @DisplayName("10. Web: Empty-State Behavior When User Has Zero Vehicles")
    @WithMockUser(username = "dash_user2@test.com", roles = {"NORMAL_USER"})
    void testDashboardEmptyStateZeroVehicles() throws Exception {
        // Delete User 2's vehicle
        vehicleRepository.deleteById(user2Vehicle.getVehicleId());

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("vehicleCount", 0L))
                .andExpect(model().attribute("serviceCount", 0L))
                .andExpect(model().attribute("fuelCount", 0L))
                .andExpect(model().attribute("maintenanceCount", 0L))
                .andExpect(model().attribute("alertCount", 0L));
    }

    @Test
    @DisplayName("11. Web: Empty-State Behavior When User Has Vehicle But Zero Logs")
    @WithMockUser(username = "dash_user2@test.com", roles = {"NORMAL_USER"})
    void testDashboardEmptyStateZeroLogs() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("vehicleCount", 1L))
                .andExpect(model().attribute("serviceCount", 0L))
                .andExpect(model().attribute("fuelCount", 0L))
                .andExpect(model().attribute("maintenanceCount", 0L))
                .andExpect(model().attribute("recentServices", hasSize(0)))
                .andExpect(model().attribute("recentFuelRecords", hasSize(0)))
                .andExpect(model().attribute("maintenanceAlerts", hasSize(0)));
    }

    // =========================================================================
    // SECTION 2: SECURITY & RBAC ISOLATION
    // =========================================================================

    @Test
    @DisplayName("12. Security: Unauthenticated Access to /dashboard Redirects to /login")
    void testUnauthenticatedAccessRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("13. Security: ADMIN Role Cannot Access User /dashboard (403 Forbidden)")
    @WithMockUser(username = "dash_admin@test.com", roles = {"ADMIN"})
    void testAdminCannotAccessUserDashboard() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("14. Security: NORMAL_USER Cannot Access /admin/dashboard (403 Forbidden)")
    @WithMockUser(username = "dash_user1@test.com", roles = {"NORMAL_USER"})
    void testUserCannotAccessAdminDashboard() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("15. Security: ADMIN Role Can Access /admin/dashboard (200 OK)")
    @WithMockUser(username = "dash_admin@test.com", roles = {"ADMIN"})
    void testAdminCanAccessAdminDashboard() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"))
                .andExpect(model().attributeExists("totalUsers"))
                .andExpect(model().attributeExists("totalVehicles"));
    }

    @Test
    @DisplayName("16. Security: ADMIN Dashboard Displays System-Wide Statistics Only")
    @WithMockUser(username = "dash_admin@test.com", roles = {"ADMIN"})
    void testAdminDashboardDisplaysSystemStats() throws Exception {
        // Total users = 2 NORMAL_USERS
        // Total vehicles = 3 across all users
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("totalVehicles", 3L))
                .andExpect(model().attribute("totalServiceRecords", 2L))
                .andExpect(model().attribute("totalFuelRecords", 2L))
                .andExpect(model().attribute("totalMaintenanceRecords", 3L));
    }

    // =========================================================================
    // SECTION 3: OWNERSHIP & DATA ISOLATION
    // =========================================================================

    @Test
    @DisplayName("17. Ownership Isolation: User 1 Sees Only User 1's Vehicles and Data")
    @WithMockUser(username = "dash_user1@test.com", roles = {"NORMAL_USER"})
    void testUser1SeesOnlyOwnData() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("vehicleCount", 2L))
                .andExpect(model().attribute("serviceCount", 2L))
                .andExpect(model().attribute("fuelCount", 2L))
                .andExpect(model().attribute("maintenanceCount", 3L));
    }

    @Test
    @DisplayName("18. Ownership Isolation: User 2 Sees Only User 2's Vehicles and Data (Zero Leakage)")
    @WithMockUser(username = "dash_user2@test.com", roles = {"NORMAL_USER"})
    void testUser2SeesOnlyOwnData() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("vehicleCount", 1L))
                .andExpect(model().attribute("serviceCount", 0L))
                .andExpect(model().attribute("fuelCount", 0L))
                .andExpect(model().attribute("maintenanceCount", 0L))
                .andExpect(model().attribute("totalFuelCost", new BigDecimal("0")))
                .andExpect(model().attribute("totalServiceCost", new BigDecimal("0")))
                .andExpect(model().attribute("totalMaintenanceCost", new BigDecimal("0")));
    }

    // =========================================================================
    // SECTION 4: REST API REPORTING & RBAC
    // =========================================================================

    @Test
    @DisplayName("19. REST: GET /api/dashboard Returns 200 OK with User Metrics")
    @WithMockUser(username = "dash_user1@test.com", roles = {"NORMAL_USER"})
    void testRestGetDashboardMetrics() throws Exception {
        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.vehicleCount").value(2))
                .andExpect(jsonPath("$.serviceCount").value(2))
                .andExpect(jsonPath("$.fuelCount").value(2))
                .andExpect(jsonPath("$.maintenanceCount").value(3));
    }

    @Test
    @DisplayName("20. REST: GET /api/dashboard/summary Returns Full Financial and Urgency Breakdowns")
    @WithMockUser(username = "dash_user1@test.com", roles = {"NORMAL_USER"})
    void testRestGetDashboardSummary() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overdueCount").value(1))
                .andExpect(jsonPath("$.dueTodayCount").value(1))
                .andExpect(jsonPath("$.upcomingCount").value(1))
                .andExpect(jsonPath("$.completedCount").value(0))
                .andExpect(jsonPath("$.totalFuelCost").value(6000.0))
                .andExpect(jsonPath("$.totalServiceCost").value(5700.0))
                .andExpect(jsonPath("$.totalMaintenanceCost").value(1700.0))
                .andExpect(jsonPath("$.totalGarageCost").value(13400.0));
    }

    @Test
    @DisplayName("21. REST: GET /api/dashboard/alerts Returns User Maintenance Alerts")
    @WithMockUser(username = "dash_user1@test.com", roles = {"NORMAL_USER"})
    void testRestGetAlerts() throws Exception {
        mockMvc.perform(get("/api/dashboard/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].title", anyOf(is("Brake Pad Inspection"), is("Tyre Pressure & Rotation"))));
    }

    @Test
    @DisplayName("22. REST: GET /api/dashboard/recent-services Returns Latest User Services")
    @WithMockUser(username = "dash_user1@test.com", roles = {"NORMAL_USER"})
    void testRestGetRecentServices() throws Exception {
        mockMvc.perform(get("/api/dashboard/recent-services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("23. REST: GET /api/dashboard/recent-fuel Returns Latest User Fuel Logs")
    @WithMockUser(username = "dash_user1@test.com", roles = {"NORMAL_USER"})
    void testRestGetRecentFuel() throws Exception {
        mockMvc.perform(get("/api/dashboard/recent-fuel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("24. REST: Unauthenticated API Access Blocked (Redirected to /login)")
    void testRestUnauthenticatedBlocked() throws Exception {
        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("25. REST: ADMIN Role Forbidden from /api/dashboard (403 Forbidden)")
    @WithMockUser(username = "dash_admin@test.com", roles = {"ADMIN"})
    void testRestAdminForbiddenFromUserDashboard() throws Exception {
        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("26. REST: NORMAL_USER Forbidden from /api/admin/statistics (403 Forbidden)")
    @WithMockUser(username = "dash_user1@test.com", roles = {"NORMAL_USER"})
    void testRestUserForbiddenFromAdminStatistics() throws Exception {
        mockMvc.perform(get("/api/admin/statistics"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("27. REST: ADMIN Role Can Access /api/admin/statistics (200 OK)")
    @WithMockUser(username = "dash_admin@test.com", roles = {"ADMIN"})
    void testRestAdminCanAccessStatistics() throws Exception {
        mockMvc.perform(get("/api/admin/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVehicles").value(3))
                .andExpect(jsonPath("$.totalServiceRecords").value(2))
                .andExpect(jsonPath("$.totalFuelRecords").value(2))
                .andExpect(jsonPath("$.totalMaintenanceRecords").value(3));
    }
}