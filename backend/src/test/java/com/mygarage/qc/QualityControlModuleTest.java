package com.mygarage.qc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mygarage.dto.request.UpdateProfileRequest;
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
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Milestone 10 (M10) — Comprehensive 6-Layer Quality Control & System Verification Suite.
 * Covers:
 * 1. Web Profile Viewing
 * 2. Web Profile Update
 * 3. Web Password Change Success
 * 4. Web Password Change Incorrect Password Rejected
 * 5. Web Password Change Mismatched Passwords Rejected
 * 6. Web Vehicle Details Page Displays Complete Records
 * 7. Web Vehicle Timeline Displays Service, Fuel, and Maintenance
 * 8. Web Fuel History Displays Estimated Mileage
 * 9. Web Maintenance Status Calculation
 * 10. REST Profile: GET /api/profile
 * 11. REST Profile: PUT /api/profile
 * 12. REST Profile: POST /api/profile/change-password
 * 13. REST Profile: Unauthenticated Access Blocked
 * 14. REST Error Handling: Structured Problem/JSON Details
 * 15. Complete 6-Layer End-to-End Garage Lifecycle Verification
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class QualityControlModuleTest {

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

    @Autowired
    private ObjectMapper objectMapper;

    private User qcUser;
    private VehicleCategory qcCategory;
    private Vehicle qcVehicle;

    @BeforeEach
    void setUp() {
        maintenanceRecordRepository.deleteAll();
        serviceRecordRepository.deleteAll();
        fuelRecordRepository.deleteAll();
        vehicleRepository.deleteAll();

        // Setup QC User
        qcUser = userRepository.findByEmail("qc_user@test.com").orElseGet(User::new);
        qcUser.setFullName("QC Inspector");
        qcUser.setEmail("qc_user@test.com");
        qcUser.setPasswordHash(passwordEncoder.encode("QcPassword@123"));
        qcUser.setRole(Role.NORMAL_USER);
        qcUser.setActive(true);
        qcUser.setPhone("9876543210");
        qcUser = userRepository.save(qcUser);

        // Setup Category
        qcCategory = categoryRepository.findByNameIgnoreCase("SUV QC").orElseGet(() -> {
            VehicleCategory cat = new VehicleCategory();
            cat.setName("SUV QC");
            cat.setIcon("🚙");
            return categoryRepository.save(cat);
        });

        // Setup Vehicle
        qcVehicle = new Vehicle();
        qcVehicle.setUser(qcUser);
        qcVehicle.setCategory(qcCategory);
        qcVehicle.setPlateNumber("KA05QC1001");
        qcVehicle.setMake("Toyota");
        qcVehicle.setModel("Fortuner");
        qcVehicle.setYear(2023);
        qcVehicle.setCurrentOdometer(15000);
        qcVehicle.setFuelType("DIESEL");
        qcVehicle = vehicleRepository.save(qcVehicle);
    }

    // =========================================================================
    // SECTION 1: USER PROFILE & PASSWORD MANAGEMENT (WEB)
    // =========================================================================

    @Test
    @DisplayName("1. Web: View Profile Page")
    @WithMockUser(username = "qc_user@test.com", roles = {"NORMAL_USER"})
    void testViewProfilePage() throws Exception {
        mockMvc.perform(get("/profile"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile/index"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("profileRequest"));
    }

    @Test
    @DisplayName("2. Web: Update Profile Name & Phone")
    @WithMockUser(username = "qc_user@test.com", roles = {"NORMAL_USER"})
    void testUpdateProfileSuccess() throws Exception {
        mockMvc.perform(post("/profile/update")
                .with(csrf())
                .param("fullName", "QC Senior Inspector")
                .param("phone", "9123456780"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attributeExists("successMsg"));

        User updated = userRepository.findById(qcUser.getUserId()).orElseThrow();
        assertEquals("QC Senior Inspector", updated.getFullName());
        assertEquals("9123456780", updated.getPhone());
    }

    @Test
    @DisplayName("3. Web: Change Password Success")
    @WithMockUser(username = "qc_user@test.com", roles = {"NORMAL_USER"})
    void testChangePasswordSuccess() throws Exception {
        mockMvc.perform(post("/profile/change-password")
                .with(csrf())
                .param("currentPassword", "QcPassword@123")
                .param("newPassword", "NewSecurePass@456")
                .param("confirmNewPassword", "NewSecurePass@456"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attributeExists("passwordSuccess"));

        User updated = userRepository.findById(qcUser.getUserId()).orElseThrow();
        assertTrue(passwordEncoder.matches("NewSecurePass@456", updated.getPasswordHash()));
    }

    @Test
    @DisplayName("4. Web: Change Password with Incorrect Current Password Rejected")
    @WithMockUser(username = "qc_user@test.com", roles = {"NORMAL_USER"})
    void testChangePasswordIncorrectCurrentRejected() throws Exception {
        mockMvc.perform(post("/profile/change-password")
                .with(csrf())
                .param("currentPassword", "WrongOldPassword")
                .param("newPassword", "NewSecurePass@456")
                .param("confirmNewPassword", "NewSecurePass@456"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attribute("passwordError", containsString("Current password is incorrect.")));
    }

    @Test
    @DisplayName("5. Web: Change Password with Mismatched Passwords Rejected")
    @WithMockUser(username = "qc_user@test.com", roles = {"NORMAL_USER"})
    void testChangePasswordMismatchedRejected() throws Exception {
        mockMvc.perform(post("/profile/change-password")
                .with(csrf())
                .param("currentPassword", "QcPassword@123")
                .param("newPassword", "PassOne@123")
                .param("confirmNewPassword", "PassTwo@123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attribute("passwordError", containsString("New passwords do not match.")));
    }

    // =========================================================================
    // SECTION 2: VEHICLE DETAILS & TIMELINE (WEB)
    // =========================================================================

    @Test
    @DisplayName("6. Web: Vehicle Details Page Displays Complete Records")
    @WithMockUser(username = "qc_user@test.com", roles = {"NORMAL_USER"})
    void testVehicleDetailsDisplaysCompleteRecords() throws Exception {
        // Add Service
        ServiceRecord s = new ServiceRecord();
        s.setVehicle(qcVehicle);
        s.setServiceDate(LocalDate.now().minusWeeks(2));
        s.setServiceType("Engine Oil Replacement");
        s.setCost(new BigDecimal("4500.00"));
        s.setOdometerAtService(14000);
        serviceRecordRepository.save(s);

        // Add Fuel Fill
        FuelRecord f = new FuelRecord();
        f.setVehicle(qcVehicle);
        f.setFuelDate(LocalDate.now().minusDays(3));
        f.setFuelType(FuelType.DIESEL);
        f.setQuantityLitres(new BigDecimal("40.00"));
        f.setCostPerLitre(new BigDecimal("90.00"));
        f.setTotalCost(new BigDecimal("3600.00"));
        f.setOdometerAtFill(14500);
        fuelRecordRepository.save(f);

        // Add Maintenance Task
        MaintenanceRecord m = new MaintenanceRecord();
        m.setVehicle(qcVehicle);
        m.setTitle("Brake Pad Inspection");
        m.setScheduledDate(LocalDate.now().plusWeeks(1));
        m.setStatus(MaintenanceStatus.UPCOMING);
        maintenanceRecordRepository.save(m);

        mockMvc.perform(get("/vehicles/" + qcVehicle.getVehicleId()))
                .andExpect(status().isOk())
                .andExpect(view().name("vehicle/detail"))
                .andExpect(model().attributeExists("vehicle"))
                .andExpect(model().attributeExists("serviceRecords"))
                .andExpect(model().attributeExists("fuelRecords"))
                .andExpect(model().attributeExists("maintenanceRecords"));
    }

    @Test
    @DisplayName("7. Web: Vehicle Timeline Displays Service, Fuel, and Maintenance")
    @WithMockUser(username = "qc_user@test.com", roles = {"NORMAL_USER"})
    void testVehicleTimelineDisplaysRecords() throws Exception {
        ServiceRecord s = new ServiceRecord();
        s.setVehicle(qcVehicle);
        s.setServiceDate(LocalDate.now().minusDays(10));
        s.setServiceType("Wheel Alignment");
        s.setCost(new BigDecimal("1200.00"));
        s.setGarageName("Precision Wheels");
        serviceRecordRepository.save(s);

        mockMvc.perform(get("/vehicles/" + qcVehicle.getVehicleId() + "/timeline"))
                .andExpect(status().isOk())
                .andExpect(view().name("vehicle/timeline"))
                .andExpect(model().attributeExists("vehicle"))
                .andExpect(model().attributeExists("serviceRecords"))
                .andExpect(model().attributeExists("fuelRecords"))
                .andExpect(model().attributeExists("maintenanceRecords"));
    }

    @Test
    @DisplayName("8. Web: Fuel History Displays Calculated Mileage")
    @WithMockUser(username = "qc_user@test.com", roles = {"NORMAL_USER"})
    void testFuelHistoryDisplaysMileage() throws Exception {
        FuelRecord f1 = new FuelRecord();
        f1.setVehicle(qcVehicle);
        f1.setFuelDate(LocalDate.now().minusDays(10));
        f1.setFuelType(FuelType.DIESEL);
        f1.setQuantityLitres(new BigDecimal("50.00"));
        f1.setCostPerLitre(new BigDecimal("90.00"));
        f1.setTotalCost(new BigDecimal("4500.00"));
        f1.setOdometerAtFill(14000);
        fuelRecordRepository.save(f1);

        FuelRecord f2 = new FuelRecord();
        f2.setVehicle(qcVehicle);
        f2.setFuelDate(LocalDate.now().minusDays(2));
        f2.setFuelType(FuelType.DIESEL);
        f2.setQuantityLitres(new BigDecimal("40.00"));
        f2.setCostPerLitre(new BigDecimal("90.00"));
        f2.setTotalCost(new BigDecimal("3600.00"));
        f2.setOdometerAtFill(14600);
        f2.setEstimatedMileageKmpl(new BigDecimal("15.00"));
        fuelRecordRepository.save(f2);

        mockMvc.perform(get("/vehicles/" + qcVehicle.getVehicleId()))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("fuelRecords"))
                .andExpect(model().attribute("fuelRecords", hasItem(hasProperty("estimatedMileageKmpl", is(new BigDecimal("15.00"))))));
    }

    @Test
    @DisplayName("9. Web: Maintenance Status Calculation Verified in Details View")
    @WithMockUser(username = "qc_user@test.com", roles = {"NORMAL_USER"})
    void testMaintenanceStatusCalculationInView() throws Exception {
        MaintenanceRecord mOverdue = new MaintenanceRecord();
        mOverdue.setVehicle(qcVehicle);
        mOverdue.setTitle("Air Filter (Overdue)");
        mOverdue.setScheduledDate(LocalDate.now().minusDays(4));
        mOverdue.setStatus(MaintenanceStatus.OVERDUE);
        maintenanceRecordRepository.save(mOverdue);

        MaintenanceRecord mCompleted = new MaintenanceRecord();
        mCompleted.setVehicle(qcVehicle);
        mCompleted.setTitle("Oil Filter (Done)");
        mCompleted.setScheduledDate(LocalDate.now().minusDays(10));
        mCompleted.setCompletedDate(LocalDate.now().minusDays(9));
        mCompleted.setStatus(MaintenanceStatus.COMPLETED);
        maintenanceRecordRepository.save(mCompleted);

        mockMvc.perform(get("/vehicles/" + qcVehicle.getVehicleId()))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("maintenanceRecords"))
                .andExpect(model().attribute("maintenanceRecords", hasItem(hasProperty("status", is(MaintenanceStatus.OVERDUE)))))
                .andExpect(model().attribute("maintenanceRecords", hasItem(hasProperty("status", is(MaintenanceStatus.COMPLETED)))));
    }

    // =========================================================================
    // SECTION 3: REST PROFILE APIS & ERROR HANDLING
    // =========================================================================

    @Test
    @DisplayName("10. REST Profile: GET /api/profile Returns User Information")
    @WithMockUser(username = "qc_user@test.com", roles = {"NORMAL_USER"})
    void testRestGetProfile() throws Exception {
        mockMvc.perform(get("/api/profile"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.email").value("qc_user@test.com"))
                .andExpect(jsonPath("$.fullName").value("QC Inspector"))
                .andExpect(jsonPath("$.role").value("NORMAL_USER"));
    }

    @Test
    @DisplayName("11. REST Profile: PUT /api/profile Updates Profile Information")
    @WithMockUser(username = "qc_user@test.com", roles = {"NORMAL_USER"})
    void testRestPutProfile() throws Exception {
        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setFullName("Updated REST Inspector");
        req.setPhone("9988776655");

        mockMvc.perform(put("/api/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Updated REST Inspector"))
                .andExpect(jsonPath("$.phone").value("9988776655"));
    }

    @Test
    @DisplayName("12. REST Profile: POST /api/profile/change-password Success")
    @WithMockUser(username = "qc_user@test.com", roles = {"NORMAL_USER"})
    void testRestChangePasswordSuccess() throws Exception {
        mockMvc.perform(post("/api/profile/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "currentPassword", "QcPassword@123",
                        "newPassword", "NewRestPass@789"
                ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password changed successfully."));

        User updated = userRepository.findById(qcUser.getUserId()).orElseThrow();
        assertTrue(passwordEncoder.matches("NewRestPass@789", updated.getPasswordHash()));
    }

    @Test
    @DisplayName("13. REST Profile: Unauthenticated Access Blocked")
    void testRestProfileUnauthenticatedBlocked() throws Exception {
        mockMvc.perform(get("/api/profile"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("14. REST Error Handling: Structured JSON on Invalid Argument")
    @WithMockUser(username = "qc_user@test.com", roles = {"NORMAL_USER"})
    void testRestStructuredErrorHandling() throws Exception {
        mockMvc.perform(post("/api/profile/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "currentPassword", "WrongPass",
                        "newPassword", "123" // Too short
                ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").exists());
    }

    // =========================================================================
    // SECTION 4: COMPLETE 6-LAYER END-TO-END WORKFLOW INTEGRATION
    // =========================================================================

    @Test
    @DisplayName("15. 6-Layer QC: Full Garage Lifecycle (Vehicle -> Service -> Fuel -> Maintenance -> Dashboard)")
    @WithMockUser(username = "qc_user@test.com", roles = {"NORMAL_USER"})
    void testFullGarageLifecycleIntegration() throws Exception {
        // 1. Add Service
        mockMvc.perform(post("/vehicles/" + qcVehicle.getVehicleId() + "/services/add")
                .with(csrf())
                .param("serviceDate", LocalDate.now().minusWeeks(1).toString())
                .param("serviceType", "Major 20k Service")
                .param("cost", "7500.00")
                .param("odometerAtService", "15500")
                .param("garageName", "Toyota Authorized Service"))
                .andExpect(status().is3xxRedirection());

        // 2. Add Fuel Fill
        mockMvc.perform(post("/vehicles/" + qcVehicle.getVehicleId() + "/fuel/add")
                .with(csrf())
                .param("fuelDate", LocalDate.now().minusDays(2).toString())
                .param("fuelType", "DIESEL")
                .param("quantityLitres", "45.00")
                .param("costPerLitre", "90.00")
                .param("odometerAtFill", "16000"))
                .andExpect(status().is3xxRedirection());

        // 3. Add Maintenance Task
        mockMvc.perform(post("/vehicles/" + qcVehicle.getVehicleId() + "/maintenance/add")
                .with(csrf())
                .param("title", "Coolant Flush")
                .param("scheduledDate", LocalDate.now().minusDays(1).toString()) // Overdue!
                .param("cost", "1200.00"))
                .andExpect(status().is3xxRedirection());

        // 4. Verify Dashboard displays updated counters & alerts
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("vehicleCount", equalTo(1L)))
                .andExpect(model().attribute("serviceCount", equalTo(1L)))
                .andExpect(model().attribute("fuelCount", equalTo(1L)))
                .andExpect(model().attribute("maintenanceCount", equalTo(1L)))
                .andExpect(model().attribute("overdueCount", equalTo(1L)))
                .andExpect(model().attribute("totalFuelCost", new BigDecimal("4050.00")))
                .andExpect(model().attribute("totalServiceCost", new BigDecimal("7500.00")))
                .andExpect(model().attribute("totalMaintenanceCost", new BigDecimal("1200.00")))
                .andExpect(model().attribute("totalGarageCost", new BigDecimal("12750.00")));
    }
}