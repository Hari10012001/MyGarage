# SECURITY DESIGN & RBAC SPECIFICATION — MyGarage (APPJFS19)

**Project Name:** MyGarage — A Vehicle Service History, Fuel Record and Maintenance Tracking Platform  
**System ID:** APPJFS19  
**Security Framework:** Spring Security 6.x (Spring Boot 3.3.5)  
**Authentication Mechanism:** Form-based HTTP Session (No JWT)  
**Password Encryption:** BCrypt (`BCryptPasswordEncoder`, Strength: 10)  
**Last Verified:** Milestone 3 (M3) Final Review — 2026-09-03

---

## 1. SecurityFilterChain Matcher Ordering Architecture

The conceptual and actual evaluation order in `SecurityConfig.java` strictly enforces that more specific administrative paths are evaluated **before** broad wildcards:

```java
// 1. Public Pages (Permit All)
.requestMatchers("/", "/login", "/register", "/access-denied").permitAll()

// 2. Static Resources & Actuator Health (Permit All)
.requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
.requestMatchers("/actuator/health").permitAll()

// 3. Specific Admin Web & REST Routes (Evaluated FIRST before broad /api/**)
.requestMatchers("/admin/**").hasRole("ADMIN")
.requestMatchers("/api/admin/**").hasRole("ADMIN")

// 4. Normal User Web Routes (Garage, Service, Fuel, Maintenance)
.requestMatchers("/dashboard/**", "/vehicles/**", "/service/**", "/fuel/**", "/maintenance/**").hasRole("NORMAL_USER")

// 5. Normal User REST APIs (Evaluated AFTER specific /api/admin/** rule)
.requestMatchers("/api/**").hasRole("NORMAL_USER")

// 6. Shared Profile Routes (Accessible to both NORMAL_USER and ADMIN)
.requestMatchers("/profile/**").hasAnyRole("NORMAL_USER", "ADMIN")

// 7. Fallback Rule (Any other request requires authentication)
.anyRequest().authenticated()
```

> [!IMPORTANT]
> **Matcher Ordering Guarantee:** Because `.requestMatchers("/api/admin/**").hasRole("ADMIN")` is placed **above** `.requestMatchers("/api/**").hasRole("NORMAL_USER")`, Spring Security always requires `ROLE_ADMIN` for `/api/admin/**`. The broad `/api/**` rule never intercepts administrative endpoints.

---

## 2. API Authorization & Access Matrix

| Scenario | Request | Required Role | Actual Outcome | Verification Status |
|---|---|---|---|---|
| **1. Unauthenticated Vehicle API** | `GET /api/vehicles` | `NORMAL_USER` | Redirect to `/login` / Blocked | **PASS** |
| **2. NORMAL_USER Vehicle API** | `GET /api/vehicles` | `NORMAL_USER` | `HTTP 200 OK` | **PASS** |
| **3. ADMIN Vehicle API** | `GET /api/vehicles` | `NORMAL_USER` | `HTTP 403 Forbidden` (Defined Isolation) | **PASS** |
| **4. NORMAL_USER Admin Stats** | `GET /api/admin/statistics`| `ADMIN` | `HTTP 403 Forbidden` | **PASS** |
| **5. ADMIN Admin Stats** | `GET /api/admin/statistics`| `ADMIN` | `HTTP 200 OK` | **PASS** |
| **6. NORMAL_USER Admin Endpoints** | `GET /api/admin/users`, `/categories` | `ADMIN` | `HTTP 403 Forbidden` | **PASS** |
| **7. ADMIN Admin Endpoints** | `GET /api/admin/users`, `/categories` | `ADMIN` | `HTTP 200 OK` | **PASS** |
| **8. Unauthenticated POST API** | `POST /api/vehicles` | `NORMAL_USER` | Redirect to `/login` / Blocked | **PASS** |
| **9. ADMIN State-Changing User API** | `POST /api/vehicles` | `NORMAL_USER` | `HTTP 403 Forbidden` | **PASS** |
| **10. NORMAL_USER Admin Post API** | `POST /api/admin/users/1/toggle` | `ADMIN` | `HTTP 403 Forbidden` | **PASS** |
| **11. ADMIN Admin Post API** | `POST /api/admin/users/{id}/toggle`| `ADMIN` | `HTTP 200 OK` | **PASS** |

---

## 3. CSRF Protection Architecture

- **Web MVC Forms:** CSRF is strictly **enabled** across all state-altering web operations (`POST /login`, `POST /register`, `POST /logout`, `/vehicles/**`, `/service/**`, etc.).
  - Attempting `POST /logout` without a valid CSRF token is rejected with HTTP 403 / 405.
  - Submitting `POST /logout` with the valid Thymeleaf `_csrf` token succeeds and redirects to `/login?logout=true`.
- **REST APIs:** The stateless testing path `.ignoringRequestMatchers("/api/**")` allows standard JSON API clients (Postman, REST automation) without global CSRF disablement.
- **Global Disablement Prohibited:** `http.csrf().disable()` is **never** used globally.

---

## 4. Security Defect Mitigations Completed

1. **Jackson Infinite Recursion & Sensitive Data Exposure:**
   - Added `@JsonIgnore` to `User.passwordHash` to ensure password hashes are never exposed in JSON responses.
   - Added `@JsonIgnore` to `User.vehicles` and `VehicleCategory.vehicles` to prevent circular serialization and `LazyInitializationException` outside active sessions.
2. **Deterministic Test Isolation:**
   - Ensured `AuthenticationAndAuthorizationTest` re-activates `john@example.com` in `setUp()` and uses isolated accounts for state-toggle verifications.
3. **Privilege Escalation Prevention:**
   - `UserService.register()` hardcodes `Role.NORMAL_USER`. No client request can grant or request `Role.ADMIN`.
   - `toggleUserStatus()` prevents disabling `Role.ADMIN` accounts.