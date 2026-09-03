package com.mygarage.fuel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mygarage.dto.request.FuelRecordRequest;
import com.mygarage.model.*;
import com.mygarage.model.enums.FuelType;
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
 * Milestone 6 (M6) — Comprehensive Fuel Module Test Suite.
 * Covers 28 test cases:
 * 1. Fuel history listing
 * 2. Empty state
 * 3. Add fuel record
 * 4. Valid fuel data
 * 5. Invalid fuel data (missing required fields)
 * 6. Negative quantity rejected
 * 7. Negative rate rejected
 * 8. Zero/negative quantity validation
 * 9. Negative odometer rejected
 * 10. Fuel record linked to correct vehicle
 * 11. Fuel ordering (fuelDate DESC)
 * 12. Mileage calculation correctness
 * 13. Edit/update fuel record
 * 14. Delete fuel record
 * 15. Same-user access allowed
 * 16. Cross-user fuel access blocked (Web)
 * 17. Cross-user fuel creation blocked (Web)
 * 18. Cross-user fuel mutation blocked (Web)
 * 19. Cross-user fuel deletion blocked (Web)
 * 20. Mismatched vehicle/fuel IDs blocked (Web)
 * 21. Unauthenticated access blocked
 * 22. ADMIN isolation (Web & REST)
 * 23. REST GET list of fuel records
 * 24. REST GET fuel record by ID
 * 25. REST POST add fuel record
 * 26. REST PUT update fuel record
 * 27. REST DELETE fuel record
 * 28. REST cross-user ownership protection & Vehicle cascade deletion
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FuelModuleTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VehicleCategoryRepository categoryRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private FuelRecordRepository fuelRecordRepository;

    @Autowired
    private ServiceRecordRepository serviceRecordRepository;

    @Autowired
    private MaintenanceRecordRepository maintenanceRecordRepository;

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
    private FuelRecord user1InitialFuel;

    @BeforeEach
    void setUp() {
        maintenanceRecordRepository.deleteAll();
        serviceRecordRepository.deleteAll();
        fuelRecordRepository.deleteAll();
        vehicleRepository.deleteAll();

        // 1. Setup User 1
        user1 = userRepository.findByEmail("fuel_user1@test.com").orElseGet(User::new);
        user1.setFullName("Fuel User One");
        user1.setEmail("fuel_user1@test.com");
        user1.setPasswordHash(passwordEncoder.encode("Secret@123"));
        user1.setPhone("9876543111");
        user1.setRole(Role.NORMAL_USER);
        user1.setActive(true);
        user1 = userRepository.save(user1);

        // 2. Setup User 2
        user2 = userRepository.findByEmail("fuel_user2@test.com").orElseGet(User::new);
        user2.setFullName("Fuel User Two");
        user2.setEmail("fuel_user2@test.com");
        user2.setPasswordHash(passwordEncoder.encode("Secret@123"));
        user2.setPhone("9876543112");
        user2.setRole(Role.NORMAL_USER);
        user2.setActive(true);
        user2 = userRepository.save(user2);

        // 3. Setup Admin
        admin = userRepository.findByEmail("fuel_admin@test.com").orElseGet(User::new);
        admin.setFullName("Fuel Admin");
        admin.setEmail("fuel_admin@test.com");
        admin.setPasswordHash(passwordEncoder.encode("Admin@123"));
        admin.setPhone("9999999111");
        admin.setRole(Role.ADMIN);
        admin.setActive(true);
        admin = userRepository.save(admin);

        // 4. Setup Category
        category = categoryRepository.findByNameIgnoreCase("Sedan").orElseGet(() -> {
            VehicleCategory cat = new VehicleCategory();
            cat.setName("Sedan");
            cat.setIcon("🚗");
            return categoryRepository.save(cat);
        });

        // 5. Seed Vehicles
        user1Vehicle = new Vehicle();
        user1Vehicle.setUser(user1);
        user1Vehicle.setCategory(category);
        user1Vehicle.setPlateNumber("TN09FL1001");
        user1Vehicle.setMake("Toyota");
        user1Vehicle.setModel("Corolla");
        user1Vehicle.setYear(2021);
        user1Vehicle.setCurrentOdometer(10000);
        user1Vehicle = vehicleRepository.save(user1Vehicle);

        user2Vehicle = new Vehicle();
        user2Vehicle.setUser(user2);
        user2Vehicle.setCategory(category);
        user2Vehicle.setPlateNumber("TN09FL2002");
        user2Vehicle.setMake("Honda");
        user2Vehicle.setModel("City");
        user2Vehicle.setYear(2022);
        user2Vehicle.setCurrentOdometer(8000);
        user2Vehicle = vehicleRepository.save(user2Vehicle);

        // 6. Seed Baseline Fuel Record for User 1
        user1InitialFuel = new FuelRecord();
        user1InitialFuel.setVehicle(user1Vehicle);
        user1InitialFuel.setFuelDate(LocalDate.now().minusDays(10));
        user1InitialFuel.setFuelType(FuelType.PETROL);
        user1InitialFuel.setQuantityLitres(new BigDecimal("30.00"));
        user1InitialFuel.setCostPerLitre(new BigDecimal("100.00"));
        user1InitialFuel.setOdometerAtFill(10000);
        user1InitialFuel.setNotes("First full tank");
        user1InitialFuel = fuelRecordRepository.save(user1InitialFuel);
    }

    // =========================================================================
    // SECTION 1: WEB MVC TESTS
    // =========================================================================

    @Test
    @DisplayName("1. Web: Fuel History Listing for Vehicle")
    @WithMockUser(username = "fuel_user1@test.com", roles = {"NORMAL_USER"})
    void testWebFuelHistoryListing() throws Exception {
        mockMvc.perform(get("/vehicles/" + user1Vehicle.getVehicleId() + "?tab=fuel"))
                .andExpect(status().isOk())
                .andExpect(view().name("vehicle/detail"))
                .andExpect(model().attributeExists("fuelRecords"))
                .andExpect(model().attribute("fuelRecords", hasSize(1)));
    }

    @Test
    @DisplayName("2. Web: Empty Fuel History Display")
    @WithMockUser(username = "fuel_user2@test.com", roles = {"NORMAL_USER"})
    void testWebEmptyFuelHistory() throws Exception {
        mockMvc.perform(get("/vehicles/" + user2Vehicle.getVehicleId() + "?tab=fuel"))
                .andExpect(status().isOk())
                .andExpect(view().name("vehicle/detail"))
                .andExpect(model().attribute("fuelRecords", hasSize(0)));
    }

    @Test
    @DisplayName("3. Web: Render Add Fuel Record Form")
    @WithMockUser(username = "fuel_user1@test.com", roles = {"NORMAL_USER"})
    void testWebAddFuelRecordForm() throws Exception {
        mockMvc.perform(get("/vehicles/" + user1Vehicle.getVehicleId() + "/fuel/add"))
                .andExpect(status().isOk())
                .andExpect(view().name("fuel/form"))
                .andExpect(model().attributeExists("fuelRequest"))
                .andExpect(model().attributeExists("fuelTypes"))
                .andExpect(model().attributeExists("vehicle"));
    }

    @Test
    @DisplayName("4. Web: Add Fuel Record Success (Valid Data & Odometer Update)")
    @WithMockUser(username = "fuel_user1@test.com", roles = {"NORMAL_USER"})
    void testWebAddFuelRecordSuccess() throws Exception {
        mockMvc.perform(post("/vehicles/" + user1Vehicle.getVehicleId() + "/fuel/add")
                .with(csrf())
                .param("fuelDate", LocalDate.now().toString())
                .param("fuelType", "PETROL")
                .param("quantityLitres", "25.00")
                .param("costPerLitre", "102.50")
                .param("odometerAtFill", "10500")
                .param("notes", "Regular tank fill"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/vehicles/" + user1Vehicle.getVehicleId() + "?tab=fuel"))
                .andExpect(flash().attributeExists("successMsg"));

        // Verify fuel record was created
        var records = fuelRecordRepository.findByVehicleVehicleIdOrderByFuelDateDesc(user1Vehicle.getVehicleId());
        assertEquals(2, records.size());

        // Verify vehicle current odometer was updated to 10500
        Vehicle updatedVehicle = vehicleRepository.findById(user1Vehicle.getVehicleId()).orElseThrow();
        assertEquals(10500, updatedVehicle.getCurrentOdometer());
    }

    @Test
    @DisplayName("5. Web: Add Fuel Validation Failure (Missing Required Fields)")
    @WithMockUser(username = "fuel_user1@test.com", roles = {"NORMAL_USER"})
    void testWebAddFuelValidationFailure() throws Exception {
        mockMvc.perform(post("/vehicles/" + user1Vehicle.getVehicleId() + "/fuel/add")
                .with(csrf())
                .param("fuelDate", "") // missing date
                .param("quantityLitres", "")) // missing quantity
                .andExpect(status().isOk())
                .andExpect(view().name("fuel/form"))
                .andExpect(model().hasErrors());
    }

    @Test
    @DisplayName("6. Web: Negative Quantity is Rejected")
    @WithMockUser(username = "fuel_user1@test.com", roles = {"NORMAL_USER"})
    void testWebNegativeQuantityRejected() throws Exception {
        mockMvc.perform(post("/vehicles/" + user1Vehicle.getVehicleId() + "/fuel/add")
                .with(csrf())
                .param("fuelDate", LocalDate.now().toString())
                .param("fuelType", "PETROL")
                .param("quantityLitres", "-15.00") // negative
                .param("costPerLitre", "100.00"))
                .andExpect(status().isOk())
                .andExpect(view().name("fuel/form"))
                .andExpect(model().attributeHasFieldErrors("fuelRequest", "quantityLitres"));
    }

    @Test
    @DisplayName("7. Web: Negative Rate is Rejected")
    @WithMockUser(username = "fuel_user1@test.com", roles = {"NORMAL_USER"})
    void testWebNegativeRateRejected() throws Exception {
        mockMvc.perform(post("/vehicles/" + user1Vehicle.getVehicleId() + "/fuel/add")
                .with(csrf())
                .param("fuelDate", LocalDate.now().toString())
                .param("fuelType", "PETROL")
                .param("quantityLitres", "20.00")
                .param("costPerLitre", "-50.00")) // negative
                .andExpect(status().isOk())
                .andExpect(view().name("fuel/form"))
                .andExpect(model().attributeHasFieldErrors("fuelRequest", "costPerLitre"));
    }

    @Test
    @DisplayName("8. Web: Negative Odometer is Rejected")
    @WithMockUser(username = "fuel_user1@test.com", roles = {"NORMAL_USER"})
    void testWebNegativeOdometerRejected() throws Exception {
        mockMvc.perform(post("/vehicles/" + user1Vehicle.getVehicleId() + "/fuel/add")
                .with(csrf())
                .param("fuelDate", LocalDate.now().toString())
                .param("fuelType", "PETROL")
                .param("quantityLitres", "20.00")
                .param("costPerLitre", "100.00")
                .param("odometerAtFill", "-500")) // negative
                .andExpect(status().isOk())
                .andExpect(view().name("fuel/form"))
                .andExpect(model().attributeHasFieldErrors("fuelRequest", "odometerAtFill"));
    }

    @Test
    @DisplayName("9. Web: Fuel Record Linked to Correct Vehicle")
    @WithMockUser(username = "fuel_user1@test.com", roles = {"NORMAL_USER"})
    void testFuelRecordLinkedToCorrectVehicle() throws Exception {
        var records = fuelRecordRepository.findByVehicleVehicleIdOrderByFuelDateDesc(user1Vehicle.getVehicleId());
        assertFalse(records.isEmpty());
        assertEquals(user1Vehicle.getVehicleId(), records.get(0).getVehicle().getVehicleId());
    }

    @Test
    @DisplayName("10. Web: Fuel Ordering (fuelDate DESC)")
    @WithMockUser(username = "fuel_user1@test.com", roles = {"NORMAL_USER"})
    void testWebFuelOrdering() throws Exception {
        // Add an older fuel fill
        FuelRecord older = new FuelRecord();
        older.setVehicle(user1Vehicle);
        older.setFuelDate(LocalDate.now().minusDays(20));
        older.setFuelType(FuelType.PETROL);
        older.setQuantityLitres(new BigDecimal("20.00"));
        older.setCostPerLitre(new BigDecimal("98.00"));
        fuelRecordRepository.save(older);

        // Add a newest fuel fill
        FuelRecord newer = new FuelRecord();
        newer.setVehicle(user1Vehicle);
        newer.setFuelDate(LocalDate.now());
        newer.setFuelType(FuelType.PETROL);
        newer.setQuantityLitres(new BigDecimal("25.00"));
        newer.setCostPerLitre(new BigDecimal("103.00"));
        fuelRecordRepository.save(newer);

        var records = fuelRecordRepository.findByVehicleVehicleIdOrderByFuelDateDesc(user1Vehicle.getVehicleId());
        assertEquals(3, records.size());
        assertEquals(LocalDate.now(), records.get(0).getFuelDate());
        assertEquals(LocalDate.now().minusDays(10), records.get(1).getFuelDate());
        assertEquals(LocalDate.now().minusDays(20), records.get(2).getFuelDate());
    }

    @Test
    @DisplayName("11. Mileage: Estimated Mileage Calculation (Distance / Litres)")
    @WithMockUser(username = "fuel_user1@test.com", roles = {"NORMAL_USER"})
    void testEstimatedMileageCalculation() throws Exception {
        // Initial record was 10,000 km
        // Add second record on a later date at 10,400 km with 20.00 Litres
        // Expected distance = 400 km. Expected mileage = 400 / 20 = 20.00 km/L
        mockMvc.perform(post("/vehicles/" + user1Vehicle.getVehicleId() + "/fuel/add")
                .with(csrf())
                .param("fuelDate", LocalDate.now().toString())
                .param("fuelType", "PETROL")
                .param("quantityLitres", "20.00")
                .param("costPerLitre", "100.00")
                .param("odometerAtFill", "10400"))
                .andExpect(status().is3xxRedirection());

        var records = fuelRecordRepository.findByVehicleVehicleIdOrderByFuelDateDesc(user1Vehicle.getVehicleId());
        assertEquals(2, records.size());
        FuelRecord latest = records.get(0);
        assertNotNull(latest.getEstimatedMileageKmpl());
        assertEquals(new BigDecimal("20.00"), latest.getEstimatedMileageKmpl());
    }

    @Test
    @DisplayName("12. Web: Render Edit Fuel Record Form")
    @WithMockUser(username = "fuel_user1@test.com", roles = {"NORMAL_USER"})
    void testWebEditFuelRecordForm() throws Exception {
        mockMvc.perform(get("/vehicles/" + user1Vehicle.getVehicleId() + "/fuel/" + user1InitialFuel.getFuelId() + "/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("fuel/form"))
                .andExpect(model().attributeExists("fuelRequest"))
                .andExpect(model().attribute("fuelId", user1InitialFuel.getFuelId()));
    }

    @Test
    @DisplayName("13. Web: Update Fuel Record Success")
    @WithMockUser(username = "fuel_user1@test.com", roles = {"NORMAL_USER"})
    void testWebUpdateFuelRecordSuccess() throws Exception {
        mockMvc.perform(post("/vehicles/" + user1Vehicle.getVehicleId() + "/fuel/" + user1InitialFuel.getFuelId() + "/edit")
                .with(csrf())
                .param("fuelDate", user1InitialFuel.getFuelDate().toString())
                .param("fuelType", "PETROL")
                .param("quantityLitres", "35.00")
                .param("costPerLitre", "105.00")
                .param("odometerAtFill", "10100")
                .param("notes", "Updated to full tank"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/vehicles/" + user1Vehicle.getVehicleId() + "?tab=fuel"))
                .andExpect(flash().attributeExists("successMsg"));

        FuelRecord updated = fuelRecordRepository.findById(user1InitialFuel.getFuelId()).orElseThrow();
        assertEquals(new BigDecimal("35.00"), updated.getQuantityLitres());
        assertEquals(new BigDecimal("105.00"), updated.getCostPerLitre());
        assertEquals(new BigDecimal("3675.00"), updated.getTotalCost());
    }

    @Test
    @DisplayName("14. Web: Delete Fuel Record Success")
    @WithMockUser(username = "fuel_user1@test.com", roles = {"NORMAL_USER"})
    void testWebDeleteFuelRecordSuccess() throws Exception {
        mockMvc.perform(post("/vehicles/" + user1Vehicle.getVehicleId() + "/fuel/" + user1InitialFuel.getFuelId() + "/delete")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/vehicles/" + user1Vehicle.getVehicleId() + "?tab=fuel"))
                .andExpect(flash().attributeExists("successMsg"));

        assertTrue(fuelRecordRepository.findById(user1InitialFuel.getFuelId()).isEmpty());
    }

    // =========================================================================
    // SECTION 2: OWNERSHIP & CROSS-USER ISOLATION
    // =========================================================================

    @Test
    @DisplayName("15. Ownership: Same-User Access Allowed")
    @WithMockUser(username = "fuel_user1@test.com", roles = {"NORMAL_USER"})
    void testSameUserAccessAllowed() throws Exception {
        mockMvc.perform(get("/vehicles/" + user1Vehicle.getVehicleId() + "?tab=fuel"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("16. Ownership: Cross-User Fuel History Blocked (User 2 on User 1 Vehicle)")
    @WithMockUser(username = "fuel_user2@test.com", roles = {"NORMAL_USER"})
    void testCrossUserFuelHistoryBlocked() throws Exception {
        mockMvc.perform(get("/vehicles/" + user1Vehicle.getVehicleId() + "?tab=fuel"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error/error"));
    }

    @Test
    @DisplayName("17. Ownership: Cross-User Fuel Creation Blocked (User 2 on User 1 Vehicle)")
    @WithMockUser(username = "fuel_user2@test.com", roles = {"NORMAL_USER"})
    void testCrossUserFuelCreationBlocked() throws Exception {
        mockMvc.perform(post("/vehicles/" + user1Vehicle.getVehicleId() + "/fuel/add")
                .with(csrf())
                .param("fuelDate", LocalDate.now().toString())
                .param("fuelType", "PETROL")
                .param("quantityLitres", "10.00")
                .param("costPerLitre", "100.00"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error/error"));

        assertEquals(1, fuelRecordRepository.findByVehicleVehicleIdOrderByFuelDateDesc(user1Vehicle.getVehicleId()).size());
    }

    @Test
    @DisplayName("18. Ownership: Cross-User Fuel Mutation Blocked (User 2 edits User 1 Record)")
    @WithMockUser(username = "fuel_user2@test.com", roles = {"NORMAL_USER"})
    void testCrossUserFuelMutationBlocked() throws Exception {
        mockMvc.perform(post("/vehicles/" + user1Vehicle.getVehicleId() + "/fuel/" + user1InitialFuel.getFuelId() + "/edit")
                .with(csrf())
                .param("fuelDate", LocalDate.now().toString())
                .param("fuelType", "PETROL")
                .param("quantityLitres", "50.00")
                .param("costPerLitre", "90.00"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error/error"));

        // Value must remain unchanged
        FuelRecord unchanged = fuelRecordRepository.findById(user1InitialFuel.getFuelId()).orElseThrow();
        assertEquals(new BigDecimal("30.00"), unchanged.getQuantityLitres());
    }

    @Test
    @DisplayName("19. Ownership: Cross-User Fuel Deletion Blocked (User 2 deletes User 1 Record)")
    @WithMockUser(username = "fuel_user2@test.com", roles = {"NORMAL_USER"})
    void testCrossUserFuelDeletionBlocked() throws Exception {
        mockMvc.perform(post("/vehicles/" + user1Vehicle.getVehicleId() + "/fuel/" + user1InitialFuel.getFuelId() + "/delete")
                .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error/error"));

        assertTrue(fuelRecordRepository.findById(user1InitialFuel.getFuelId()).isPresent());
    }

    @Test
    @DisplayName("20. Ownership: Mismatched Vehicle/Fuel ID Blocked (URL Tampering)")
    @WithMockUser(username = "fuel_user1@test.com", roles = {"NORMAL_USER"})
    void testMismatchedVehicleAndFuelIdBlocked() throws Exception {
        // User 1 creates another vehicle
        Vehicle vehicle1b = new Vehicle();
        vehicle1b.setUser(user1);
        vehicle1b.setCategory(category);
        vehicle1b.setPlateNumber("TN09FL3003");
        vehicle1b.setMake("Toyota");
        vehicle1b.setModel("Camry");
        vehicle1b.setYear(2023);
        vehicle1b = vehicleRepository.save(vehicle1b);

        // Attempt to access user1InitialFuel (attached to user1Vehicle) through vehicle1b's URL
        mockMvc.perform(get("/vehicles/" + vehicle1b.getVehicleId() + "/fuel/" + user1InitialFuel.getFuelId() + "/edit"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error/error"));
    }

    // =========================================================================
    // SECTION 3: SECURITY & ADMIN ISOLATION
    // =========================================================================

    @Test
    @DisplayName("21. Security: Unauthenticated Access Redirects to /login")
    void testUnauthenticatedAccessRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/vehicles/" + user1Vehicle.getVehicleId() + "/fuel/add"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("22. Security: ADMIN Role Cannot Access User Fuel Endpoints (403 Forbidden)")
    @WithMockUser(username = "fuel_admin@test.com", roles = {"ADMIN"})
    void testAdminCannotAccessFuelEndpoints() throws Exception {
        mockMvc.perform(get("/vehicles/" + user1Vehicle.getVehicleId() + "?tab=fuel"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/vehicles/" + user1Vehicle.getVehicleId() + "/fuel"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/fuel/" + user1InitialFuel.getFuelId()))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // SECTION 4: REST API CRUD & OWNERSHIP
    // =========================================================================

    @Test
    @DisplayName("23. REST: GET /api/vehicles/{id}/fuel Returns List")
    @WithMockUser(username = "fuel_user1@test.com", roles = {"NORMAL_USER"})
    void testRestGetFuelRecordsSuccess() throws Exception {
        mockMvc.perform(get("/api/vehicles/" + user1Vehicle.getVehicleId() + "/fuel"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].fuelType").value("PETROL"))
                .andExpect(jsonPath("$[0].quantityLitres").value(30.0));
    }

    @Test
    @DisplayName("24. REST: GET /api/fuel/{id} Returns Single Fuel Record")
    @WithMockUser(username = "fuel_user1@test.com", roles = {"NORMAL_USER"})
    void testRestGetFuelByIdSuccess() throws Exception {
        mockMvc.perform(get("/api/fuel/" + user1InitialFuel.getFuelId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fuelType").value("PETROL"))
                .andExpect(jsonPath("$.quantityLitres").value(30.0))
                .andExpect(jsonPath("$.totalCost").value(3000.0));
    }

    @Test
    @DisplayName("25. REST: POST /api/vehicles/{id}/fuel Success (201 Created)")
    @WithMockUser(username = "fuel_user1@test.com", roles = {"NORMAL_USER"})
    void testRestPostFuelSuccess() throws Exception {
        FuelRecordRequest req = new FuelRecordRequest();
        req.setFuelDate(LocalDate.now());
        req.setFuelType(FuelType.PETROL);
        req.setQuantityLitres(new BigDecimal("22.50"));
        req.setCostPerLitre(new BigDecimal("103.00"));
        req.setOdometerAtFill(10350);

        mockMvc.perform(post("/api/vehicles/" + user1Vehicle.getVehicleId() + "/fuel")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quantityLitres").value(22.5))
                .andExpect(jsonPath("$.totalCost").value(2317.50));
    }

    @Test
    @DisplayName("26. REST: PUT /api/fuel/{id} Updates Record (200 OK)")
    @WithMockUser(username = "fuel_user1@test.com", roles = {"NORMAL_USER"})
    void testRestPutFuelSuccess() throws Exception {
        FuelRecordRequest req = new FuelRecordRequest();
        req.setFuelDate(user1InitialFuel.getFuelDate());
        req.setFuelType(FuelType.PETROL);
        req.setQuantityLitres(new BigDecimal("40.00"));
        req.setCostPerLitre(new BigDecimal("101.00"));
        req.setOdometerAtFill(10050);

        mockMvc.perform(put("/api/fuel/" + user1InitialFuel.getFuelId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantityLitres").value(40.0))
                .andExpect(jsonPath("$.totalCost").value(4040.00));
    }

    @Test
    @DisplayName("27. REST: DELETE /api/fuel/{id} Deletes Record (200 OK)")
    @WithMockUser(username = "fuel_user1@test.com", roles = {"NORMAL_USER"})
    void testRestDeleteFuelSuccess() throws Exception {
        mockMvc.perform(delete("/api/fuel/" + user1InitialFuel.getFuelId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Fuel record deleted."));

        assertTrue(fuelRecordRepository.findById(user1InitialFuel.getFuelId()).isEmpty());
    }

    @Test
    @DisplayName("28. REST: Cross-User Ownership Blocked & Cascade Deletion Removes Fuel Records")
    @WithMockUser(username = "fuel_user2@test.com", roles = {"NORMAL_USER"})
    void testRestCrossUserBlockedAndCascadeDelete() throws Exception {
        // User 2 attempts to GET User 1's fuel record -> 400 Bad Request
        mockMvc.perform(get("/api/fuel/" + user1InitialFuel.getFuelId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Fuel record not found or access denied."));

        // User 2 attempts to POST against User 1's vehicle -> 400 Bad Request
        FuelRecordRequest req = new FuelRecordRequest();
        req.setFuelDate(LocalDate.now());
        req.setFuelType(FuelType.PETROL);
        req.setQuantityLitres(new BigDecimal("10.00"));
        req.setCostPerLitre(new BigDecimal("100.00"));

        mockMvc.perform(post("/api/vehicles/" + user1Vehicle.getVehicleId() + "/fuel")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Vehicle not found or access denied."));

        // Cascade Deletion Verification
        Long vId = user1Vehicle.getVehicleId();
        Long fId = user1InitialFuel.getFuelId();
        assertTrue(fuelRecordRepository.findById(fId).isPresent());

        // Delete vehicle by owner
        vehicleRepository.deleteById(vId);

        // Fuel record must be completely cascaded and deleted
        assertTrue(fuelRecordRepository.findById(fId).isEmpty());
    }
}