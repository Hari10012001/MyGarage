package com.mygarage.report;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Milestone 12 (M12) — Vehicle Resale Dossier, Data Export & Advanced Reporting Module Test Suite.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class VehicleReportAndExportModuleTest {

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
    private Vehicle vehicleA;
    private Vehicle vehicleB;
    private ServiceRecord serviceA;
    private FuelRecord fuelA;
    private MaintenanceRecord maintA;

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
        userA = userRepository.findByEmail("m12_user_a@test.com").orElseGet(User::new);
        userA.setFullName("Alice Johnson");
        userA.setEmail("m12_user_a@test.com");
        userA.setPasswordHash(passwordEncoder.encode("Pass@123"));
        userA.setPhone("9876543210");
        userA.setRole(Role.NORMAL_USER);
        userA.setActive(true);
        userA = userRepository.save(userA);

        // User B
        userB = userRepository.findByEmail("m12_user_b@test.com").orElseGet(User::new);
        userB.setFullName("Bob Smith");
        userB.setEmail("m12_user_b@test.com");
        userB.setPasswordHash(passwordEncoder.encode("Pass@123"));
        userB.setPhone("9876543211");
        userB.setRole(Role.NORMAL_USER);
        userB.setActive(true);
        userB = userRepository.save(userB);

        // Category
        VehicleCategory cat = categoryRepository.findByNameIgnoreCase("Sedan M12").orElseGet(() -> {
            VehicleCategory c = new VehicleCategory();
            c.setName("Sedan M12");
            c.setIcon("🚗");
            return categoryRepository.save(c);
        });

        // Vehicle A
        vehicleA = new Vehicle();
        vehicleA.setUser(userA);
        vehicleA.setCategory(cat);
        vehicleA.setPlateNumber("TN01M12A");
        vehicleA.setMake("Toyota");
        vehicleA.setModel("Camry");
        vehicleA.setYear(2022);
        vehicleA.setColor("Silver");
        vehicleA.setCurrentOdometer(35000);
        vehicleA.setFuelType("PETROL");
        vehicleA = vehicleRepository.save(vehicleA);

        // Vehicle B
        vehicleB = new Vehicle();
        vehicleB.setUser(userB);
        vehicleB.setCategory(cat);
        vehicleB.setPlateNumber("TN01M12B");
        vehicleB.setMake("Honda");
        vehicleB.setModel("Accord");
        vehicleB.setYear(2023);
        vehicleB.setColor("Black");
        vehicleB.setCurrentOdometer(15000);
        vehicleB.setFuelType("HYBRID");
        vehicleB = vehicleRepository.save(vehicleB);

        // Service for Vehicle A
        serviceA = new ServiceRecord();
        serviceA.setVehicle(vehicleA);
        serviceA.setServiceDate(LocalDate.now().minusMonths(2));
        serviceA.setServiceType("Full Synthetic Oil Service");
        serviceA.setGarageName("Toyota Authorised Service");
        serviceA.setCost(new BigDecimal("4500.00"));
        serviceA.setOdometerAtService(30000);
        serviceA.setDescription("Replaced oil, oil filter, air filter");
        serviceA = serviceRecordRepository.save(serviceA);

        // Fuel for Vehicle A
        fuelA = new FuelRecord();
        fuelA.setVehicle(vehicleA);
        fuelA.setFuelDate(LocalDate.now().minusDays(10));
        fuelA.setFuelType(com.mygarage.model.enums.FuelType.PETROL);
        fuelA.setQuantityLitres(new BigDecimal("40.00"));
        fuelA.setCostPerLitre(new BigDecimal("102.50"));
        fuelA.setTotalCost(new BigDecimal("4100.00"));
        fuelA.setOdometerAtFill(34500);
        fuelA.setEstimatedMileageKmpl(new BigDecimal("16.50"));
        fuelA = fuelRecordRepository.save(fuelA);

        // Maintenance for Vehicle A
        maintA = new MaintenanceRecord();
        maintA.setVehicle(vehicleA);
        maintA.setTitle("Brake Fluid Flush");
        maintA.setDescription("Replace DOT 4 brake fluid");
        maintA.setScheduledDate(LocalDate.now().plusMonths(1));
        maintA.setStatus(MaintenanceStatus.UPCOMING);
        maintA.setCost(new BigDecimal("1200.00"));
        maintA = maintenanceRecordRepository.save(maintA);
    }

    // =========================================================================
    // SECTION 1: WEB MVC ROUTES & REPORTS HUB
    // =========================================================================

    @Test
    @DisplayName("1. Web: /reports renders Reports & Export Hub for NORMAL_USER")
    @WithMockUser(username = "m12_user_a@test.com", roles = {"NORMAL_USER"})
    void testReportsHubAsNormalUser() throws Exception {
        mockMvc.perform(get("/reports"))
                .andExpect(status().isOk())
                .andExpect(view().name("report/index"))
                .andExpect(model().attributeExists("summary"))
                .andExpect(model().attributeExists("vehicles"))
                .andExpect(model().attribute("summary", hasProperty("totalVehicles", is(1L))))
                .andExpect(model().attribute("summary", hasProperty("totalGarageCost", is(new BigDecimal("9800.00")))));
    }

    @Test
    @DisplayName("2. Web: /vehicles/{id}/report renders print-ready Vehicle Resale Dossier for owner")
    @WithMockUser(username = "m12_user_a@test.com", roles = {"NORMAL_USER"})
    void testVehicleDossierAsOwner() throws Exception {
        mockMvc.perform(get("/vehicles/" + vehicleA.getVehicleId() + "/report"))
                .andExpect(status().isOk())
                .andExpect(view().name("report/dossier"))
                .andExpect(model().attributeExists("dossier"))
                .andExpect(model().attribute("dossier", hasProperty("plateNumber", is("TN01M12A"))))
                .andExpect(model().attribute("dossier", hasProperty("ownerName", is("Alice Johnson"))))
                .andExpect(model().attribute("dossier", hasProperty("totalServiceCost", is(new BigDecimal("4500.00")))))
                .andExpect(model().attribute("dossier", hasProperty("totalFuelCost", is(new BigDecimal("4100.00")))))
                .andExpect(model().attribute("dossier", hasProperty("totalMaintenanceCost", is(new BigDecimal("1200.00")))))
                .andExpect(model().attribute("dossier", hasProperty("totalOwnershipCost", is(new BigDecimal("9800.00")))));
    }

    @Test
    @DisplayName("3. Web: Cross-user dossier access is blocked with 403 Forbidden")
    @WithMockUser(username = "m12_user_b@test.com", roles = {"NORMAL_USER"})
    void testVehicleDossierCrossUserForbidden() throws Exception {
        mockMvc.perform(get("/vehicles/" + vehicleA.getVehicleId() + "/report"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("4. Web: Dossier for non-existent vehicle returns 400 Bad Request")
    @WithMockUser(username = "m12_user_a@test.com", roles = {"NORMAL_USER"})
    void testVehicleDossierNonExistentBadRequest() throws Exception {
        mockMvc.perform(get("/vehicles/999999/report"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error/error"));
    }

    @Test
    @DisplayName("5. Web: Export Services CSV as owner returns 200 OK and text/csv")
    @WithMockUser(username = "m12_user_a@test.com", roles = {"NORMAL_USER"})
    void testExportServicesCsvAsOwner() throws Exception {
        mockMvc.perform(get("/vehicles/" + vehicleA.getVehicleId() + "/export/services/csv"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv;charset=UTF-8"))
                .andExpect(header().string("Content-Disposition", containsString("vehicle-" + vehicleA.getVehicleId() + "-services.csv")))
                .andExpect(content().string(containsString("Service ID,Vehicle Plate,Service Date,Service Type")))
                .andExpect(content().string(containsString("Full Synthetic Oil Service")))
                .andExpect(content().string(containsString("Toyota Authorised Service")));
    }

    @Test
    @DisplayName("6. Web: Export Fuel CSV as owner returns 200 OK and text/csv")
    @WithMockUser(username = "m12_user_a@test.com", roles = {"NORMAL_USER"})
    void testExportFuelCsvAsOwner() throws Exception {
        mockMvc.perform(get("/vehicles/" + vehicleA.getVehicleId() + "/export/fuel/csv"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv;charset=UTF-8"))
                .andExpect(header().string("Content-Disposition", containsString("vehicle-" + vehicleA.getVehicleId() + "-fuel.csv")))
                .andExpect(content().string(containsString("Fuel ID,Vehicle Plate,Fuel Date,Fuel Type")))
                .andExpect(content().string(containsString("40.00")))
                .andExpect(content().string(containsString("4100.00")));
    }

    @Test
    @DisplayName("7. Web: Export Maintenance CSV as owner returns 200 OK and text/csv")
    @WithMockUser(username = "m12_user_a@test.com", roles = {"NORMAL_USER"})
    void testExportMaintenanceCsvAsOwner() throws Exception {
        mockMvc.perform(get("/vehicles/" + vehicleA.getVehicleId() + "/export/maintenance/csv"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv;charset=UTF-8"))
                .andExpect(header().string("Content-Disposition", containsString("vehicle-" + vehicleA.getVehicleId() + "-maintenance.csv")))
                .andExpect(content().string(containsString("Maintenance ID,Vehicle Plate,Title,Status")))
                .andExpect(content().string(containsString("Brake Fluid Flush")));
    }

    @Test
    @DisplayName("8. Web: Export Master All History CSV as owner returns 200 OK and text/csv")
    @WithMockUser(username = "m12_user_a@test.com", roles = {"NORMAL_USER"})
    void testExportMasterAllCsvAsOwner() throws Exception {
        mockMvc.perform(get("/vehicles/" + vehicleA.getVehicleId() + "/export/all/csv"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv;charset=UTF-8"))
                .andExpect(header().string("Content-Disposition", containsString("vehicle-" + vehicleA.getVehicleId() + "-master-history.csv")))
                .andExpect(content().string(containsString("Record Type,Record ID,Vehicle Plate,Date,Title / Type")))
                .andExpect(content().string(containsString("SERVICE")))
                .andExpect(content().string(containsString("FUEL")))
                .andExpect(content().string(containsString("MAINTENANCE")));
    }

    @Test
    @DisplayName("9. Web: Export Garage Summary CSV for user returns 200 OK and text/csv")
    @WithMockUser(username = "m12_user_a@test.com", roles = {"NORMAL_USER"})
    void testExportGarageCsvAsNormalUser() throws Exception {
        mockMvc.perform(get("/export/garage/csv"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv;charset=UTF-8"))
                .andExpect(header().string("Content-Disposition", containsString("garage-portfolio-summary.csv")))
                .andExpect(content().string(containsString("Vehicle ID,Plate Number,Make,Model,Year")))
                .andExpect(content().string(containsString("TN01M12A")))
                .andExpect(content().string(containsString("Toyota")));
    }

    // =========================================================================
    // SECTION 2: OWNERSHIP ISOLATION & TAMPERING PROTECTION
    // =========================================================================

    @Test
    @DisplayName("10. Security: Cross-user export services CSV blocked with 403 Forbidden")
    @WithMockUser(username = "m12_user_b@test.com", roles = {"NORMAL_USER"})
    void testExportServicesCsvCrossUserForbidden() throws Exception {
        mockMvc.perform(get("/vehicles/" + vehicleA.getVehicleId() + "/export/services/csv"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("11. Security: Cross-user export fuel CSV blocked with 403 Forbidden")
    @WithMockUser(username = "m12_user_b@test.com", roles = {"NORMAL_USER"})
    void testExportFuelCsvCrossUserForbidden() throws Exception {
        mockMvc.perform(get("/vehicles/" + vehicleA.getVehicleId() + "/export/fuel/csv"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("12. Security: Cross-user export maintenance CSV blocked with 403 Forbidden")
    @WithMockUser(username = "m12_user_b@test.com", roles = {"NORMAL_USER"})
    void testExportMaintenanceCsvCrossUserForbidden() throws Exception {
        mockMvc.perform(get("/vehicles/" + vehicleA.getVehicleId() + "/export/maintenance/csv"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("13. Security: Cross-user export master all CSV blocked with 403 Forbidden")
    @WithMockUser(username = "m12_user_b@test.com", roles = {"NORMAL_USER"})
    void testExportMasterAllCsvCrossUserForbidden() throws Exception {
        mockMvc.perform(get("/vehicles/" + vehicleA.getVehicleId() + "/export/all/csv"))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // SECTION 3: REST API ENDPOINTS
    // =========================================================================

    @Test
    @DisplayName("14. REST: GET /api/reports/garage-summary returns portfolio metrics")
    @WithMockUser(username = "m12_user_a@test.com", roles = {"NORMAL_USER"})
    void testRestApiGarageSummary() throws Exception {
        mockMvc.perform(get("/api/reports/garage-summary"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalVehicles").value(1))
                .andExpect(jsonPath("$.totalServices").value(1))
                .andExpect(jsonPath("$.totalFuelLogs").value(1))
                .andExpect(jsonPath("$.totalMaintenanceTasks").value(1))
                .andExpect(jsonPath("$.totalGarageCost").value(9800.00))
                .andExpect(jsonPath("$.vehicleBreakdown", hasSize(1)))
                .andExpect(jsonPath("$.vehicleBreakdown[0].plateNumber").value("TN01M12A"));
    }

    @Test
    @DisplayName("15. REST: GET /api/vehicles/{id}/export/summary returns vehicle dossier JSON")
    @WithMockUser(username = "m12_user_a@test.com", roles = {"NORMAL_USER"})
    void testRestApiVehicleDossierSummary() throws Exception {
        mockMvc.perform(get("/api/vehicles/" + vehicleA.getVehicleId() + "/export/summary"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.vehicleId").value(vehicleA.getVehicleId()))
                .andExpect(jsonPath("$.plateNumber").value("TN01M12A"))
                .andExpect(jsonPath("$.make").value("Toyota"))
                .andExpect(jsonPath("$.totalOwnershipCost").value(9800.00))
                .andExpect(jsonPath("$.totalServicesCount").value(1))
                .andExpect(jsonPath("$.services", hasSize(1)))
                .andExpect(jsonPath("$.fuelLogs", hasSize(1)))
                .andExpect(jsonPath("$.maintenanceRecords", hasSize(1)));
    }

    @Test
    @DisplayName("16. REST: GET /api/vehicles/{id}/export/summary cross-user blocked with 403")
    @WithMockUser(username = "m12_user_b@test.com", roles = {"NORMAL_USER"})
    void testRestApiVehicleDossierSummaryCrossUserForbidden() throws Exception {
        mockMvc.perform(get("/api/vehicles/" + vehicleA.getVehicleId() + "/export/summary"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("17. REST: GET /api/vehicles/{id}/export/summary non-existent vehicle returns 400")
    @WithMockUser(username = "m12_user_a@test.com", roles = {"NORMAL_USER"})
    void testRestApiVehicleDossierNonExistent() throws Exception {
        mockMvc.perform(get("/api/vehicles/999999/export/summary"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("18. REST: GET /api/vehicles/{id}/export/services returns CSV stream")
    @WithMockUser(username = "m12_user_a@test.com", roles = {"NORMAL_USER"})
    void testRestApiExportServices() throws Exception {
        mockMvc.perform(get("/api/vehicles/" + vehicleA.getVehicleId() + "/export/services"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv;charset=UTF-8"))
                .andExpect(content().string(containsString("Full Synthetic Oil Service")));
    }

    @Test
    @DisplayName("19. REST: GET /api/vehicles/{id}/export/fuel returns CSV stream")
    @WithMockUser(username = "m12_user_a@test.com", roles = {"NORMAL_USER"})
    void testRestApiExportFuel() throws Exception {
        mockMvc.perform(get("/api/vehicles/" + vehicleA.getVehicleId() + "/export/fuel"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv;charset=UTF-8"))
                .andExpect(content().string(containsString("4100.00")));
    }

    @Test
    @DisplayName("20. REST: GET /api/vehicles/{id}/export/maintenance returns CSV stream")
    @WithMockUser(username = "m12_user_a@test.com", roles = {"NORMAL_USER"})
    void testRestApiExportMaintenance() throws Exception {
        mockMvc.perform(get("/api/vehicles/" + vehicleA.getVehicleId() + "/export/maintenance"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv;charset=UTF-8"))
                .andExpect(content().string(containsString("Brake Fluid Flush")));
    }

    @Test
    @DisplayName("21. REST: GET /api/vehicles/{id}/export/all returns CSV stream")
    @WithMockUser(username = "m12_user_a@test.com", roles = {"NORMAL_USER"})
    void testRestApiExportAll() throws Exception {
        mockMvc.perform(get("/api/vehicles/" + vehicleA.getVehicleId() + "/export/all"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv;charset=UTF-8"))
                .andExpect(content().string(containsString("Record Type,Record ID")));
    }

    // =========================================================================
    // SECTION 4: ROLE-BASED ACCESS CONTROL & UNAUTHENTICATED PROTECTION
    // =========================================================================

    @Test
    @DisplayName("22. RBAC: ADMIN blocked from /reports (403)")
    @WithMockUser(username = "admin@mygarage.com", roles = {"ADMIN"})
    void testAdminBlockedFromUserReports() throws Exception {
        mockMvc.perform(get("/reports"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("23. RBAC: ADMIN blocked from /vehicles/{id}/report (403)")
    @WithMockUser(username = "admin@mygarage.com", roles = {"ADMIN"})
    void testAdminBlockedFromVehicleDossier() throws Exception {
        mockMvc.perform(get("/vehicles/" + vehicleA.getVehicleId() + "/report"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("24. RBAC: ADMIN blocked from /api/reports/garage-summary (403)")
    @WithMockUser(username = "admin@mygarage.com", roles = {"ADMIN"})
    void testAdminBlockedFromApiReports() throws Exception {
        mockMvc.perform(get("/api/reports/garage-summary"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("25. Security: Unauthenticated access to /reports redirects to /login")
    void testUnauthenticatedRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/reports"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("26. Security: Unauthenticated access to /api/reports/garage-summary blocked")
    void testUnauthenticatedApiBlocked() throws Exception {
        mockMvc.perform(get("/api/reports/garage-summary"))
                .andExpect(status().is3xxRedirection()); // Form login redirects unauthenticated web requests
    }
}
