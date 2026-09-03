package com.mygarage.maintenance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mygarage.dto.request.MaintenanceRequest;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Milestone 7 (M7) — Comprehensive Maintenance Module Test Suite.
 * Covers 30 test cases:
 * 1. Web: Maintenance task listing for vehicle
 * 2. Web: Empty state display
 * 3. Web: Render Add Maintenance form
 * 4. Web: Add Maintenance record success
 * 5. Web: Validation failure (missing required title/date)
 * 6. Web: Negative cost rejected
 * 7. Status: Computed as OVERDUE for past date
 * 8. Status: Computed as DUE_TODAY for today's date
 * 9. Status: Computed as UPCOMING for future date
 * 10. Web: Mark Maintenance completed
 * 11. Web: Render Edit Maintenance form
 * 12. Web: Update Maintenance record success
 * 13. Web: Delete Maintenance record success
 * 14. Ownership: Same-user access allowed
 * 15. Ownership: Cross-user maintenance view blocked
 * 16. Ownership: Cross-user maintenance creation blocked
 * 17. Ownership: Cross-user maintenance update blocked
 * 18. Ownership: Cross-user maintenance complete blocked
 * 19. Ownership: Cross-user maintenance delete blocked
 * 20. Ownership: Mismatched vehicle/maintenance ID blocked (URL tampering)
 * 21. Security: Unauthenticated access redirects to /login
 * 22. Security: ADMIN role cannot access user maintenance endpoints (403 Forbidden)
 * 23. REST: GET /api/vehicles/{id}/maintenance returns list
 * 24. REST: GET /api/maintenance/{id} returns single record
 * 25. REST: POST /api/vehicles/{id}/maintenance adds record (201 Created)
 * 26. REST: PUT /api/maintenance/{id} updates record (200 OK)
 * 27. REST: PATCH /api/maintenance/{id}/complete marks task completed (200 OK)
 * 28. REST: POST /api/maintenance/{id}/complete marks task completed (200 OK)
 * 29. REST: DELETE /api/maintenance/{id} deletes record (200 OK)
 * 30. Ownership & Cascade: REST cross-user blocked & Vehicle deletion cascades to maintenance
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MaintenanceModuleTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VehicleCategoryRepository categoryRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private MaintenanceRecordRepository maintenanceRecordRepository;

    @Autowired
    private ServiceRecordRepository serviceRecordRepository;

    @Autowired
    private FuelRecordRepository fuelRecordRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private User user1;
    private User user2;
    private User admin;
    private VehicleCategory category;
    private Vehicle user1Vehicle;
    private Vehicle user2Vehicle;
    private MaintenanceRecord user1MaintTask;

    @BeforeEach
    void setUp() {
        maintenanceRecordRepository.deleteAll();
        serviceRecordRepository.deleteAll();
        fuelRecordRepository.deleteAll();
        vehicleRepository.deleteAll();

        // 1. User 1
        user1 = userRepository.findByEmail("maint_user1@test.com").orElseGet(User::new);
        user1.setFullName("Maintenance User One");
        user1.setEmail("maint_user1@test.com");
        user1.setPasswordHash(passwordEncoder.encode("Secret@123"));
        user1.setPhone("9876543001");
        user1.setRole(Role.NORMAL_USER);
        user1.setActive(true);
        user1 = userRepository.save(user1);

        // 2. User 2
        user2 = userRepository.findByEmail("maint_user2@test.com").orElseGet(User::new);
        user2.setFullName("Maintenance User Two");
        user2.setEmail("maint_user2@test.com");
        user2.setPasswordHash(passwordEncoder.encode("Secret@123"));
        user2.setPhone("9876543002");
        user2.setRole(Role.NORMAL_USER);
        user2.setActive(true);
        user2 = userRepository.save(user2);

        // 3. Admin
        admin = userRepository.findByEmail("maint_admin@test.com").orElseGet(User::new);
        admin.setFullName("Maintenance Admin");
        admin.setEmail("maint_admin@test.com");
        admin.setPasswordHash(passwordEncoder.encode("Admin@123"));
        admin.setPhone("9999999001");
        admin.setRole(Role.ADMIN);
        admin.setActive(true);
        admin = userRepository.save(admin);

        // 4. Category
        category = categoryRepository.findByNameIgnoreCase("Sedan").orElseGet(() -> {
            VehicleCategory cat = new VehicleCategory();
            cat.setName("Sedan");
            cat.setIcon("🚗");
            return categoryRepository.save(cat);
        });

        // 5. Vehicles
        user1Vehicle = new Vehicle();
        user1Vehicle.setUser(user1);
        user1Vehicle.setCategory(category);
        user1Vehicle.setPlateNumber("TN07MN1001");
        user1Vehicle.setMake("Hyundai");
        user1Vehicle.setModel("Verna");
        user1Vehicle.setYear(2023);
        user1Vehicle.setCurrentOdometer(12000);
        user1Vehicle = vehicleRepository.save(user1Vehicle);

        user2Vehicle = new Vehicle();
        user2Vehicle.setUser(user2);
        user2Vehicle.setCategory(category);
        user2Vehicle.setPlateNumber("TN07MN2002");
        user2Vehicle.setMake("Skoda");
        user2Vehicle.setModel("Slavia");
        user2Vehicle.setYear(2023);
        user2Vehicle.setCurrentOdometer(9000);
        user2Vehicle = vehicleRepository.save(user2Vehicle);

        // 6. Baseline Maintenance Task for User 1
        user1MaintTask = new MaintenanceRecord();
        user1MaintTask.setVehicle(user1Vehicle);
        user1MaintTask.setTitle("Brake Fluid Flush");
        user1MaintTask.setDescription("Check and flush hydraulic brake lines");
        user1MaintTask.setScheduledDate(LocalDate.now().plusDays(15));
        user1MaintTask.setStatus(MaintenanceStatus.UPCOMING);
        user1MaintTask.setCost(new BigDecimal("1500.00"));
        user1MaintTask = maintenanceRecordRepository.save(user1MaintTask);
    }

    // =========================================================================
    // SECTION 1: WEB MVC TESTS
    // =========================================================================

    @Test
    @DisplayName("1. Web: Maintenance Task Listing for Vehicle")
    @WithMockUser(username = "maint_user1@test.com", roles = {"NORMAL_USER"})
    void testWebMaintenanceListing() throws Exception {
        mockMvc.perform(get("/vehicles/" + user1Vehicle.getVehicleId() + "?tab=maintenance"))
                .andExpect(status().isOk())
                .andExpect(view().name("vehicle/detail"))
                .andExpect(model().attributeExists("maintenanceRecords"))
                .andExpect(model().attribute("maintenanceRecords", hasSize(1)));
    }

    @Test
    @DisplayName("2. Web: Empty Maintenance History Display")
    @WithMockUser(username = "maint_user2@test.com", roles = {"NORMAL_USER"})
    void testWebEmptyMaintenanceHistory() throws Exception {
        mockMvc.perform(get("/vehicles/" + user2Vehicle.getVehicleId() + "?tab=maintenance"))
                .andExpect(status().isOk())
                .andExpect(view().name("vehicle/detail"))
                .andExpect(model().attribute("maintenanceRecords", hasSize(0)));
    }

    @Test
    @DisplayName("3. Web: Render Add Maintenance Form")
    @WithMockUser(username = "maint_user1@test.com", roles = {"NORMAL_USER"})
    void testWebAddMaintenanceForm() throws Exception {
        mockMvc.perform(get("/vehicles/" + user1Vehicle.getVehicleId() + "/maintenance/add"))
                .andExpect(status().isOk())
                .andExpect(view().name("maintenance/form"))
                .andExpect(model().attributeExists("maintenanceRequest"))
                .andExpect(model().attributeExists("vehicle"));
    }

    @Test
    @DisplayName("4. Web: Add Maintenance Task Success")
    @WithMockUser(username = "maint_user1@test.com", roles = {"NORMAL_USER"})
    void testWebAddMaintenanceSuccess() throws Exception {
        mockMvc.perform(post("/vehicles/" + user1Vehicle.getVehicleId() + "/maintenance/add")
                .with(csrf())
                .param("title", "Engine Coolant Replacement")
                .param("description", "Drain radiator and fill with 50/50 mix")
                .param("scheduledDate", LocalDate.now().plusMonths(1).toString())
                .param("cost", "1200.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/vehicles/" + user1Vehicle.getVehicleId() + "?tab=maintenance"))
                .andExpect(flash().attributeExists("successMsg"));

        var records = maintenanceRecordRepository.findByVehicleVehicleIdOrderByScheduledDateAsc(user1Vehicle.getVehicleId());
        assertEquals(2, records.size());
    }

    @Test
    @DisplayName("5. Web: Validation Failure (Missing Required Title or Scheduled Date)")
    @WithMockUser(username = "maint_user1@test.com", roles = {"NORMAL_USER"})
    void testWebValidationFailure() throws Exception {
        mockMvc.perform(post("/vehicles/" + user1Vehicle.getVehicleId() + "/maintenance/add")
                .with(csrf())
                .param("title", "") // blank title
                .param("scheduledDate", "")) // blank date
                .andExpect(status().isOk())
                .andExpect(view().name("maintenance/form"))
                .andExpect(model().hasErrors());
    }

    @Test
    @DisplayName("6. Web: Negative Cost is Rejected")
    @WithMockUser(username = "maint_user1@test.com", roles = {"NORMAL_USER"})
    void testWebNegativeCostRejected() throws Exception {
        mockMvc.perform(post("/vehicles/" + user1Vehicle.getVehicleId() + "/maintenance/add")
                .with(csrf())
                .param("title", "Battery Replacement")
                .param("scheduledDate", LocalDate.now().toString())
                .param("cost", "-500.00")) // negative
                .andExpect(status().isOk())
                .andExpect(view().name("maintenance/form"))
                .andExpect(model().attributeHasFieldErrors("maintenanceRequest", "cost"));
    }

    @Test
    @DisplayName("7. Status: Computed as OVERDUE for Past Scheduled Date")
    @WithMockUser(username = "maint_user1@test.com", roles = {"NORMAL_USER"})
    void testStatusComputedAsOverdue() throws Exception {
        mockMvc.perform(post("/vehicles/" + user1Vehicle.getVehicleId() + "/maintenance/add")
                .with(csrf())
                .param("title", "Air Filter Change")
                .param("scheduledDate", LocalDate.now().minusDays(5).toString()))
                .andExpect(status().is3xxRedirection());

        var records = maintenanceRecordRepository.findByVehicleVehicleIdOrderByScheduledDateAsc(user1Vehicle.getVehicleId());
        MaintenanceRecord overdueTask = records.stream()
                .filter(r -> r.getTitle().equals("Air Filter Change"))
                .findFirst().orElseThrow();
        assertEquals(MaintenanceStatus.OVERDUE, overdueTask.getStatus());
    }

    @Test
    @DisplayName("8. Status: Computed as DUE_TODAY for Today's Scheduled Date")
    @WithMockUser(username = "maint_user1@test.com", roles = {"NORMAL_USER"})
    void testStatusComputedAsDueToday() throws Exception {
        mockMvc.perform(post("/vehicles/" + user1Vehicle.getVehicleId() + "/maintenance/add")
                .with(csrf())
                .param("title", "Wiper Blade Change")
                .param("scheduledDate", LocalDate.now().toString()))
                .andExpect(status().is3xxRedirection());

        var records = maintenanceRecordRepository.findByVehicleVehicleIdOrderByScheduledDateAsc(user1Vehicle.getVehicleId());
        MaintenanceRecord dueTodayTask = records.stream()
                .filter(r -> r.getTitle().equals("Wiper Blade Change"))
                .findFirst().orElseThrow();
        assertEquals(MaintenanceStatus.DUE_TODAY, dueTodayTask.getStatus());
    }

    @Test
    @DisplayName("9. Status: Computed as UPCOMING for Future Scheduled Date")
    @WithMockUser(username = "maint_user1@test.com", roles = {"NORMAL_USER"})
    void testStatusComputedAsUpcoming() throws Exception {
        mockMvc.perform(post("/vehicles/" + user1Vehicle.getVehicleId() + "/maintenance/add")
                .with(csrf())
                .param("title", "Timing Belt Inspection")
                .param("scheduledDate", LocalDate.now().plusMonths(3).toString()))
                .andExpect(status().is3xxRedirection());

        var records = maintenanceRecordRepository.findByVehicleVehicleIdOrderByScheduledDateAsc(user1Vehicle.getVehicleId());
        MaintenanceRecord upcomingTask = records.stream()
                .filter(r -> r.getTitle().equals("Timing Belt Inspection"))
                .findFirst().orElseThrow();
        assertEquals(MaintenanceStatus.UPCOMING, upcomingTask.getStatus());
    }

    @Test
    @DisplayName("10. Web: Mark Maintenance Completed")
    @WithMockUser(username = "maint_user1@test.com", roles = {"NORMAL_USER"})
    void testWebMarkMaintenanceCompleted() throws Exception {
        mockMvc.perform(post("/vehicles/" + user1Vehicle.getVehicleId() + "/maintenance/" + user1MaintTask.getMaintenanceId() + "/complete")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/vehicles/" + user1Vehicle.getVehicleId() + "?tab=maintenance"))
                .andExpect(flash().attributeExists("successMsg"));

        MaintenanceRecord completed = maintenanceRecordRepository.findById(user1MaintTask.getMaintenanceId()).orElseThrow();
        assertEquals(MaintenanceStatus.COMPLETED, completed.getStatus());
        assertNotNull(completed.getCompletedDate());
    }

    @Test
    @DisplayName("11. Web: Render Edit Maintenance Form")
    @WithMockUser(username = "maint_user1@test.com", roles = {"NORMAL_USER"})
    void testWebEditMaintenanceForm() throws Exception {
        mockMvc.perform(get("/vehicles/" + user1Vehicle.getVehicleId() + "/maintenance/" + user1MaintTask.getMaintenanceId() + "/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("maintenance/form"))
                .andExpect(model().attributeExists("maintenanceRequest"))
                .andExpect(model().attribute("maintenanceId", user1MaintTask.getMaintenanceId()));
    }

    @Test
    @DisplayName("12. Web: Update Maintenance Task Success")
    @WithMockUser(username = "maint_user1@test.com", roles = {"NORMAL_USER"})
    void testWebUpdateMaintenanceSuccess() throws Exception {
        mockMvc.perform(post("/vehicles/" + user1Vehicle.getVehicleId() + "/maintenance/" + user1MaintTask.getMaintenanceId() + "/edit")
                .with(csrf())
                .param("title", "Brake Fluid Flush & Bleed")
                .param("scheduledDate", user1MaintTask.getScheduledDate().toString())
                .param("cost", "1800.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/vehicles/" + user1Vehicle.getVehicleId() + "?tab=maintenance"))
                .andExpect(flash().attributeExists("successMsg"));

        MaintenanceRecord updated = maintenanceRecordRepository.findById(user1MaintTask.getMaintenanceId()).orElseThrow();
        assertEquals("Brake Fluid Flush & Bleed", updated.getTitle());
        assertEquals(new BigDecimal("1800.00"), updated.getCost());
    }

    @Test
    @DisplayName("13. Web: Delete Maintenance Task Success")
    @WithMockUser(username = "maint_user1@test.com", roles = {"NORMAL_USER"})
    void testWebDeleteMaintenanceSuccess() throws Exception {
        mockMvc.perform(post("/vehicles/" + user1Vehicle.getVehicleId() + "/maintenance/" + user1MaintTask.getMaintenanceId() + "/delete")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/vehicles/" + user1Vehicle.getVehicleId() + "?tab=maintenance"))
                .andExpect(flash().attributeExists("successMsg"));

        assertTrue(maintenanceRecordRepository.findById(user1MaintTask.getMaintenanceId()).isEmpty());
    }

    // =========================================================================
    // SECTION 2: OWNERSHIP & TAMPERING ISOLATION
    // =========================================================================

    @Test
    @DisplayName("14. Ownership: Same-User Access Allowed")
    @WithMockUser(username = "maint_user1@test.com", roles = {"NORMAL_USER"})
    void testSameUserAccessAllowed() throws Exception {
        mockMvc.perform(get("/vehicles/" + user1Vehicle.getVehicleId() + "?tab=maintenance"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("15. Ownership: Cross-User Maintenance View Blocked (User 2 on User 1 Vehicle)")
    @WithMockUser(username = "maint_user2@test.com", roles = {"NORMAL_USER"})
    void testCrossUserMaintenanceViewBlocked() throws Exception {
        mockMvc.perform(get("/vehicles/" + user1Vehicle.getVehicleId() + "?tab=maintenance"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error/error"));
    }

    @Test
    @DisplayName("16. Ownership: Cross-User Maintenance Creation Blocked")
    @WithMockUser(username = "maint_user2@test.com", roles = {"NORMAL_USER"})
    void testCrossUserMaintenanceCreationBlocked() throws Exception {
        mockMvc.perform(post("/vehicles/" + user1Vehicle.getVehicleId() + "/maintenance/add")
                .with(csrf())
                .param("title", "Hacked Task")
                .param("scheduledDate", LocalDate.now().toString()))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error/error"));

        assertEquals(1, maintenanceRecordRepository.findByVehicleVehicleIdOrderByScheduledDateAsc(user1Vehicle.getVehicleId()).size());
    }

    @Test
    @DisplayName("17. Ownership: Cross-User Maintenance Update Blocked")
    @WithMockUser(username = "maint_user2@test.com", roles = {"NORMAL_USER"})
    void testCrossUserMaintenanceUpdateBlocked() throws Exception {
        mockMvc.perform(post("/vehicles/" + user1Vehicle.getVehicleId() + "/maintenance/" + user1MaintTask.getMaintenanceId() + "/edit")
                .with(csrf())
                .param("title", "Hacked Title")
                .param("scheduledDate", LocalDate.now().toString()))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error/error"));

        MaintenanceRecord unchanged = maintenanceRecordRepository.findById(user1MaintTask.getMaintenanceId()).orElseThrow();
        assertEquals("Brake Fluid Flush", unchanged.getTitle());
    }

    @Test
    @DisplayName("18. Ownership: Cross-User Maintenance Complete Blocked")
    @WithMockUser(username = "maint_user2@test.com", roles = {"NORMAL_USER"})
    void testCrossUserMaintenanceCompleteBlocked() throws Exception {
        mockMvc.perform(post("/vehicles/" + user1Vehicle.getVehicleId() + "/maintenance/" + user1MaintTask.getMaintenanceId() + "/complete")
                .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error/error"));

        MaintenanceRecord unchanged = maintenanceRecordRepository.findById(user1MaintTask.getMaintenanceId()).orElseThrow();
        assertEquals(MaintenanceStatus.UPCOMING, unchanged.getStatus());
    }

    @Test
    @DisplayName("19. Ownership: Cross-User Maintenance Delete Blocked")
    @WithMockUser(username = "maint_user2@test.com", roles = {"NORMAL_USER"})
    void testCrossUserMaintenanceDeleteBlocked() throws Exception {
        mockMvc.perform(post("/vehicles/" + user1Vehicle.getVehicleId() + "/maintenance/" + user1MaintTask.getMaintenanceId() + "/delete")
                .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error/error"));

        assertTrue(maintenanceRecordRepository.findById(user1MaintTask.getMaintenanceId()).isPresent());
    }

    @Test
    @DisplayName("20. Ownership: Mismatched Vehicle/Maintenance ID Blocked (URL Tampering)")
    @WithMockUser(username = "maint_user1@test.com", roles = {"NORMAL_USER"})
    void testMismatchedVehicleAndMaintenanceIdBlocked() throws Exception {
        // User 1 creates second vehicle
        Vehicle vehicle1b = new Vehicle();
        vehicle1b.setUser(user1);
        vehicle1b.setCategory(category);
        vehicle1b.setPlateNumber("TN07MN3003");
        vehicle1b.setMake("Toyota");
        vehicle1b.setModel("Innova");
        vehicle1b.setYear(2024);
        vehicle1b = vehicleRepository.save(vehicle1b);

        // Attempt to edit user1MaintTask (attached to user1Vehicle) through vehicle1b's URL
        mockMvc.perform(get("/vehicles/" + vehicle1b.getVehicleId() + "/maintenance/" + user1MaintTask.getMaintenanceId() + "/edit"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error/error"));
    }

    // =========================================================================
    // SECTION 3: SECURITY & ADMIN ISOLATION
    // =========================================================================

    @Test
    @DisplayName("21. Security: Unauthenticated Access Redirects to /login")
    void testUnauthenticatedAccessRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/vehicles/" + user1Vehicle.getVehicleId() + "/maintenance/add"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("22. Security: ADMIN Role Cannot Access User Maintenance Endpoints (403 Forbidden)")
    @WithMockUser(username = "maint_admin@test.com", roles = {"ADMIN"})
    void testAdminCannotAccessMaintenanceEndpoints() throws Exception {
        mockMvc.perform(get("/vehicles/" + user1Vehicle.getVehicleId() + "?tab=maintenance"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/vehicles/" + user1Vehicle.getVehicleId() + "/maintenance"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/maintenance/" + user1MaintTask.getMaintenanceId()))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // SECTION 4: REST API CRUD & OWNERSHIP
    // =========================================================================

    @Test
    @DisplayName("23. REST: GET /api/vehicles/{id}/maintenance Returns List")
    @WithMockUser(username = "maint_user1@test.com", roles = {"NORMAL_USER"})
    void testRestGetMaintenanceListSuccess() throws Exception {
        mockMvc.perform(get("/api/vehicles/" + user1Vehicle.getVehicleId() + "/maintenance"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Brake Fluid Flush"))
                .andExpect(jsonPath("$[0].status").value("UPCOMING"));
    }

    @Test
    @DisplayName("24. REST: GET /api/maintenance/{id} Returns Single Record")
    @WithMockUser(username = "maint_user1@test.com", roles = {"NORMAL_USER"})
    void testRestGetMaintenanceByIdSuccess() throws Exception {
        mockMvc.perform(get("/api/maintenance/" + user1MaintTask.getMaintenanceId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Brake Fluid Flush"))
                .andExpect(jsonPath("$.cost").value(1500.0));
    }

    @Test
    @DisplayName("25. REST: POST /api/vehicles/{id}/maintenance Creates Record (201 Created)")
    @WithMockUser(username = "maint_user1@test.com", roles = {"NORMAL_USER"})
    void testRestPostMaintenanceSuccess() throws Exception {
        MaintenanceRequest req = new MaintenanceRequest();
        req.setTitle("Cabin Air Filter Replacement");
        req.setScheduledDate(LocalDate.now().plusWeeks(2));
        req.setCost(new BigDecimal("750.00"));

        mockMvc.perform(post("/api/vehicles/" + user1Vehicle.getVehicleId() + "/maintenance")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Cabin Air Filter Replacement"))
                .andExpect(jsonPath("$.status").value("UPCOMING"))
                .andExpect(jsonPath("$.cost").value(750.0));
    }

    @Test
    @DisplayName("26. REST: PUT /api/maintenance/{id} Updates Record (200 OK)")
    @WithMockUser(username = "maint_user1@test.com", roles = {"NORMAL_USER"})
    void testRestPutMaintenanceSuccess() throws Exception {
        MaintenanceRequest req = new MaintenanceRequest();
        req.setTitle("Brake Fluid Flush & ABS Inspection");
        req.setScheduledDate(user1MaintTask.getScheduledDate());
        req.setCost(new BigDecimal("2100.00"));

        mockMvc.perform(put("/api/maintenance/" + user1MaintTask.getMaintenanceId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Brake Fluid Flush & ABS Inspection"))
                .andExpect(jsonPath("$.cost").value(2100.0));
    }

    @Test
    @DisplayName("27. REST: PATCH /api/maintenance/{id}/complete Marks Task Completed")
    @WithMockUser(username = "maint_user1@test.com", roles = {"NORMAL_USER"})
    void testRestPatchMarkCompletedSuccess() throws Exception {
        mockMvc.perform(patch("/api/maintenance/" + user1MaintTask.getMaintenanceId() + "/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.completedDate").isNotEmpty());
    }

    @Test
    @DisplayName("28. REST: POST /api/maintenance/{id}/complete Marks Task Completed")
    @WithMockUser(username = "maint_user1@test.com", roles = {"NORMAL_USER"})
    void testRestPostMarkCompletedSuccess() throws Exception {
        mockMvc.perform(post("/api/maintenance/" + user1MaintTask.getMaintenanceId() + "/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.completedDate").isNotEmpty());
    }

    @Test
    @DisplayName("29. REST: DELETE /api/maintenance/{id} Deletes Record (200 OK)")
    @WithMockUser(username = "maint_user1@test.com", roles = {"NORMAL_USER"})
    void testRestDeleteMaintenanceSuccess() throws Exception {
        mockMvc.perform(delete("/api/maintenance/" + user1MaintTask.getMaintenanceId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Maintenance record deleted."));

        assertTrue(maintenanceRecordRepository.findById(user1MaintTask.getMaintenanceId()).isEmpty());
    }

    @Test
    @DisplayName("30. Ownership & Cascade: REST Cross-User Blocked & Vehicle Cascade Deletion")
    @WithMockUser(username = "maint_user2@test.com", roles = {"NORMAL_USER"})
    void testRestCrossUserBlockedAndCascadeDeletion() throws Exception {
        // User 2 attempts GET on User 1's maintenance task -> 400 Bad Request
        mockMvc.perform(get("/api/maintenance/" + user1MaintTask.getMaintenanceId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Maintenance record not found or access denied."));

        // User 2 attempts POST against User 1's vehicle -> 400 Bad Request
        MaintenanceRequest req = new MaintenanceRequest();
        req.setTitle("Illegal Task");
        req.setScheduledDate(LocalDate.now());

        mockMvc.perform(post("/api/vehicles/" + user1Vehicle.getVehicleId() + "/maintenance")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Vehicle not found or access denied."));

        // Cascade Deletion Verification
        Long vId = user1Vehicle.getVehicleId();
        Long mId = user1MaintTask.getMaintenanceId();
        assertTrue(maintenanceRecordRepository.findById(mId).isPresent());

        // Delete vehicle by owner
        vehicleRepository.deleteById(vId);

        // Maintenance record must be completely cascaded and deleted
        assertTrue(maintenanceRecordRepository.findById(mId).isEmpty());
    }
}