package com.mygarage.records;

import com.mygarage.model.*;
import com.mygarage.model.enums.FuelType;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Milestone 11 (M11) — Global Record Logs, Cross-Vehicle Aggregation & System Monitoring Module Test Suite.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GlobalRecordsModuleTest {

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

    private User userA;
    private User userB;
    private VehicleCategory category;
    private Vehicle vehicleA1;
    private Vehicle vehicleA2;
    private Vehicle vehicleB1;
    private MaintenanceRecord maintA;
    private MaintenanceRecord maintB;

    @BeforeEach
    void setUp() {
        maintenanceRecordRepository.deleteAll();
        serviceRecordRepository.deleteAll();
        fuelRecordRepository.deleteAll();
        vehicleRepository.deleteAll();

        // Setup Admin
        User adminUser = userRepository.findByEmail("admin@mygarage.com").orElseGet(User::new);
        adminUser.setFullName("System Administrator");
        adminUser.setEmail("admin@mygarage.com");
        adminUser.setPasswordHash(passwordEncoder.encode("Admin@123"));
        adminUser.setRole(Role.ADMIN);
        adminUser.setActive(true);
        userRepository.save(adminUser);

        // Setup User A
        userA = userRepository.findByEmail("m11_user_a@test.com").orElseGet(User::new);
        userA.setFullName("User A M11");
        userA.setEmail("m11_user_a@test.com");
        userA.setPasswordHash(passwordEncoder.encode("Pass@123"));
        userA.setRole(Role.NORMAL_USER);
        userA.setActive(true);
        userA = userRepository.save(userA);

        // Setup User B
        userB = userRepository.findByEmail("m11_user_b@test.com").orElseGet(User::new);
        userB.setFullName("User B M11");
        userB.setEmail("m11_user_b@test.com");
        userB.setPasswordHash(passwordEncoder.encode("Pass@123"));
        userB.setRole(Role.NORMAL_USER);
        userB.setActive(true);
        userB = userRepository.save(userB);

        // Category
        category = categoryRepository.findByNameIgnoreCase("Sedan M11").orElseGet(() -> {
            VehicleCategory c = new VehicleCategory();
            c.setName("Sedan M11");
            c.setIcon("🚗");
            return categoryRepository.save(c);
        });

        // Vehicles for User A
        vehicleA1 = new Vehicle();
        vehicleA1.setUser(userA);
        vehicleA1.setCategory(category);
        vehicleA1.setPlateNumber("KA01M11A1");
        vehicleA1.setMake("Honda");
        vehicleA1.setModel("City");
        vehicleA1.setYear(2022);
        vehicleA1.setCurrentOdometer(20000);
        vehicleA1.setFuelType("PETROL");
        vehicleA1 = vehicleRepository.save(vehicleA1);

        vehicleA2 = new Vehicle();
        vehicleA2.setUser(userA);
        vehicleA2.setCategory(category);
        vehicleA2.setPlateNumber("KA01M11A2");
        vehicleA2.setMake("Hyundai");
        vehicleA2.setModel("Verna");
        vehicleA2.setYear(2023);
        vehicleA2.setCurrentOdometer(10000);
        vehicleA2.setFuelType("DIESEL");
        vehicleA2 = vehicleRepository.save(vehicleA2);

        // Vehicle for User B
        vehicleB1 = new Vehicle();
        vehicleB1.setUser(userB);
        vehicleB1.setCategory(category);
        vehicleB1.setPlateNumber("KA01M11B1");
        vehicleB1.setMake("Toyota");
        vehicleB1.setModel("Corolla");
        vehicleB1.setYear(2021);
        vehicleB1.setCurrentOdometer(30000);
        vehicleB1.setFuelType("PETROL");
        vehicleB1 = vehicleRepository.save(vehicleB1);

        // Service records for User A
        ServiceRecord s1 = new ServiceRecord();
        s1.setVehicle(vehicleA1);
        s1.setServiceDate(LocalDate.now().minusDays(10));
        s1.setServiceType("Oil Change");
        s1.setGarageName("Honda Express");
        s1.setCost(new BigDecimal("2500.00"));
        serviceRecordRepository.save(s1);

        ServiceRecord s2 = new ServiceRecord();
        s2.setVehicle(vehicleA2);
        s2.setServiceDate(LocalDate.now().minusDays(5));
        s2.setServiceType("Brake Pad Replacement");
        s2.setGarageName("Hyundai Care");
        s2.setCost(new BigDecimal("4000.00"));
        serviceRecordRepository.save(s2);

        // Service record for User B
        ServiceRecord sB = new ServiceRecord();
        sB.setVehicle(vehicleB1);
        sB.setServiceDate(LocalDate.now().minusDays(2));
        sB.setServiceType("Tire Replacement");
        sB.setGarageName("Toyota Center");
        sB.setCost(new BigDecimal("12000.00"));
        serviceRecordRepository.save(sB);

        // Fuel records for User A
        FuelRecord f1 = new FuelRecord();
        f1.setVehicle(vehicleA1);
        f1.setFuelDate(LocalDate.now().minusDays(8));
        f1.setFuelType(FuelType.PETROL);
        f1.setQuantityLitres(new BigDecimal("35.00"));
        f1.setCostPerLitre(new BigDecimal("100.00"));
        f1.setTotalCost(new BigDecimal("3500.00"));
        f1.setOdometerAtFill(19500);
        fuelRecordRepository.save(f1);

        FuelRecord f2 = new FuelRecord();
        f2.setVehicle(vehicleA2);
        f2.setFuelDate(LocalDate.now().minusDays(3));
        f2.setFuelType(FuelType.DIESEL);
        f2.setQuantityLitres(new BigDecimal("40.00"));
        f2.setCostPerLitre(new BigDecimal("90.00"));
        f2.setTotalCost(new BigDecimal("3600.00"));
        f2.setOdometerAtFill(9800);
        fuelRecordRepository.save(f2);

        // Fuel record for User B
        FuelRecord fB = new FuelRecord();
        fB.setVehicle(vehicleB1);
        fB.setFuelDate(LocalDate.now().minusDays(1));
        fB.setFuelType(FuelType.PETROL);
        fB.setQuantityLitres(new BigDecimal("50.00"));
        fB.setCostPerLitre(new BigDecimal("100.00"));
        fB.setTotalCost(new BigDecimal("5000.00"));
        fB.setOdometerAtFill(29800);
        fuelRecordRepository.save(fB);

        // Maintenance tasks
        maintA = new MaintenanceRecord();
        maintA.setVehicle(vehicleA1);
        maintA.setTitle("Air Filter Change");
        maintA.setScheduledDate(LocalDate.now().minusDays(2)); // Overdue
        maintA.setStatus(MaintenanceStatus.OVERDUE);
        maintA.setCost(new BigDecimal("600.00"));
        maintA = maintenanceRecordRepository.save(maintA);

        MaintenanceRecord maintA2 = new MaintenanceRecord();
        maintA2.setVehicle(vehicleA2);
        maintA2.setTitle("Coolant Check");
        maintA2.setScheduledDate(LocalDate.now().plusWeeks(2)); // Upcoming
        maintA2.setStatus(MaintenanceStatus.UPCOMING);
        maintA2.setCost(new BigDecimal("800.00"));
        maintenanceRecordRepository.save(maintA2);

        maintB = new MaintenanceRecord();
        maintB.setVehicle(vehicleB1);
        maintB.setTitle("Battery Check");
        maintB.setScheduledDate(LocalDate.now().plusDays(5));
        maintB.setStatus(MaintenanceStatus.UPCOMING);
        maintB.setCost(new BigDecimal("1500.00"));
        maintB = maintenanceRecordRepository.save(maintB);
    }

    // =========================================================================
    // SECTION 1: WEB MVC FUNCTIONALITY & CROSS-VEHICLE AGGREGATION
    // =========================================================================

    @Test
    @DisplayName("1. Web: /services lists all services across all vehicles for User A")
    @WithMockUser(username = "m11_user_a@test.com", roles = {"NORMAL_USER"})
    void testAllServicesView() throws Exception {
        mockMvc.perform(get("/services"))
                .andExpect(status().isOk())
                .andExpect(view().name("service/index"))
                .andExpect(model().attribute("services", hasSize(2)))
                .andExpect(model().attribute("totalCost", is(new BigDecimal("6500.00"))));
    }

    @Test
    @DisplayName("2. Web: /services search keyword filters services")
    @WithMockUser(username = "m11_user_a@test.com", roles = {"NORMAL_USER"})
    void testAllServicesSearch() throws Exception {
        mockMvc.perform(get("/services").param("search", "Brake"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("services", hasSize(1)))
                .andExpect(model().attribute("services", hasItem(hasProperty("serviceType", is("Brake Pad Replacement")))));
    }

    @Test
    @DisplayName("3. Web: /fuel lists all fuel records across all vehicles for User A")
    @WithMockUser(username = "m11_user_a@test.com", roles = {"NORMAL_USER"})
    void testAllFuelView() throws Exception {
        mockMvc.perform(get("/fuel"))
                .andExpect(status().isOk())
                .andExpect(view().name("fuel/index"))
                .andExpect(model().attribute("fuelRecords", hasSize(2)))
                .andExpect(model().attribute("totalCost", is(new BigDecimal("7100.00"))))
                .andExpect(model().attribute("totalLitres", is(new BigDecimal("75.00"))));
    }

    @Test
    @DisplayName("4. Web: /maintenance lists all maintenance tasks across all vehicles for User A")
    @WithMockUser(username = "m11_user_a@test.com", roles = {"NORMAL_USER"})
    void testAllMaintenanceView() throws Exception {
        mockMvc.perform(get("/maintenance"))
                .andExpect(status().isOk())
                .andExpect(view().name("maintenance/index"))
                .andExpect(model().attribute("tasks", hasSize(2)))
                .andExpect(model().attribute("overdueCount", is(1L)))
                .andExpect(model().attribute("upcomingCount", is(1L)));
    }

    @Test
    @DisplayName("5. Web: /maintenance status filter filters tasks")
    @WithMockUser(username = "m11_user_a@test.com", roles = {"NORMAL_USER"})
    void testAllMaintenanceStatusFilter() throws Exception {
        mockMvc.perform(get("/maintenance").param("status", "OVERDUE"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("tasks", hasSize(1)))
                .andExpect(model().attribute("tasks", hasItem(hasProperty("title", is("Air Filter Change")))));
    }

    @Test
    @DisplayName("6. Web: /maintenance/{id}/complete marks task completed with CSRF")
    @WithMockUser(username = "m11_user_a@test.com", roles = {"NORMAL_USER"})
    void testCompleteMaintenanceCentralized() throws Exception {
        mockMvc.perform(post("/maintenance/" + maintA.getMaintenanceId() + "/complete")
                .with(csrf())
                .param("completedDate", LocalDate.now().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/maintenance"))
                .andExpect(flash().attributeExists("successMsg"));

        MaintenanceRecord updated = maintenanceRecordRepository.findById(maintA.getMaintenanceId()).orElseThrow();
        assertEquals(MaintenanceStatus.COMPLETED, updated.getStatus());
    }

    @Test
    @DisplayName("7. Web: /admin/records renders platform record monitoring for ADMIN")
    @WithMockUser(username = "admin@mygarage.com", roles = {"ADMIN"})
    void testAdminRecordMonitoringView() throws Exception {
        mockMvc.perform(get("/admin/records"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/records"))
                .andExpect(model().attributeExists("monitoring"))
                .andExpect(model().attributeExists("categories"));
    }

    // =========================================================================
    // SECTION 2: SECURITY, RBAC & PRIVACY ISOLATION
    // =========================================================================

    @Test
    @DisplayName("8. Security: NORMAL_USER blocked from /admin/records (403)")
    @WithMockUser(username = "m11_user_a@test.com", roles = {"NORMAL_USER"})
    void testNormalUserBlockedFromAdminRecords() throws Exception {
        mockMvc.perform(get("/admin/records"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("9. Security: ADMIN blocked from user garage routes (/services, /fuel, /maintenance)")
    @WithMockUser(username = "admin@mygarage.com", roles = {"ADMIN"})
    void testAdminBlockedFromUserRoutes() throws Exception {
        mockMvc.perform(get("/services")).andExpect(status().isForbidden());
        mockMvc.perform(get("/fuel")).andExpect(status().isForbidden());
        mockMvc.perform(get("/maintenance")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("10. Security: Cross-user isolation in /services (User A cannot see User B's service)")
    @WithMockUser(username = "m11_user_a@test.com", roles = {"NORMAL_USER"})
    void testCrossUserIsolationServices() throws Exception {
        mockMvc.perform(get("/services"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("services", not(hasItem(hasProperty("serviceType", is("Tire Replacement"))))));
    }

    @Test
    @DisplayName("11. Security: Cross-user isolation in /fuel (User A cannot see User B's fuel)")
    @WithMockUser(username = "m11_user_a@test.com", roles = {"NORMAL_USER"})
    void testCrossUserIsolationFuel() throws Exception {
        mockMvc.perform(get("/fuel"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("fuelRecords", not(hasItem(hasProperty("odometerAtFill", is(29800))))));
    }

    @Test
    @DisplayName("12. Security: Cross-user isolation in /maintenance (User A cannot see User B's tasks)")
    @WithMockUser(username = "m11_user_a@test.com", roles = {"NORMAL_USER"})
    void testCrossUserIsolationMaintenance() throws Exception {
        mockMvc.perform(get("/maintenance"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("tasks", not(hasItem(hasProperty("title", is("Battery Check"))))));
    }

    @Test
    @DisplayName("13. Security: Tampering rejected (User A cannot complete User B's task)")
    @WithMockUser(username = "m11_user_a@test.com", roles = {"NORMAL_USER"})
    void testCrossUserCompleteTamperingRejected() throws Exception {
        mockMvc.perform(post("/maintenance/" + maintB.getMaintenanceId() + "/complete")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/maintenance"))
                .andExpect(flash().attributeExists("errorMsg"));

        MaintenanceRecord unaltered = maintenanceRecordRepository.findById(maintB.getMaintenanceId()).orElseThrow();
        assertEquals(MaintenanceStatus.UPCOMING, unaltered.getStatus());
    }

    // =========================================================================
    // SECTION 3: REST API FUNCTIONALITY & RBAC
    // =========================================================================

    @Test
    @DisplayName("14. REST: GET /api/services returns user's services across all vehicles")
    @WithMockUser(username = "m11_user_a@test.com", roles = {"NORMAL_USER"})
    void testRestAllServices() throws Exception {
        mockMvc.perform(get("/api/services"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("15. REST: GET /api/services?search=Brake returns filtered results")
    @WithMockUser(username = "m11_user_a@test.com", roles = {"NORMAL_USER"})
    void testRestAllServicesSearch() throws Exception {
        mockMvc.perform(get("/api/services").param("search", "Brake"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].serviceType").value("Brake Pad Replacement"));
    }

    @Test
    @DisplayName("16. REST: GET /api/fuel returns user's fuel logs across all vehicles")
    @WithMockUser(username = "m11_user_a@test.com", roles = {"NORMAL_USER"})
    void testRestAllFuel() throws Exception {
        mockMvc.perform(get("/api/fuel"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("17. REST: GET /api/maintenance returns user's maintenance tasks")
    @WithMockUser(username = "m11_user_a@test.com", roles = {"NORMAL_USER"})
    void testRestAllMaintenance() throws Exception {
        mockMvc.perform(get("/api/maintenance"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("18. REST: GET /api/maintenance?status=OVERDUE returns filtered status")
    @WithMockUser(username = "m11_user_a@test.com", roles = {"NORMAL_USER"})
    void testRestAllMaintenanceStatusFilter() throws Exception {
        mockMvc.perform(get("/api/maintenance").param("status", "OVERDUE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Air Filter Change"));
    }

    @Test
    @DisplayName("19. REST: GET /api/admin/records returns platform monitoring summary")
    @WithMockUser(username = "admin@mygarage.com", roles = {"ADMIN"})
    void testRestAdminRecordMonitoring() throws Exception {
        mockMvc.perform(get("/api/admin/records"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalVehicles").exists())
                .andExpect(jsonPath("$.totalServiceRecords").exists())
                .andExpect(jsonPath("$.totalFuelRecords").exists())
                .andExpect(jsonPath("$.totalMaintenanceRecords").exists());
    }

    @Test
    @DisplayName("20. REST: NORMAL_USER blocked from /api/admin/records (403)")
    @WithMockUser(username = "m11_user_a@test.com", roles = {"NORMAL_USER"})
    void testRestNormalUserBlockedFromAdminRecords() throws Exception {
        mockMvc.perform(get("/api/admin/records"))
                .andExpect(status().isForbidden());
    }
}