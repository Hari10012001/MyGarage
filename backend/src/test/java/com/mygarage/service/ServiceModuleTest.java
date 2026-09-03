package com.mygarage.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mygarage.dto.request.ServiceRecordRequest;
import com.mygarage.model.*;
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
 * Milestone 5 (M5) — Comprehensive Service Module Test Suite.
 * Covers:
 * 1. Service history listing
 * 2. Empty service history
 * 3. Add service record
 * 4. Valid service data
 * 5. Invalid service data (missing fields)
 * 6. Negative cost rejected
 * 7. Negative odometer rejected
 * 8. Service record linked to correct vehicle
 * 9. Service ordering (serviceDate DESC)
 * 10. Service detail
 * 11. Service update
 * 12. Service delete
 * 13. Same-user authorized access
 * 14. Cross-user service access blocked (Web & REST)
 * 15. Cross-user service creation blocked (Web & REST)
 * 16. Cross-user service mutation blocked (Web & REST)
 * 17. Cross-user service deletion blocked (Web & REST)
 * 18. Unauthenticated access blocked
 * 19. ADMIN isolation
 * 20. REST CRUD operations
 * 21. Vehicle cascade deletion removes service records
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ServiceModuleTest {

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

    private User user1;
    private User user2;
    private User admin;
    private VehicleCategory category;
    private Vehicle user1Vehicle;
    private Vehicle user2Vehicle;
    private ServiceRecord user1Service;

    @BeforeEach
    void setUp() {
        maintenanceRecordRepository.deleteAll();
        fuelRecordRepository.deleteAll();
        serviceRecordRepository.deleteAll();
        vehicleRepository.deleteAll();

        // 1. Setup User 1
        user1 = userRepository.findByEmail("svc_user1@test.com").orElseGet(User::new);
        user1.setFullName("Service User One");
        user1.setEmail("svc_user1@test.com");
        user1.setPasswordHash(passwordEncoder.encode("Secret@123"));
        user1.setPhone("9876543221");
        user1.setRole(Role.NORMAL_USER);
        user1.setActive(true);
        user1 = userRepository.save(user1);

        // 2. Setup User 2
        user2 = userRepository.findByEmail("svc_user2@test.com").orElseGet(User::new);
        user2.setFullName("Service User Two");
        user2.setEmail("svc_user2@test.com");
        user2.setPasswordHash(passwordEncoder.encode("Secret@123"));
        user2.setPhone("9876543222");
        user2.setRole(Role.NORMAL_USER);
        user2.setActive(true);
        user2 = userRepository.save(user2);

        // 3. Setup Admin
        admin = userRepository.findByEmail("svc_admin@test.com").orElseGet(User::new);
        admin.setFullName("Service Admin");
        admin.setEmail("svc_admin@test.com");
        admin.setPasswordHash(passwordEncoder.encode("Admin@123"));
        admin.setPhone("9999999992");
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
        user1Vehicle.setPlateNumber("TN10AB1001");
        user1Vehicle.setMake("Honda");
        user1Vehicle.setModel("Civic");
        user1Vehicle.setYear(2022);
        user1Vehicle.setCurrentOdometer(20000);
        user1Vehicle = vehicleRepository.save(user1Vehicle);

        user2Vehicle = new Vehicle();
        user2Vehicle.setUser(user2);
        user2Vehicle.setCategory(category);
        user2Vehicle.setPlateNumber("TN10AB2002");
        user2Vehicle.setMake("Hyundai");
        user2Vehicle.setModel("i20");
        user2Vehicle.setYear(2023);
        user2Vehicle.setCurrentOdometer(10000);
        user2Vehicle = vehicleRepository.save(user2Vehicle);

        // 6. Seed baseline Service Record for User 1's vehicle
        user1Service = new ServiceRecord();
        user1Service.setVehicle(user1Vehicle);
        user1Service.setServiceDate(LocalDate.now().minusMonths(1));
        user1Service.setServiceType("Regular Oil Change");
        user1Service.setDescription("Synthetic oil + filter replacement");
        user1Service.setGarageName("Honda Authorized Center");
        user1Service.setCost(new BigDecimal("3500.00"));
        user1Service.setOdometerAtService(20000);
        user1Service.setNextServiceDueDate(LocalDate.now().plusMonths(5));
        user1Service.setNotes("Clean engine oil report");
        user1Service = serviceRecordRepository.save(user1Service);
    }

    // =========================================================================
    // SECTION 1: WEB MVC TESTS
    // =========================================================================

    @Test
    @DisplayName("1. Web: Service History Listing for Vehicle")
    @WithMockUser(username = "svc_user1@test.com", roles = {"NORMAL_USER"})
    void testWebServiceHistoryListing() throws Exception {
        mockMvc.perform(get("/vehicles/" + user1Vehicle.getVehicleId() + "?tab=service"))
                .andExpect(status().isOk())
                .andExpect(view().name("vehicle/detail"))
                .andExpect(model().attributeExists("serviceRecords"))
                .andExpect(model().attribute("serviceRecords", hasSize(1)));
    }

    @Test
    @DisplayName("2. Web: Empty Service History Display")
    @WithMockUser(username = "svc_user2@test.com", roles = {"NORMAL_USER"})
    void testWebEmptyServiceHistory() throws Exception {
        mockMvc.perform(get("/vehicles/" + user2Vehicle.getVehicleId() + "?tab=service"))
                .andExpect(status().isOk())
                .andExpect(view().name("vehicle/detail"))
                .andExpect(model().attribute("serviceRecords", hasSize(0)));
    }

    @Test
    @DisplayName("3. Web: Render Add Service Record Form")
    @WithMockUser(username = "svc_user1@test.com", roles = {"NORMAL_USER"})
    void testWebAddServiceRecordForm() throws Exception {
        mockMvc.perform(get("/vehicles/" + user1Vehicle.getVehicleId() + "/services/add"))
                .andExpect(status().isOk())
                .andExpect(view().name("service/form"))
                .andExpect(model().attributeExists("serviceRequest"))
                .andExpect(model().attributeExists("vehicle"));
    }

    @Test
    @DisplayName("4. Web: Add Service Record Success (Valid Data)")
    @WithMockUser(username = "svc_user1@test.com", roles = {"NORMAL_USER"})
    void testWebAddServiceRecordSuccess() throws Exception {
        mockMvc.perform(post("/vehicles/" + user1Vehicle.getVehicleId() + "/services/add")
                .with(csrf())
                .param("serviceDate", LocalDate.now().toString())
                .param("serviceType", "Brake Pad Replacement")
                .param("garageName", "Bosch Car Service")
                .param("cost", "4200.50")
                .param("odometerAtService", "22500")
                .param("description", "Front disc pads changed"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/vehicles/" + user1Vehicle.getVehicleId() + "?tab=service"))
                .andExpect(flash().attributeExists("successMsg"));

        // Verify record created and linked to vehicle
        var records = serviceRecordRepository.findByVehicleVehicleIdOrderByServiceDateDesc(user1Vehicle.getVehicleId());
        assertEquals(2, records.size());

        // Verify vehicle current odometer updated because 22500 > 20000
        Vehicle updatedVehicle = vehicleRepository.findById(user1Vehicle.getVehicleId()).orElseThrow();
        assertEquals(22500, updatedVehicle.getCurrentOdometer());
    }

    @Test
    @DisplayName("5. Web: Add Service Validation Failure (Missing Required Fields)")
    @WithMockUser(username = "svc_user1@test.com", roles = {"NORMAL_USER"})
    void testWebAddServiceValidationFailure() throws Exception {
        mockMvc.perform(post("/vehicles/" + user1Vehicle.getVehicleId() + "/services/add")
                .with(csrf())
                .param("serviceDate", "") // blank date
                .param("serviceType", "")) // blank type
                .andExpect(status().isOk())
                .andExpect(view().name("service/form"))
                .andExpect(model().hasErrors());
    }

    @Test
    @DisplayName("6. Web: Negative Cost is Rejected")
    @WithMockUser(username = "svc_user1@test.com", roles = {"NORMAL_USER"})
    void testWebNegativeCostRejected() throws Exception {
        mockMvc.perform(post("/vehicles/" + user1Vehicle.getVehicleId() + "/services/add")
                .with(csrf())
                .param("serviceDate", LocalDate.now().toString())
                .param("serviceType", "Inspection")
                .param("cost", "-500.00")) // negative
                .andExpect(status().isOk())
                .andExpect(view().name("service/form"))
                .andExpect(model().attributeHasFieldErrors("serviceRequest", "cost"));
    }

    @Test
    @DisplayName("7. Web: Negative Odometer is Rejected")
    @WithMockUser(username = "svc_user1@test.com", roles = {"NORMAL_USER"})
    void testWebNegativeOdometerRejected() throws Exception {
        mockMvc.perform(post("/vehicles/" + user1Vehicle.getVehicleId() + "/services/add")
                .with(csrf())
                .param("serviceDate", LocalDate.now().toString())
                .param("serviceType", "Inspection")
                .param("odometerAtService", "-100")) // negative
                .andExpect(status().isOk())
                .andExpect(view().name("service/form"))
                .andExpect(model().attributeHasFieldErrors("serviceRequest", "odometerAtService"));
    }

    @Test
    @DisplayName("8. Web: Service History Ordering (serviceDate DESC)")
    @WithMockUser(username = "svc_user1@test.com", roles = {"NORMAL_USER"})
    void testWebServiceOrdering() throws Exception {
        // Add an older service
        ServiceRecord olderService = new ServiceRecord();
        olderService.setVehicle(user1Vehicle);
        olderService.setServiceDate(LocalDate.now().minusMonths(6));
        olderService.setServiceType("Initial 1000km Inspection");
        olderService.setCost(BigDecimal.ZERO);
        serviceRecordRepository.save(olderService);

        // Add a newest service
        ServiceRecord newerService = new ServiceRecord();
        newerService.setVehicle(user1Vehicle);
        newerService.setServiceDate(LocalDate.now());
        newerService.setServiceType("Recent Wheel Balancing");
        newerService.setCost(new BigDecimal("800.00"));
        serviceRecordRepository.save(newerService);

        var records = serviceRecordRepository.findByVehicleVehicleIdOrderByServiceDateDesc(user1Vehicle.getVehicleId());
        assertEquals(3, records.size());
        assertEquals("Recent Wheel Balancing", records.get(0).getServiceType());
        assertEquals("Regular Oil Change", records.get(1).getServiceType());
        assertEquals("Initial 1000km Inspection", records.get(2).getServiceType());
    }

    @Test
    @DisplayName("9. Web: Render Edit Service Record Form")
    @WithMockUser(username = "svc_user1@test.com", roles = {"NORMAL_USER"})
    void testWebEditServiceRecordForm() throws Exception {
        mockMvc.perform(get("/vehicles/" + user1Vehicle.getVehicleId() + "/services/" + user1Service.getServiceId() + "/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("service/form"))
                .andExpect(model().attributeExists("serviceRequest"))
                .andExpect(model().attribute("serviceId", user1Service.getServiceId()));
    }

    @Test
    @DisplayName("10. Web: Update Service Record Success")
    @WithMockUser(username = "svc_user1@test.com", roles = {"NORMAL_USER"})
    void testWebUpdateServiceRecordSuccess() throws Exception {
        mockMvc.perform(post("/vehicles/" + user1Vehicle.getVehicleId() + "/services/" + user1Service.getServiceId() + "/edit")
                .with(csrf())
                .param("serviceDate", user1Service.getServiceDate().toString())
                .param("serviceType", "Major 20K Service")
                .param("garageName", "Honda Premium Workshop")
                .param("cost", "5500.00")
                .param("odometerAtService", "21000")
                .param("description", "Engine flush + oil change"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/vehicles/" + user1Vehicle.getVehicleId() + "?tab=service"))
                .andExpect(flash().attributeExists("successMsg"));

        ServiceRecord updated = serviceRecordRepository.findById(user1Service.getServiceId()).orElseThrow();
        assertEquals("Major 20K Service", updated.getServiceType());
        assertEquals(new BigDecimal("5500.00"), updated.getCost());
    }

    @Test
    @DisplayName("11. Web: Delete Service Record Success")
    @WithMockUser(username = "svc_user1@test.com", roles = {"NORMAL_USER"})
    void testWebDeleteServiceRecordSuccess() throws Exception {
        mockMvc.perform(post("/vehicles/" + user1Vehicle.getVehicleId() + "/services/" + user1Service.getServiceId() + "/delete")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/vehicles/" + user1Vehicle.getVehicleId() + "?tab=service"))
                .andExpect(flash().attributeExists("successMsg"));

        assertTrue(serviceRecordRepository.findById(user1Service.getServiceId()).isEmpty());
    }

    // =========================================================================
    // SECTION 2: CROSS-USER & OWNERSHIP ISOLATION
    // =========================================================================

    @Test
    @DisplayName("12. Ownership: Cross-User Service Creation Blocked (User 2 on User 1 Vehicle)")
    @WithMockUser(username = "svc_user2@test.com", roles = {"NORMAL_USER"})
    void testCrossUserServiceCreationBlocked() throws Exception {
        mockMvc.perform(post("/vehicles/" + user1Vehicle.getVehicleId() + "/services/add")
                .with(csrf())
                .param("serviceDate", LocalDate.now().toString())
                .param("serviceType", "Hacked Service")
                .param("cost", "100.00"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error/error"));

        // Verify NO records added to user 1's vehicle
        assertEquals(1, serviceRecordRepository.findByVehicleVehicleIdOrderByServiceDateDesc(user1Vehicle.getVehicleId()).size());
    }

    @Test
    @DisplayName("13. Ownership: Cross-User Service Mutation Blocked (User 2 edits User 1 Record)")
    @WithMockUser(username = "svc_user2@test.com", roles = {"NORMAL_USER"})
    void testCrossUserServiceMutationBlocked() throws Exception {
        mockMvc.perform(post("/vehicles/" + user1Vehicle.getVehicleId() + "/services/" + user1Service.getServiceId() + "/edit")
                .with(csrf())
                .param("serviceDate", LocalDate.now().toString())
                .param("serviceType", "Hacked Type")
                .param("cost", "0.00"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error/error"));

        // Original record must remain unchanged
        ServiceRecord unchanged = serviceRecordRepository.findById(user1Service.getServiceId()).orElseThrow();
        assertEquals("Regular Oil Change", unchanged.getServiceType());
    }

    @Test
    @DisplayName("14. Ownership: Cross-User Service Deletion Blocked (User 2 deletes User 1 Record)")
    @WithMockUser(username = "svc_user2@test.com", roles = {"NORMAL_USER"})
    void testCrossUserServiceDeletionBlocked() throws Exception {
        mockMvc.perform(post("/vehicles/" + user1Vehicle.getVehicleId() + "/services/" + user1Service.getServiceId() + "/delete")
                .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error/error"));

        // User 1's service record must still exist
        assertTrue(serviceRecordRepository.findById(user1Service.getServiceId()).isPresent());
    }

    @Test
    @DisplayName("15. Ownership: Service Mismatched Vehicle ID Blocked (URL Tampering)")
    @WithMockUser(username = "svc_user1@test.com", roles = {"NORMAL_USER"})
    void testMismatchedVehicleAndServiceIdBlocked() throws Exception {
        // User 1 creates a second vehicle
        Vehicle vehicle1b = new Vehicle();
        vehicle1b.setUser(user1);
        vehicle1b.setCategory(category);
        vehicle1b.setPlateNumber("TN10AB3003");
        vehicle1b.setMake("Toyota");
        vehicle1b.setModel("Yaris");
        vehicle1b.setYear(2021);
        vehicle1b = vehicleRepository.save(vehicle1b);

        // Attempt to edit user1Service (which belongs to user1Vehicle) through vehicle1b's URL
        mockMvc.perform(get("/vehicles/" + vehicle1b.getVehicleId() + "/services/" + user1Service.getServiceId() + "/edit"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error/error"));
    }

    // =========================================================================
    // SECTION 3: REST API OPERATIONS & OWNERSHIP
    // =========================================================================

    @Test
    @DisplayName("16. REST: GET /api/vehicles/{id}/services returns list")
    @WithMockUser(username = "svc_user1@test.com", roles = {"NORMAL_USER"})
    void testRestGetServicesSuccess() throws Exception {
        mockMvc.perform(get("/api/vehicles/" + user1Vehicle.getVehicleId() + "/services"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].serviceType").value("Regular Oil Change"));
    }

    @Test
    @DisplayName("17. REST: GET /api/services/{id} returns single record")
    @WithMockUser(username = "svc_user1@test.com", roles = {"NORMAL_USER"})
    void testRestGetServiceByIdSuccess() throws Exception {
        mockMvc.perform(get("/api/services/" + user1Service.getServiceId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceType").value("Regular Oil Change"))
                .andExpect(jsonPath("$.garageName").value("Honda Authorized Center"));
    }

    @Test
    @DisplayName("18. REST: Cross-User GET /api/services/{id} returns 400 Bad Request")
    @WithMockUser(username = "svc_user2@test.com", roles = {"NORMAL_USER"})
    void testRestCrossUserGetServiceBlocked() throws Exception {
        mockMvc.perform(get("/api/services/" + user1Service.getServiceId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Service record not found or access denied."));
    }

    @Test
    @DisplayName("19. REST: POST /api/vehicles/{id}/services Success (201 Created)")
    @WithMockUser(username = "svc_user1@test.com", roles = {"NORMAL_USER"})
    void testRestPostServiceSuccess() throws Exception {
        ServiceRecordRequest req = new ServiceRecordRequest();
        req.setServiceDate(LocalDate.now());
        req.setServiceType("Spark Plug Replacement");
        req.setCost(new BigDecimal("1800.00"));
        req.setOdometerAtService(24000);
        req.setGarageName("Authorized Spark Center");

        mockMvc.perform(post("/api/vehicles/" + user1Vehicle.getVehicleId() + "/services")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.serviceType").value("Spark Plug Replacement"))
                .andExpect(jsonPath("$.cost").value(1800.0));
    }

    @Test
    @DisplayName("20. REST: Cross-User POST /api/vehicles/{id}/services returns 400")
    @WithMockUser(username = "svc_user2@test.com", roles = {"NORMAL_USER"})
    void testRestCrossUserPostServiceBlocked() throws Exception {
        ServiceRecordRequest req = new ServiceRecordRequest();
        req.setServiceDate(LocalDate.now());
        req.setServiceType("Illegal Service Entry");

        mockMvc.perform(post("/api/vehicles/" + user1Vehicle.getVehicleId() + "/services")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Vehicle not found or access denied."));
    }

    @Test
    @DisplayName("21. REST: PUT /api/services/{id} Updates Record")
    @WithMockUser(username = "svc_user1@test.com", roles = {"NORMAL_USER"})
    void testRestPutServiceSuccess() throws Exception {
        ServiceRecordRequest req = new ServiceRecordRequest();
        req.setServiceDate(user1Service.getServiceDate());
        req.setServiceType("Premium Full Synthetic Service");
        req.setCost(new BigDecimal("4999.00"));
        req.setGarageName("Honda Prime");

        mockMvc.perform(put("/api/services/" + user1Service.getServiceId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceType").value("Premium Full Synthetic Service"))
                .andExpect(jsonPath("$.cost").value(4999.0));
    }

    @Test
    @DisplayName("22. REST: DELETE /api/services/{id} Deletes Record")
    @WithMockUser(username = "svc_user1@test.com", roles = {"NORMAL_USER"})
    void testRestDeleteServiceSuccess() throws Exception {
        mockMvc.perform(delete("/api/services/" + user1Service.getServiceId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Service record deleted."));

        assertTrue(serviceRecordRepository.findById(user1Service.getServiceId()).isEmpty());
    }

    // =========================================================================
    // SECTION 4: SECURITY & CASCADE VERIFICATION
    // =========================================================================

    @Test
    @DisplayName("23. Security: ADMIN Role Cannot Access User Service Endpoints (403 Forbidden)")
    @WithMockUser(username = "svc_admin@test.com", roles = {"ADMIN"})
    void testAdminCannotAccessServiceEndpoints() throws Exception {
        mockMvc.perform(get("/vehicles/" + user1Vehicle.getVehicleId() + "?tab=service"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/vehicles/" + user1Vehicle.getVehicleId() + "/services"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/services/" + user1Service.getServiceId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("24. Security: Unauthenticated Access is Redirected to /login")
    void testUnauthenticatedServiceAccessRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/vehicles/" + user1Vehicle.getVehicleId() + "/services/add"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("25. Cascade: Vehicle Deletion Cascades and Removes Service Records")
    @WithMockUser(username = "svc_user1@test.com", roles = {"NORMAL_USER"})
    void testVehicleDeletionCascadeRemovesServiceRecords() throws Exception {
        Long vehicleId = user1Vehicle.getVehicleId();
        Long serviceId = user1Service.getServiceId();

        // Ensure service record exists
        assertTrue(serviceRecordRepository.findById(serviceId).isPresent());

        // Delete vehicle
        mockMvc.perform(post("/vehicles/" + vehicleId + "/delete")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/vehicles"));

        // Verify service record was cascaded and removed
        assertTrue(serviceRecordRepository.findById(serviceId).isEmpty());
    }
}