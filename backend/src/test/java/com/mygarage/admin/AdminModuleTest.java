package com.mygarage.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Milestone 9 (M9) — Comprehensive Admin & System Management Module Test Suite.
 * Covers 30 test cases:
 * 1. Security: ADMIN accesses /admin/dashboard (200 OK)
 * 2. Security: ADMIN accesses /admin/statistics (200 OK)
 * 3. Security: NORMAL_USER blocked from /admin/dashboard (403 Forbidden)
 * 4. Security: NORMAL_USER blocked from /admin/users (403 Forbidden)
 * 5. Security: NORMAL_USER blocked from /admin/categories (403 Forbidden)
 * 6. Security: NORMAL_USER blocked from /admin/statistics (403 Forbidden)
 * 7. Security: Unauthenticated to /admin/dashboard redirects to /login
 * 8. Security: Unauthenticated to /admin/users redirects to /login
 * 9. Web: User listing renders normal users
 * 10. Web: User search filters by query
 * 11. Web: User toggle active -> disabled
 * 12. Web: User toggle disabled -> active
 * 13. Security: Primary Admin cannot be deactivated
 * 14. Web: Category listing displays categories
 * 15. Web: Add category success
 * 16. Web: Add duplicate category rejected with error message
 * 17. Web: Edit category success
 * 18. Web: Delete category without vehicles success
 * 19. Web: Delete category with vehicles assigned blocked
 * 20. CSRF: Category add without CSRF rejected (403 Forbidden)
 * 21. CSRF: User toggle without CSRF rejected (403 Forbidden)
 * 22. REST: GET /api/admin/statistics returns system metrics
 * 23. REST: GET /api/admin/users returns normal users
 * 24. REST: GET /api/admin/users?search= filters users
 * 25. REST: POST /api/admin/users/{id}/toggle toggles active state
 * 26. REST: Primary Admin toggle rejected (400 Bad Request)
 * 27. REST: Category CRUD (GET, POST, PUT, DELETE)
 * 28. REST: Delete Category with vehicles assigned rejected (400 Bad Request)
 * 29. REST: NORMAL_USER blocked from all /api/admin/** (403 Forbidden)
 * 30. Security: Role escalation prevention (Normal user cannot register as ADMIN)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminModuleTest {

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

    private User primaryAdmin;
    private User normalUser1;
    private User normalUser2;
    private VehicleCategory categoryWithVehicle;
    private VehicleCategory categoryEmpty;
    private Vehicle sampleVehicle;

    @BeforeEach
    void setUp() {
        maintenanceRecordRepository.deleteAll();
        serviceRecordRepository.deleteAll();
        fuelRecordRepository.deleteAll();
        vehicleRepository.deleteAll();

        // 1. Primary Admin
        primaryAdmin = userRepository.findByEmail("admin@mygarage.com").orElseGet(User::new);
        primaryAdmin.setFullName("System Administrator");
        primaryAdmin.setEmail("admin@mygarage.com");
        primaryAdmin.setPasswordHash(passwordEncoder.encode("Admin@123"));
        primaryAdmin.setRole(Role.ADMIN);
        primaryAdmin.setActive(true);
        primaryAdmin = userRepository.save(primaryAdmin);

        // 2. Normal Users
        normalUser1 = userRepository.findByEmail("admin_test_u1@test.com").orElseGet(User::new);
        normalUser1.setFullName("Alice Walker");
        normalUser1.setEmail("admin_test_u1@test.com");
        normalUser1.setPasswordHash(passwordEncoder.encode("User@123"));
        normalUser1.setRole(Role.NORMAL_USER);
        normalUser1.setActive(true);
        normalUser1 = userRepository.save(normalUser1);

        normalUser2 = userRepository.findByEmail("admin_test_u2@test.com").orElseGet(User::new);
        normalUser2.setFullName("Bob Martin");
        normalUser2.setEmail("admin_test_u2@test.com");
        normalUser2.setPasswordHash(passwordEncoder.encode("User@123"));
        normalUser2.setRole(Role.NORMAL_USER);
        normalUser2.setActive(false); // Inactive initially
        normalUser2 = userRepository.save(normalUser2);

        // 3. Categories
        categoryWithVehicle = categoryRepository.findByNameIgnoreCase("Sedan Admin").orElseGet(() -> {
            VehicleCategory cat = new VehicleCategory();
            cat.setName("Sedan Admin");
            cat.setIcon("🚗");
            return categoryRepository.save(cat);
        });

        categoryEmpty = categoryRepository.findByNameIgnoreCase("Motorcycle Admin").orElseGet(() -> {
            VehicleCategory cat = new VehicleCategory();
            cat.setName("Motorcycle Admin");
            cat.setIcon("🏍️");
            return categoryRepository.save(cat);
        });

        // 4. Vehicle assigned to categoryWithVehicle
        sampleVehicle = new Vehicle();
        sampleVehicle.setUser(normalUser1);
        sampleVehicle.setCategory(categoryWithVehicle);
        sampleVehicle.setPlateNumber("KA01AD1001");
        sampleVehicle.setMake("Honda");
        sampleVehicle.setModel("City");
        sampleVehicle.setYear(2023);
        sampleVehicle = vehicleRepository.save(sampleVehicle);
    }

    // =========================================================================
    // SECTION 1: SECURITY & RBAC ACCESS TESTS
    // =========================================================================

    @Test
    @DisplayName("1. Security: ADMIN accesses /admin/dashboard (200 OK)")
    @WithMockUser(username = "admin@mygarage.com", roles = {"ADMIN"})
    void testAdminAccessDashboard() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"))
                .andExpect(model().attributeExists("totalUsers"))
                .andExpect(model().attributeExists("totalVehicles"));
    }

    @Test
    @DisplayName("2. Security: ADMIN accesses /admin/statistics (200 OK)")
    @WithMockUser(username = "admin@mygarage.com", roles = {"ADMIN"})
    void testAdminAccessStatistics() throws Exception {
        mockMvc.perform(get("/admin/statistics"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/statistics"))
                .andExpect(model().attributeExists("totalUsers"));
    }

    @Test
    @DisplayName("3. Security: NORMAL_USER blocked from /admin/dashboard (403 Forbidden)")
    @WithMockUser(username = "admin_test_u1@test.com", roles = {"NORMAL_USER"})
    void testUserBlockedFromAdminDashboard() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("4. Security: NORMAL_USER blocked from /admin/users (403 Forbidden)")
    @WithMockUser(username = "admin_test_u1@test.com", roles = {"NORMAL_USER"})
    void testUserBlockedFromAdminUsers() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("5. Security: NORMAL_USER blocked from /admin/categories (403 Forbidden)")
    @WithMockUser(username = "admin_test_u1@test.com", roles = {"NORMAL_USER"})
    void testUserBlockedFromAdminCategories() throws Exception {
        mockMvc.perform(get("/admin/categories"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("6. Security: NORMAL_USER blocked from /admin/statistics (403 Forbidden)")
    @WithMockUser(username = "admin_test_u1@test.com", roles = {"NORMAL_USER"})
    void testUserBlockedFromAdminStatistics() throws Exception {
        mockMvc.perform(get("/admin/statistics"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("7. Security: Unauthenticated to /admin/dashboard redirects to /login")
    void testUnauthenticatedRedirectsDashboard() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("8. Security: Unauthenticated to /admin/users redirects to /login")
    void testUnauthenticatedRedirectsUsers() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    // =========================================================================
    // SECTION 2: USER MANAGEMENT (WEB MVC)
    // =========================================================================

    @Test
    @DisplayName("9. Web: User listing renders normal users")
    @WithMockUser(username = "admin@mygarage.com", roles = {"ADMIN"})
    void testWebUserListing() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users"))
                .andExpect(model().attributeExists("users"))
                .andExpect(model().attribute("users", hasItem(hasProperty("fullName", is("Alice Walker")))));
    }

    @Test
    @DisplayName("10. Web: User search filters by query")
    @WithMockUser(username = "admin@mygarage.com", roles = {"ADMIN"})
    void testWebUserSearch() throws Exception {
        mockMvc.perform(get("/admin/users").param("search", "Alice"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("users", hasSize(1)))
                .andExpect(model().attribute("users", hasItem(hasProperty("fullName", is("Alice Walker")))));
    }

    @Test
    @DisplayName("11. Web: User toggle active -> disabled")
    @WithMockUser(username = "admin@mygarage.com", roles = {"ADMIN"})
    void testWebUserToggleActiveToDisabled() throws Exception {
        assertTrue(normalUser1.isActive());

        mockMvc.perform(post("/admin/users/" + normalUser1.getUserId() + "/toggle")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attributeExists("successMsg"));

        User updated = userRepository.findById(normalUser1.getUserId()).orElseThrow();
        assertFalse(updated.isActive());
    }

    @Test
    @DisplayName("12. Web: User toggle disabled -> active")
    @WithMockUser(username = "admin@mygarage.com", roles = {"ADMIN"})
    void testWebUserToggleDisabledToActive() throws Exception {
        assertFalse(normalUser2.isActive());

        mockMvc.perform(post("/admin/users/" + normalUser2.getUserId() + "/toggle")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attributeExists("successMsg"));

        User updated = userRepository.findById(normalUser2.getUserId()).orElseThrow();
        assertTrue(updated.isActive());
    }

    @Test
    @DisplayName("13. Security: Primary Admin cannot be deactivated")
    @WithMockUser(username = "admin@mygarage.com", roles = {"ADMIN"})
    void testPrimaryAdminCannotBeDeactivated() throws Exception {
        mockMvc.perform(post("/admin/users/" + primaryAdmin.getUserId() + "/toggle")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attribute("errorMsg", containsString("Cannot deactivate or modify the primary administrator account.")));

        User adminAfter = userRepository.findById(primaryAdmin.getUserId()).orElseThrow();
        assertTrue(adminAfter.isActive());
    }

    // =========================================================================
    // SECTION 3: CATEGORY MANAGEMENT (WEB MVC)
    // =========================================================================

    @Test
    @DisplayName("14. Web: Category listing displays categories")
    @WithMockUser(username = "admin@mygarage.com", roles = {"ADMIN"})
    void testWebCategoryListing() throws Exception {
        mockMvc.perform(get("/admin/categories"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/categories"))
                .andExpect(model().attributeExists("categories"));
    }

    @Test
    @DisplayName("15. Web: Add category success")
    @WithMockUser(username = "admin@mygarage.com", roles = {"ADMIN"})
    void testWebAddCategorySuccess() throws Exception {
        mockMvc.perform(post("/admin/categories/add")
                .with(csrf())
                .param("name", "Electric Scooter")
                .param("icon", "🛵")
                .param("description", "Two-wheeled EV"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/categories"))
                .andExpect(flash().attributeExists("successMsg"));

        assertTrue(categoryRepository.existsByNameIgnoreCase("Electric Scooter"));
    }

    @Test
    @DisplayName("16. Web: Add duplicate category rejected with error message")
    @WithMockUser(username = "admin@mygarage.com", roles = {"ADMIN"})
    void testWebAddDuplicateCategoryRejected() throws Exception {
        mockMvc.perform(post("/admin/categories/add")
                .with(csrf())
                .param("name", "Sedan Admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/categories"))
                .andExpect(flash().attributeExists("errorMsg"));
    }

    @Test
    @DisplayName("17. Web: Edit category success")
    @WithMockUser(username = "admin@mygarage.com", roles = {"ADMIN"})
    void testWebEditCategorySuccess() throws Exception {
        mockMvc.perform(post("/admin/categories/" + categoryEmpty.getCategoryId() + "/edit")
                .with(csrf())
                .param("name", "Cruiser Motorcycle")
                .param("icon", "🏍️")
                .param("description", "Heavy highway cruiser"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/categories"))
                .andExpect(flash().attributeExists("successMsg"));

        VehicleCategory updated = categoryRepository.findById(categoryEmpty.getCategoryId()).orElseThrow();
        assertEquals("Cruiser Motorcycle", updated.getName());
    }

    @Test
    @DisplayName("18. Web: Delete category without vehicles success")
    @WithMockUser(username = "admin@mygarage.com", roles = {"ADMIN"})
    void testWebDeleteCategorySuccess() throws Exception {
        mockMvc.perform(post("/admin/categories/" + categoryEmpty.getCategoryId() + "/delete")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/categories"))
                .andExpect(flash().attributeExists("successMsg"));

        assertTrue(categoryRepository.findById(categoryEmpty.getCategoryId()).isEmpty());
    }

    @Test
    @DisplayName("19. Web: Delete category with vehicles assigned blocked")
    @WithMockUser(username = "admin@mygarage.com", roles = {"ADMIN"})
    void testWebDeleteCategoryWithVehiclesBlocked() throws Exception {
        mockMvc.perform(post("/admin/categories/" + categoryWithVehicle.getCategoryId() + "/delete")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/categories"))
                .andExpect(flash().attribute("errorMsg", containsString("Cannot delete category")));

        assertTrue(categoryRepository.findById(categoryWithVehicle.getCategoryId()).isPresent());
    }

    // =========================================================================
    // SECTION 4: CSRF PROTECTION
    // =========================================================================

    @Test
    @DisplayName("20. CSRF: Category add without CSRF rejected (403 Forbidden)")
    @WithMockUser(username = "admin@mygarage.com", roles = {"ADMIN"})
    void testCategoryAddWithoutCsrfForbidden() throws Exception {
        mockMvc.perform(post("/admin/categories/add")
                .param("name", "Hack Category"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("21. CSRF: User toggle without CSRF rejected (403 Forbidden)")
    @WithMockUser(username = "admin@mygarage.com", roles = {"ADMIN"})
    void testUserToggleWithoutCsrfForbidden() throws Exception {
        mockMvc.perform(post("/admin/users/" + normalUser1.getUserId() + "/toggle"))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // SECTION 5: REST API VERIFICATION
    // =========================================================================

    @Test
    @DisplayName("22. REST: GET /api/admin/statistics returns system metrics")
    @WithMockUser(username = "admin@mygarage.com", roles = {"ADMIN"})
    void testRestGetStatistics() throws Exception {
        mockMvc.perform(get("/api/admin/statistics"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalUsers").exists())
                .andExpect(jsonPath("$.totalVehicles").value(1));
    }

    @Test
    @DisplayName("23. REST: GET /api/admin/users returns normal users")
    @WithMockUser(username = "admin@mygarage.com", roles = {"ADMIN"})
    void testRestGetUsers() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))));
    }

    @Test
    @DisplayName("24. REST: GET /api/admin/users?search= filters users")
    @WithMockUser(username = "admin@mygarage.com", roles = {"ADMIN"})
    void testRestSearchUsers() throws Exception {
        mockMvc.perform(get("/api/admin/users").param("search", "Alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].fullName").value("Alice Walker"));
    }

    @Test
    @DisplayName("25. REST: POST /api/admin/users/{id}/toggle toggles active state")
    @WithMockUser(username = "admin@mygarage.com", roles = {"ADMIN"})
    void testRestToggleUser() throws Exception {
        mockMvc.perform(post("/api/admin/users/" + normalUser1.getUserId() + "/toggle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false));
    }

    @Test
    @DisplayName("26. REST: Primary Admin toggle rejected (400 Bad Request)")
    @WithMockUser(username = "admin@mygarage.com", roles = {"ADMIN"})
    void testRestPrimaryAdminToggleRejected() throws Exception {
        mockMvc.perform(post("/api/admin/users/" + primaryAdmin.getUserId() + "/toggle"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Cannot deactivate or modify the primary administrator account.")));
    }

    @Test
    @DisplayName("27. REST: Category CRUD (GET, POST, PUT, DELETE)")
    @WithMockUser(username = "admin@mygarage.com", roles = {"ADMIN"})
    void testRestCategoryCrud() throws Exception {
        // 1. POST
        String response = mockMvc.perform(post("/api/admin/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "name", "Sports Coupe",
                        "icon", "🏎️",
                        "description", "Performance coupe"
                ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Sports Coupe"))
                .andReturn().getResponse().getContentAsString();

        VehicleCategory created = objectMapper.readValue(response, VehicleCategory.class);

        // 2. GET by ID
        mockMvc.perform(get("/api/admin/categories/" + created.getCategoryId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Sports Coupe"));

        // 3. PUT
        mockMvc.perform(put("/api/admin/categories/" + created.getCategoryId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "name", "Grand Tourer",
                        "icon", "🏎️",
                        "description", "Luxury GT"
                ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Grand Tourer"));

        // 4. DELETE
        mockMvc.perform(delete("/api/admin/categories/" + created.getCategoryId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Category deleted successfully."));
    }

    @Test
    @DisplayName("28. REST: Delete Category with vehicles assigned rejected (400 Bad Request)")
    @WithMockUser(username = "admin@mygarage.com", roles = {"ADMIN"})
    void testRestDeleteCategoryWithVehiclesRejected() throws Exception {
        mockMvc.perform(delete("/api/admin/categories/" + categoryWithVehicle.getCategoryId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Cannot delete category")));
    }

    @Test
    @DisplayName("29. REST: NORMAL_USER blocked from all /api/admin/** (403 Forbidden)")
    @WithMockUser(username = "admin_test_u1@test.com", roles = {"NORMAL_USER"})
    void testUserBlockedFromAdminApi() throws Exception {
        mockMvc.perform(get("/api/admin/statistics"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/categories"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("30. Security: Role escalation prevention (Normal user cannot register as ADMIN)")
    void testRoleEscalationPrevention() throws Exception {
        // Attempt to register with any payload; service hardcodes Role.NORMAL_USER
        mockMvc.perform(post("/register")
                .with(csrf())
                .param("fullName", "Attacker")
                .param("email", "attacker@test.com")
                .param("password", "Pass@123")
                .param("confirmPassword", "Pass@123"))
                .andExpect(status().is3xxRedirection());

        User registered = userRepository.findByEmail("attacker@test.com").orElseThrow();
        assertEquals(Role.NORMAL_USER, registered.getRole());
    }
}