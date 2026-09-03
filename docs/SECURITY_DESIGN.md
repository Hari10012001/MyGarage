# SECURITY DESIGN & RBAC SPECIFICATION — MyGarage (APPJFS19)

**Project Name:** MyGarage — A Vehicle Service History, Fuel Record and Maintenance Tracking Platform  
**System ID:** APPJFS19  
**Security Framework:** Spring Security 6.x (Spring Boot 3.3.5)  
**Authentication Mechanism:** HTTP Session Form Login (No JWT)  
**Password Encryption:** BCrypt (`BCryptPasswordEncoder`, Strength: 10)  
**Last Verified:** Milestone 3 (M3) — 2026-09-03

---

## 1. Role-Based Access Control (RBAC) Matrix

| Path / Endpoint Pattern | HTTP Method | Permitted Roles | Unauthenticated Action | Unauthorized Action |
|---|---|---|---|---|
| `/` | `GET` | `permitAll()` | HTTP 200 OK | Allowed |
| `/login` | `GET`, `POST` | `permitAll()` | HTTP 200 OK | Allowed |
| `/register` | `GET`, `POST` | `permitAll()` | HTTP 200 OK | Allowed |
| `/access-denied` | `GET` | `permitAll()` | HTTP 200 OK | Allowed |
| `/css/**`, `/js/**`, `/images/**` | `GET` | `permitAll()` | HTTP 200 OK | Allowed |
| `/actuator/health` | `GET` | `permitAll()` | HTTP 200 OK | Allowed |
| `/dashboard/**` | `GET` | `ROLE_NORMAL_USER` | Redirect to `/login` | HTTP 403 / Access Denied |
| `/vehicles/**` | `GET`, `POST` | `ROLE_NORMAL_USER` | Redirect to `/login` | HTTP 403 / Access Denied |
| `/service/**` | `GET`, `POST` | `ROLE_NORMAL_USER` | Redirect to `/login` | HTTP 403 / Access Denied |
| `/fuel/**` | `GET`, `POST` | `ROLE_NORMAL_USER` | Redirect to `/login` | HTTP 403 / Access Denied |
| `/maintenance/**` | `GET`, `POST` | `ROLE_NORMAL_USER` | Redirect to `/login` | HTTP 403 / Access Denied |
| `/profile/**` | `GET`, `POST` | `ROLE_NORMAL_USER`, `ROLE_ADMIN` | Redirect to `/login` | HTTP 403 / Access Denied |
| `/api/**` (user data) | `GET`, `POST`, `PUT`, `DELETE` | `ROLE_NORMAL_USER` | HTTP 401/403 or redirect | HTTP 403 Forbidden |
| `/admin/**` | `GET`, `POST` | `ROLE_ADMIN` | Redirect to `/login` | HTTP 403 / Access Denied |
| `/api/admin/**` | `GET`, `POST`, `PUT`, `DELETE` | `ROLE_ADMIN` | HTTP 401/403 | HTTP 403 Forbidden |

---

## 2. Security Architecture Principles

### 1. Zero Plaintext Passwords
- All passwords are encrypted with `BCryptPasswordEncoder(10)`.
- Password hashes begin with the standard BCrypt identifier (`$2a$` or `$2b$`).
- Plaintext passwords are never logged, exposed in exceptions, or stored in MySQL.
- Default seeded administrator:
  - Email: `admin@mygarage.com`
  - Password: `Admin@123`
  - Hash: `$2a$10$gTZWG2345TLShuH70fsN3OL/e4/dliZSJggoEfl6i3KMK5YobVm9u`

### 2. Privilege Escalation Prevention
- Public registration (`POST /register`) unconditionally assigns `Role.NORMAL_USER`.
- Client requests cannot submit or modify user roles through registration or profile update DTOs.
- Administrative accounts cannot be registered publicly and must be provisioned through administrative seed or database migrations.
- Administrative accounts cannot be deactivated via the user toggle feature (`UserService.toggleUserStatus()` prevents disabling `ADMIN`).

### 3. Session Authentication & Post-Login Redirection
- Authentication is handled via Spring Security `formLogin()`.
- Success Handler dynamically routes users based on their granted authority:
  - `ROLE_ADMIN` $\rightarrow$ `/admin/dashboard`
  - `ROLE_NORMAL_USER` $\rightarrow$ `/dashboard`
- Login failures redirect to `/login?error=true` with sanitized error messages.
- Logout (`POST /logout`) explicitly invalidates the `HttpSession`, flushes the security context, clears cookies (`JSESSIONID`), and redirects to `/login?logout=true`.

### 4. Cross-Site Request Forgery (CSRF) Protection
- CSRF protection is enabled for all Spring MVC state-modifying requests (`POST`, `PUT`, `DELETE`).
- Thymeleaf forms automatically include the synchronizer token (`<input type="hidden" name="_csrf" ... />`).
- The stateless `/api/**` endpoints are selectively exempted from CSRF checks for development/testing convenience.

### 5. Access-Denied & Exception Handling
- Access violations (e.g. `NORMAL_USER` attempting to visit `/admin/**`) trigger `AccessDeniedException`.
- Configured `.exceptionHandling(ex -> ex.accessDeniedPage("/access-denied"))` routes the user to a dedicated 403 error page.
- Sensitive stack traces and internal class names are suppressed from user-facing error views.

---

## 3. Verification & Test Coverage Matrix

The test class `com.mygarage.security.AuthenticationAndAuthorizationTest` automates 13 security verifications:
1. `testSuccessfulRegistration`: Validates full registration flow, BCrypt hashing, and role assignment.
2. `testDuplicateEmailRegistration`: Validates duplicate email rejection.
3. `testInvalidRegistrationValidation`: Validates password length and format constraints.
4. `testSuccessfulNormalUserLogin`: Confirms authentication and redirection to `/dashboard`.
5. `testInvalidLoginWrongPassword`: Confirms redirection to `/login?error=true`.
6. `testAdminAuthentication`: Confirms authentication and redirection to `/admin/dashboard`.
7. `testNormalUserCannotAccessAdmin`: Confirms HTTP 403 Forbidden when normal user requests `/admin/dashboard`.
8. `testAdminCanAccessAdminDashboard`: Confirms HTTP 200 OK for admin requesting `/admin/dashboard`.
9. `testUnauthenticatedAccessRedirectsToLogin`: Confirms unauthenticated requests to protected URLs are redirected to `/login`.
10. `testLogoutAndSessionInvalidation`: Confirms POST `/logout` invalidates session and redirects to `/login?logout=true`.
11. `testRoleEnforcementOnRest`: Confirms role gating on REST endpoints (`/api/vehicles` vs `/api/admin/statistics`).
12. `testRegistrationCannotCreateAdmin`: Asserts that `UserService.register()` strictly enforces `NORMAL_USER`.
13. `testPasswordIsBcryptEncoded`: Verifies database hash format and BCrypt signature.