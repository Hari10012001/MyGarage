package com.mygarage.vehicle;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mygarage.dto.request.VehicleRequest;
import com.mygarage.model.*;
import com.mygarage.model.enums.FuelType;
import com.mygarage.model.enums.MaintenanceStatus;
import com.mygarage.model.enums.Role;
import com.mygarage.repository.*;
import com.mygarage.service.VehicleService;
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
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Milestone 4 (M4) — Comprehensive Vehicle Module Test Suite.
 * Covers:
 * - CRUD operations via Web MVC (Thymeleaf)
 * - CRUD operations via REST API (/api/vehicles)
 * - Server-side validation (year constraints, not-blank, odometer)
 * - Ownership enforcement (User 1 cannot view, edit, delete User 2's vehicle)
 * - License plate uniqueness per user (same plate allowed across different users)
 * - Cascade deletion of service, fuel, and maintenance records
 * - ADMIN isolation (ADMIN cannot access personal vehicle garage)
 * - Unauthenticated redirects
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class VehicleModuleTest {

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
    private VehicleService vehicleService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private User user1;
    private User user2;
    private User admin;
    private VehicleCategory sedanCat;
    private VehicleCategory suvCat;
    private Vehicle user1Vehicle;

    @BeforeEach
    void setUp() {
        // Clear vehicle data first
        maintenanceRecordRepository.deleteAll();
        fuelRecordRepository.deleteAll();
        serviceRecordRepository.deleteAll();
        vehicleRepository.deleteAll();

        // 1. Setup User 1
        user1 = userRepository.findByEmail("veh_user1@test.com").orElseGet(User::new);
        user1.setFullName("Vehicle Owner One");
        user1.setEmail("veh_user1@test.com");
        user1.setPasswordHash(passwordEncoder.encode("Secret@123"));
        user1.setPhone("9876543211");
        user1.setRole(Role.NORMAL_USER);
        user1.setActive(true);
        user1 = userRepository.save(user1);

        // 2. Setup User 2
        user2 = userRepository.findByEmail("veh_user2@test.com").orElseGet(User::new);
        user2.setFullName("Vehicle Owner Two");
        user2.setEmail("veh_user2@test.com");
        user2.setPasswordHash(passwordEncoder.encode("Secret@123"));
        user2.setPhone("9876543212");
        user2.setRole(Role.NORMAL_USER);
        user2.setActive(true);
        user2 = userRepository.save(user2);

        // 3. Setup Admin
        admin = userRepository.findByEmail("veh_admin@test.com").orElseGet(User::new);
        admin.setFullName("Vehicle System Admin");
        admin.setEmail("veh_admin@test.com");
        admin.setPasswordHash(passwordEncoder.encode("Admin@123"));
        admin.setPhone("9999999991");
        admin.setRole(Role.ADMIN);
        admin.setActive(true);
        admin = userRepository.save(admin);

        // 4. Setup Categories
        sedanCat = categoryRepository.findByNameIgnoreCase("Sedan").orElseGet(() -> {
            VehicleCategory cat = new VehicleCategory();
            cat.setName("Sedan");
            cat.setIcon("🚗");
            cat.setDescription("Standard 4-door passenger cars");
            return categoryRepository.save(cat);
        });

        suvCat = categoryRepository.findByNameIgnoreCase("SUV").orElseGet(() -> {
            VehicleCategory cat = new VehicleCategory();
            cat.setName("SUV");
            cat.setIcon("🚙");
            cat.setDescription("Sport Utility Vehicles");
            return categoryRepository.save(cat);
        });

        // 5. Seed a baseline vehicle for User 1
        user1Vehicle = new Vehicle();
        user1Vehicle.setUser(user1);
        user1Vehicle.setCategory(sedanCat);
        user1Vehicle.setPlateNumber("TN01AB1234");
        user1Vehicle.setMake("Honda");
        user1Vehicle.setModel("City");
        user1Vehicle.setYear(2022);
        user1Vehicle.setColor("Silver");
        user1Vehicle.setFuelType("PETROL");
        user1Vehicle.setCurrentOdometer(15000);
        user1Vehicle.setNotes("Primary daily driver");
        user1Vehicle = vehicleRepository.save(user1Vehicle);
    }

    // =========================================================================
    // SECTION 1: WEB MVC OPERATIONS (NORMAL_USER)
    // =========================================================================

    @Test
    @DisplayName("1. Web: View My Vehicles List (NORMAL_USER)")
    @WithMockUser(username = "veh_user1@test.com", roles = {"NORMAL_USER"})
    void testWebListVehicles() throws Exception {
        mockMvc.perform(get("/vehicles"))
                .andExpect(status().isOk())
                .andExpect(view().name("vehicle/list"))
                .andExpect(model().attributeExists("vehicles"))
                .andExpect(model().attribute("vehicles", hasSize(1)));
    }

    @Test
    @DisplayName("2. Web: Render Add Vehicle Form (NORMAL_USER)")
    @WithMockUser(username = "veh_user1@test.com", roles = {"NORMAL_USER"})
    void testWebAddVehicleForm() throws Exception {
        mockMvc.perform(get("/vehicles/add"))
                .andExpect(status().isOk())
                .andExpect(view().name("vehicle/form"))
                .andExpect(model().attributeExists("vehicleRequest"))
                .andExpect(model().attributeExists("categories"));
    }

    @Test
    @DisplayName("3. Web: Add Vehicle Success")
    @WithMockUser(username = "veh_user1@test.com", roles = {"NORMAL_USER"})
    void testWebAddVehicleSuccess() throws Exception {
        mockMvc.perform(post("/vehicles/add")
                .with(csrf())
                .param("plateNumber", "TN02CD5678")
                .param("categoryId", sedanCat.getCategoryId().toString())
                .param("make", "Hyundai")
                .param("model", "Verna")
                .param("year", "2023")
                .param("color", "White")
                .param("fuelType", "DIESEL")
                .param("currentOdometer", "8500")
                .param("notes", "Second car"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/vehicles"))
                .andExpect(flash().attributeExists("successMsg"));

        assertTrue(vehicleRepository.existsByPlateNumberIgnoreCaseAndUserUserId("TN02CD5678", user1.getUserId()));
    }

    @Test
    @DisplayName("4. Web: Add Vehicle Validation Failure (Missing Required Fields)")
    @WithMockUser(username = "veh_user1@test.com", roles = {"NORMAL_USER"})
    void testWebAddVehicleValidationFailure() throws Exception {
        mockMvc.perform(post("/vehicles/add")
                .with(csrf())
                .param("plateNumber", "")
                .param("make", "")
                .param("model", "")
                .param("year", "1850") // invalid year
                .param("currentOdometer", "-50")) // invalid odometer
                .andExpect(status().isOk())
                .andExpect(view().name("vehicle/form"))
                .andExpect(model().hasErrors());
    }

    @Test
    @DisplayName("5. Web: Duplicate Plate Number per User Garage is Rejected")
    @WithMockUser(username = "veh_user1@test.com", roles = {"NORMAL_USER"})
    void testWebDuplicatePlatePerUserRejected() throws Exception {
        mockMvc.perform(post("/vehicles/add")
                .with(csrf())
                .param("plateNumber", "TN01AB1234") // already exists for user 1
                .param("categoryId", sedanCat.getCategoryId().toString())
                .param("make", "Toyota")
                .param("model", "Corolla")
                .param("year", "2021")
                .param("currentOdometer", "20000"))
                .andExpect(status().isOk())
                .andExpect(view().name("vehicle/form"))
                .andExpect(model().attributeExists("errorMsg"));
    }

    @Test
    @DisplayName("6. Web: Same Plate Number for DIFFERENT Users is Allowed")
    @WithMockUser(username = "veh_user2@test.com", roles = {"NORMAL_USER"})
    void testWebSamePlateDifferentUsersAllowed() throws Exception {
        // User 2 adds a vehicle with the SAME plate number as User 1
        mockMvc.perform(post("/vehicles/add")
                .with(csrf())
                .param("plateNumber", "TN01AB1234")
                .param("categoryId", suvCat.getCategoryId().toString())
                .param("make", "Mahindra")
                .param("model", "Thar")
                .param("year", "2023")
                .param("currentOdometer", "5000"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/vehicles"))
                .andExpect(flash().attributeExists("successMsg"));

        assertTrue(vehicleRepository.existsByPlateNumberIgnoreCaseAndUserUserId("TN01AB1234", user2.getUserId()));
        assertTrue(vehicleRepository.existsByPlateNumberIgnoreCaseAndUserUserId("TN01AB1234", user1.getUserId()));
    }

    @Test
    @DisplayName("7. Web: View Vehicle Details for Own Vehicle")
    @WithMockUser(username = "veh_user1@test.com", roles = {"NORMAL_USER"})
    void testWebViewVehicleDetails() throws Exception {
        mockMvc.perform(get("/vehicles/" + user1Vehicle.getVehicleId()))
                .andExpect(status().isOk())
                .andExpect(view().name("vehicle/detail"))
                .andExpect(model().attributeExists("vehicle"))
                .andExpect(model().attributeExists("serviceRecords"))
                .andExpect(model().attributeExists("fuelRecords"))
                .andExpect(model().attributeExists("maintenanceRecords"));
    }

    @Test
    @DisplayName("8. Web: Direct URL Access to ANOTHER User's Vehicle is Forbidden (Data Isolation)")
    @WithMockUser(username = "veh_user2@test.com", roles = {"NORMAL_USER"})
    void testWebDirectUrlAccessAnotherUserVehicleForbidden() throws Exception {
        // User 2 attempts to view User 1's vehicle
        mockMvc.perform(get("/vehicles/" + user1Vehicle.getVehicleId()))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error/error"))
                .andExpect(model().attribute("errorMessage", containsString("Vehicle not found or access denied.")));
    }

    @Test
    @DisplayName("9. Web: Render Edit Vehicle Form for Own Vehicle")
    @WithMockUser(username = "veh_user1@test.com", roles = {"NORMAL_USER"})
    void testWebEditVehicleForm() throws Exception {
        mockMvc.perform(get("/vehicles/" + user1Vehicle.getVehicleId() + "/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("vehicle/form"))
                .andExpect(model().attributeExists("vehicleRequest"))
                .andExpect(model().attribute("vehicleId", user1Vehicle.getVehicleId()));
    }

    @Test
    @DisplayName("10. Web: Edit Form for ANOTHER User's Vehicle is Forbidden")
    @WithMockUser(username = "veh_user2@test.com", roles = {"NORMAL_USER"})
    void testWebEditAnotherUserVehicleForbidden() throws Exception {
        mockMvc.perform(get("/vehicles/" + user1Vehicle.getVehicleId() + "/edit"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error/error"));
    }

    @Test
    @DisplayName("11. Web: Update Vehicle Success")
    @WithMockUser(username = "veh_user1@test.com", roles = {"NORMAL_USER"})
    void testWebUpdateVehicleSuccess() throws Exception {
        mockMvc.perform(post("/vehicles/" + user1Vehicle.getVehicleId() + "/edit")
                .with(csrf())
                .param("plateNumber", "TN01AB1234") // same plate
                .param("categoryId", sedanCat.getCategoryId().toString())
                .param("make", "Honda")
                .param("model", "City ZX") // updated model
                .param("year", "2022")
                .param("color", "Midnight Blue") // updated color
                .param("fuelType", "PETROL")
                .param("currentOdometer", "18500") // updated odometer
                .param("notes", "Regularly serviced"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/vehicles/" + user1Vehicle.getVehicleId()))
                .andExpect(flash().attributeExists("successMsg"));

        Vehicle updated = vehicleRepository.findById(user1Vehicle.getVehicleId()).orElseThrow();
        assertEquals("City ZX", updated.getModel());
        assertEquals("Midnight Blue", updated.getColor());
        assertEquals(18500, updated.getCurrentOdometer());
    }

    @Test
    @DisplayName("12. Web: Delete Vehicle Cascades and Removes Child Records (Zero Orphans)")
    @WithMockUser(username = "veh_user1@test.com", roles = {"NORMAL_USER"})
    void testWebDeleteVehicleCascades() throws Exception {
        // Attach child records to user1Vehicle
        ServiceRecord service = new ServiceRecord();
        service.setServiceDate(LocalDate.now());
        service.setServiceType("Annual Inspection");
        user1Vehicle.addServiceRecord(service);

        FuelRecord fuel = new FuelRecord();
        fuel.setFuelDate(LocalDate.now());
        fuel.setFuelType(FuelType.PETROL);
        fuel.setQuantityLitres(new BigDecimal("35.00"));
        fuel.setCostPerLitre(new BigDecimal("102.50"));
        user1Vehicle.addFuelRecord(fuel);

        MaintenanceRecord maint = new MaintenanceRecord();
        maint.setTitle("Tire Rotation");
        maint.setScheduledDate(LocalDate.now().plusWeeks(2));
        maint.setStatus(MaintenanceStatus.UPCOMING);
        user1Vehicle.addMaintenanceRecord(maint);

        user1Vehicle = vehicleRepository.save(user1Vehicle);
        Long vehicleId = user1Vehicle.getVehicleId();

        assertEquals(1, serviceRecordRepository.findByVehicleVehicleIdOrderByServiceDateDesc(vehicleId).size());
        assertEquals(1, fuelRecordRepository.findByVehicleVehicleIdOrderByFuelDateDesc(vehicleId).size());
        assertEquals(1, maintenanceRecordRepository.findByVehicleVehicleIdOrderByScheduledDateAsc(vehicleId).size());

        // Perform delete
        mockMvc.perform(post("/vehicles/" + vehicleId + "/delete")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/vehicles"))
                .andExpect(flash().attributeExists("successMsg"));

        // Verify vehicle and all child records are removed
        assertTrue(vehicleRepository.findById(vehicleId).isEmpty());
        assertTrue(serviceRecordRepository.findByVehicleVehicleIdOrderByServiceDateDesc(vehicleId).isEmpty());
        assertTrue(fuelRecordRepository.findByVehicleVehicleIdOrderByFuelDateDesc(vehicleId).isEmpty());
        assertTrue(maintenanceRecordRepository.findByVehicleVehicleIdOrderByScheduledDateAsc(vehicleId).isEmpty());
    }

    @Test
    @DisplayName("13. Web: Delete ANOTHER User's Vehicle is Forbidden")
    @WithMockUser(username = "veh_user2@test.com", roles = {"NORMAL_USER"})
    void testWebDeleteAnotherUserVehicleForbidden() throws Exception {
        mockMvc.perform(post("/vehicles/" + user1Vehicle.getVehicleId() + "/delete")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/vehicles"))
                .andExpect(flash().attributeExists("errorMsg"));

        // User 1's vehicle must STILL exist!
        assertTrue(vehicleRepository.findById(user1Vehicle.getVehicleId()).isPresent());
    }

    @Test
    @DisplayName("14. Web: Vehicle Timeline View for Own Vehicle")
    @WithMockUser(username = "veh_user1@test.com", roles = {"NORMAL_USER"})
    void testWebVehicleTimeline() throws Exception {
        mockMvc.perform(get("/vehicles/" + user1Vehicle.getVehicleId() + "/timeline"))
                .andExpect(status().isOk())
                .andExpect(view().name("vehicle/timeline"))
                .andExpect(model().attributeExists("vehicle"));
    }

    // =========================================================================
    // SECTION 2: REST API OPERATIONS (/api/vehicles)
    // =========================================================================

    @Test
    @DisplayName("15. REST: GET /api/vehicles returns User-Scoped List")
    @WithMockUser(username = "veh_user1@test.com", roles = {"NORMAL_USER"})
    void testRestGetVehiclesUserScoped() throws Exception {
        mockMvc.perform(get("/api/vehicles"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].plateNumber").value("TN01AB1234"));
    }

    @Test
    @DisplayName("16. REST: GET /api/vehicles/{id} for Own Vehicle returns 200")
    @WithMockUser(username = "veh_user1@test.com", roles = {"NORMAL_USER"})
    void testRestGetVehicleByIdSuccess() throws Exception {
        mockMvc.perform(get("/api/vehicles/" + user1Vehicle.getVehicleId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plateNumber").value("TN01AB1234"))
                .andExpect(jsonPath("$.make").value("Honda"))
                .andExpect(jsonPath("$.model").value("City"));
    }

    @Test
    @DisplayName("17. REST: GET /api/vehicles/{id} for ANOTHER User's Vehicle returns 400 Bad Request")
    @WithMockUser(username = "veh_user2@test.com", roles = {"NORMAL_USER"})
    void testRestGetVehicleByIdAnotherUserForbidden() throws Exception {
        mockMvc.perform(get("/api/vehicles/" + user1Vehicle.getVehicleId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Vehicle not found or access denied."));
    }

    @Test
    @DisplayName("18. REST: POST /api/vehicles Success returns 201 Created")
    @WithMockUser(username = "veh_user1@test.com", roles = {"NORMAL_USER"})
    void testRestPostVehicleSuccess() throws Exception {
        VehicleRequest req = new VehicleRequest();
        req.setCategoryId(sedanCat.getCategoryId());
        req.setPlateNumber("TN03EF9999");
        req.setMake("Skoda");
        req.setModel("Slavia");
        req.setYear(2023);
        req.setColor("Red");
        req.setFuelType("PETROL");
        req.setCurrentOdometer(5000);

        mockMvc.perform(post("/api/vehicles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.plateNumber").value("TN03EF9999"))
                .andExpect(jsonPath("$.make").value("Skoda"));

        assertTrue(vehicleRepository.existsByPlateNumberIgnoreCaseAndUserUserId("TN03EF9999", user1.getUserId()));
    }

    @Test
    @DisplayName("19. REST: POST /api/vehicles Validation Failure returns 400")
    @WithMockUser(username = "veh_user1@test.com", roles = {"NORMAL_USER"})
    void testRestPostVehicleValidationFailure() throws Exception {
        VehicleRequest invalidReq = new VehicleRequest();
        invalidReq.setPlateNumber(""); // blank
        invalidReq.setMake(""); // blank
        invalidReq.setModel(""); // blank
        invalidReq.setYear(1800); // invalid year

        mockMvc.perform(post("/api/vehicles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidReq)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("20. REST: PUT /api/vehicles/{id} Success returns 200")
    @WithMockUser(username = "veh_user1@test.com", roles = {"NORMAL_USER"})
    void testRestPutVehicleSuccess() throws Exception {
        VehicleRequest updateReq = new VehicleRequest();
        updateReq.setCategoryId(sedanCat.getCategoryId());
        updateReq.setPlateNumber("TN01AB1234");
        updateReq.setMake("Honda");
        updateReq.setModel("City Hybrid");
        updateReq.setYear(2022);
        updateReq.setColor("White");
        updateReq.setFuelType("HYBRID");
        updateReq.setCurrentOdometer(22000);

        mockMvc.perform(put("/api/vehicles/" + user1Vehicle.getVehicleId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.model").value("City Hybrid"))
                .andExpect(jsonPath("$.currentOdometer").value(22000));
    }

    @Test
    @DisplayName("21. REST: PUT /api/vehicles/{id} on ANOTHER User's Vehicle returns 400")
    @WithMockUser(username = "veh_user2@test.com", roles = {"NORMAL_USER"})
    void testRestPutVehicleAnotherUserForbidden() throws Exception {
        VehicleRequest req = new VehicleRequest();
        req.setCategoryId(sedanCat.getCategoryId());
        req.setPlateNumber("TN01AB1234");
        req.setMake("Honda");
        req.setModel("Hacked Model");
        req.setYear(2022);

        mockMvc.perform(put("/api/vehicles/" + user1Vehicle.getVehicleId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Vehicle not found or access denied."));
    }

    @Test
    @DisplayName("22. REST: DELETE /api/vehicles/{id} Success returns 200")
    @WithMockUser(username = "veh_user1@test.com", roles = {"NORMAL_USER"})
    void testRestDeleteVehicleSuccess() throws Exception {
        mockMvc.perform(delete("/api/vehicles/" + user1Vehicle.getVehicleId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Vehicle deleted successfully."));

        assertTrue(vehicleRepository.findById(user1Vehicle.getVehicleId()).isEmpty());
    }

    @Test
    @DisplayName("23. REST: DELETE /api/vehicles/{id} on ANOTHER User's Vehicle returns 400")
    @WithMockUser(username = "veh_user2@test.com", roles = {"NORMAL_USER"})
    void testRestDeleteVehicleAnotherUserForbidden() throws Exception {
        mockMvc.perform(delete("/api/vehicles/" + user1Vehicle.getVehicleId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Vehicle not found or access denied."));

        // User 1's vehicle must NOT be deleted
        assertTrue(vehicleRepository.findById(user1Vehicle.getVehicleId()).isPresent());
    }

    // =========================================================================
    // SECTION 3: ROLE & AUTHENTICATION BOUNDARIES
    // =========================================================================

    @Test
    @DisplayName("24. ADMIN Role Cannot Access User Vehicle Web Routes (403 Forbidden)")
    @WithMockUser(username = "veh_admin@test.com", roles = {"ADMIN"})
    void testAdminCannotAccessVehicleWeb() throws Exception {
        mockMvc.perform(get("/vehicles"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("25. ADMIN Role Cannot Access User Vehicle REST Endpoints (403 Forbidden)")
    @WithMockUser(username = "veh_admin@test.com", roles = {"ADMIN"})
    void testAdminCannotAccessVehicleRest() throws Exception {
        mockMvc.perform(get("/api/vehicles"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("26. Unauthenticated User is Redirected to /login")
    void testUnauthenticatedUserRedirected() throws Exception {
        mockMvc.perform(get("/vehicles"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        mockMvc.perform(get("/vehicles/add"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }
}