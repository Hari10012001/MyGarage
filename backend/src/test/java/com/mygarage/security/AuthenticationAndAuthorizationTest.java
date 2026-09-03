package com.mygarage.security;

import com.mygarage.dto.request.RegisterRequest;
import com.mygarage.model.User;
import com.mygarage.model.enums.Role;
import com.mygarage.repository.UserRepository;
import com.mygarage.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Milestone 3 (M3) — Comprehensive Authentication & Authorization Security Test Suite.
 * Validates registration, login, admin authentication, BCrypt hashing, role-based protection,
 * unauthorized access redirects, access-denied handling, and session invalidation.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthenticationAndAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        // Ensure standard test user exists and is active
        User user = userRepository.findByEmail("john@example.com").orElseGet(User::new);
        user.setFullName("John Doe");
        user.setEmail("john@example.com");
        user.setPasswordHash(passwordEncoder.encode("Secret@123"));
        user.setPhone("9876543210");
        user.setRole(Role.NORMAL_USER);
        user.setActive(true);
        userRepository.save(user);

        // Ensure default admin exists
        if (!userRepository.existsByEmail("admin@mygarage.com")) {
            User admin = new User();
            admin.setFullName("MyGarage Admin");
            admin.setEmail("admin@mygarage.com");
            admin.setPasswordHash(passwordEncoder.encode("Admin@123"));
            admin.setPhone("9999999999");
            admin.setRole(Role.ADMIN);
            admin.setActive(true);
            userRepository.save(admin);
        }
    }

    @Test
    @DisplayName("1. Successful NORMAL_USER Registration")
    void testSuccessfulRegistration() throws Exception {
        String email = "newuser_" + System.currentTimeMillis() + "@example.com";
        mockMvc.perform(post("/register")
                .with(csrf())
                .param("fullName", "Jane Doe")
                .param("email", email)
                .param("password", "Pass@123")
                .param("confirmPassword", "Pass@123")
                .param("phone", "9123456780"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attributeExists("successMsg"));

        Optional<User> created = userRepository.findByEmail(email);
        assertTrue(created.isPresent());
        assertEquals("Jane Doe", created.get().getFullName());
        assertEquals(Role.NORMAL_USER, created.get().getRole());
        assertTrue(passwordEncoder.matches("Pass@123", created.get().getPasswordHash()));
        assertFalse(created.get().getPasswordHash().contains("Pass@123"));
    }

    @Test
    @DisplayName("2. Duplicate Email Registration Prevention")
    void testDuplicateEmailRegistration() throws Exception {
        mockMvc.perform(post("/register")
                .with(csrf())
                .param("fullName", "John Copy")
                .param("email", "john@example.com")
                .param("password", "Secret@123")
                .param("confirmPassword", "Secret@123")
                .param("phone", "9876543210"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeExists("errorMsg"));
    }

    @Test
    @DisplayName("3. Invalid Registration: Validation Errors (Short Password & Mismatched Confirm)")
    void testInvalidRegistrationValidation() throws Exception {
        mockMvc.perform(post("/register")
                .with(csrf())
                .param("fullName", "")
                .param("email", "not-an-email")
                .param("password", "123")
                .param("confirmPassword", "456")
                .param("phone", "123"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().hasErrors());
    }

    @Test
    @DisplayName("4. Successful NORMAL_USER Login & Redirect to /dashboard")
    void testSuccessfulNormalUserLogin() throws Exception {
        mockMvc.perform(post("/login")
                .with(csrf())
                .param("email", "john@example.com")
                .param("password", "Secret@123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    @DisplayName("5. Invalid Login: Wrong Password Redirects to /login?error=true")
    void testInvalidLoginWrongPassword() throws Exception {
        mockMvc.perform(post("/login")
                .with(csrf())
                .param("email", "john@example.com")
                .param("password", "WrongPassword!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=true"));
    }

    @Test
    @DisplayName("6. ADMIN Authentication & Redirect to /admin/dashboard")
    void testAdminAuthentication() throws Exception {
        mockMvc.perform(post("/login")
                .with(csrf())
                .param("email", "admin@mygarage.com")
                .param("password", "Admin@123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"));
    }

    @Test
    @DisplayName("7. NORMAL_USER Cannot Access /admin/** (Access Denied 403 / Redirect)")
    @WithMockUser(username = "john@example.com", roles = {"NORMAL_USER"})
    void testNormalUserCannotAccessAdmin() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("8. ADMIN Can Access /admin/**")
    @WithMockUser(username = "admin@mygarage.com", roles = {"ADMIN"})
    void testAdminCanAccessAdminDashboard() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("9. Unauthenticated Access Protection (Redirects to /login)")
    void testUnauthenticatedAccessRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        mockMvc.perform(get("/vehicles"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("10. Logout and Session Invalidation")
    void testLogoutAndSessionInvalidation() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/login")
                .with(csrf())
                .param("email", "john@example.com")
                .param("password", "Secret@123"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession();
        assertNotNull(session);
        assertFalse(session.isInvalid());

        mockMvc.perform(post("/logout")
                .session(session)
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout=true"));

        assertTrue(session.isInvalid(), "HTTP session must be invalidated after logout");
    }

    @Test
    @DisplayName("11a. Unauthenticated Access to /api/vehicles is Blocked")
    void testUnauthenticatedCannotAccessApiVehicles() throws Exception {
        mockMvc.perform(get("/api/vehicles"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("11b. NORMAL_USER Allowed on /api/vehicles")
    @WithMockUser(username = "john@example.com", roles = {"NORMAL_USER"})
    void testNormalUserCanAccessApiVehicles() throws Exception {
        mockMvc.perform(get("/api/vehicles"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("11c. ADMIN Forbidden from /api/vehicles (Defined Isolation)")
    @WithMockUser(username = "admin@mygarage.com", roles = {"ADMIN"})
    void testAdminCannotAccessApiVehicles() throws Exception {
        mockMvc.perform(get("/api/vehicles"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("11d. NORMAL_USER Forbidden from /api/admin/statistics")
    @WithMockUser(username = "john@example.com", roles = {"NORMAL_USER"})
    void testNormalUserForbiddenFromApiAdminStatistics() throws Exception {
        mockMvc.perform(get("/api/admin/statistics"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("11e. ADMIN Allowed on /api/admin/statistics")
    @WithMockUser(username = "admin@mygarage.com", roles = {"ADMIN"})
    void testAdminCanAccessApiAdminStatistics() throws Exception {
        mockMvc.perform(get("/api/admin/statistics"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("11f. NORMAL_USER Forbidden from /api/admin/** (categories & users)")
    @WithMockUser(username = "john@example.com", roles = {"NORMAL_USER"})
    void testNormalUserForbiddenFromApiAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/categories"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("11g. ADMIN Allowed on /api/admin/** (categories & users)")
    @WithMockUser(username = "admin@mygarage.com", roles = {"ADMIN"})
    void testAdminCanAccessApiAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/categories"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("11h. State-Changing REST: Unauthenticated POST /api/vehicles Blocked")
    void testStateChangingUnauthenticatedBlocked() throws Exception {
        mockMvc.perform(post("/api/vehicles")
                .contentType("application/json")
                .content("{\"make\":\"Toyota\",\"model\":\"Corolla\"}"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("11i. State-Changing REST: ADMIN Forbidden from POST /api/vehicles")
    @WithMockUser(username = "admin@mygarage.com", roles = {"ADMIN"})
    void testStateChangingAdminForbiddenFromVehiclesPost() throws Exception {
        mockMvc.perform(post("/api/vehicles")
                .contentType("application/json")
                .content("{\"make\":\"Toyota\",\"model\":\"Corolla\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("11j. State-Changing REST: NORMAL_USER Forbidden from POST /api/admin/**")
    @WithMockUser(username = "john@example.com", roles = {"NORMAL_USER"})
    void testStateChangingNormalUserForbiddenFromAdminPost() throws Exception {
        mockMvc.perform(post("/api/admin/users/1/toggle"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("11k. State-Changing REST: ADMIN Allowed on POST /api/admin/**")
    @WithMockUser(username = "admin@mygarage.com", roles = {"ADMIN"})
    void testStateChangingAdminAllowedToggle() throws Exception {
        User toggleUser = new User();
        toggleUser.setFullName("Toggle Test");
        toggleUser.setEmail("toggle_target@example.com");
        toggleUser.setPasswordHash(passwordEncoder.encode("Pass@123"));
        toggleUser.setRole(Role.NORMAL_USER);
        toggleUser.setActive(true);
        toggleUser = userRepository.save(toggleUser);

        mockMvc.perform(post("/api/admin/users/" + toggleUser.getUserId() + "/toggle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User status updated successfully."));
    }

    @Test
    @DisplayName("12. Registration Service Always Enforces NORMAL_USER Role")
    void testRegistrationCannotCreateAdmin() {
        RegisterRequest req = new RegisterRequest();
        req.setFullName("Hacker Attempt");
        req.setEmail("hacker_" + System.currentTimeMillis() + "@example.com");
        req.setPassword("Password@123");
        req.setConfirmPassword("Password@123");
        req.setPhone("9999999990");

        User registered = userService.register(req);
        assertEquals(Role.NORMAL_USER, registered.getRole(), "Registration MUST always create NORMAL_USER");
        assertNotEquals(Role.ADMIN, registered.getRole(), "Registration must never assign ADMIN role");
    }

    @Test
    @DisplayName("13. Password Is Confirmed BCrypt Encoded in Database")
    void testPasswordIsBcryptEncoded() {
        Optional<User> adminUser = userRepository.findByEmail("admin@mygarage.com");
        assertTrue(adminUser.isPresent());
        String hash = adminUser.get().getPasswordHash();

        assertNotNull(hash);
        assertTrue(hash.startsWith("$2a$") || hash.startsWith("$2b$"), "Password hash must start with BCrypt signature ($2a$)");
        assertNotEquals("Admin@123", hash, "Password must never be stored in plaintext");
        assertTrue(passwordEncoder.matches("Admin@123", hash), "BCryptPasswordEncoder must match original password against hash");
    }
}