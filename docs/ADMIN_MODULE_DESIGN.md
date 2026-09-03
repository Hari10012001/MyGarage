# ADMIN & SYSTEM MANAGEMENT MODULE DESIGN & SPECIFICATION — MyGarage (APPJFS19)

**Project Name:** MyGarage — A Vehicle Service History, Fuel Record and Maintenance Tracking Platform  
**System ID:** APPJFS19  
**Module:** Milestone 9 (M9) — Admin Module & System Management  
**Author:** Senior Full Stack Architect & Engineering Team  
**Last Updated:** 2026-09-03  
**Status:** **100% IMPLEMENTED, TESTED & FULLY VERIFIED**

---

## 1. Module Overview & Architectural Principles

The Admin Module provides platform administrators with tools to monitor system health, manage user accounts, oversee vehicle categories, and view global usage analytics. At the same time, it strictly enforces privacy and privilege boundaries so that administrators cannot tamper with individual user garages, and regular users cannot escalate privileges or access system management tools.

### Core Architectural Principles:
1. **Strict Admin Matcher Ordering:**
   - Spring Security evaluates `.requestMatchers("/api/admin/**").hasRole("ADMIN")` BEFORE `.requestMatchers("/api/**").hasRole("NORMAL_USER")`.
   - Web routes under `/admin/**` are strictly gated to `ROLE_ADMIN`.
   - Normal users receive `403 Forbidden` for any administrative route or API.
2. **Primary Administrator Inviolability:**
   - The primary system administrator account (`admin@mygarage.com`, `Role.ADMIN`) is safeguarded against accidental or intentional deactivation or deletion.
   - Any attempt to toggle or deactivate the admin account triggers an `IllegalArgumentException("Cannot deactivate or modify the primary administrator account.")`, returning HTTP 400 Bad Request in REST and a descriptive error flash attribute in Web MVC.
3. **Vehicle Category Referential Protection:**
   - Administrators manage standard vehicle categories (e.g. Sedan, SUV, Motorcycle, Hatchback, Truck, Electric Scooter).
   - Before deleting any category, the system checks whether vehicles are currently assigned to it (`countByCategoryCategoryId(id) > 0`). If so, deletion is blocked with a clear warning (*"Cannot delete category: vehicles are currently assigned to it."*).
4. **Platform-Level Statistics (Zero Personal Garage Spying):**
   - Administrators only view aggregated platform-wide counts:
     - `totalUsers`, `totalVehicles`, `totalServiceRecords`, `totalFuelRecords`, `totalMaintenanceRecords`.
   - Administrators do not access individual user garage dashboards or records through administrative endpoints.
5. **Role Escalation Prevention:**
   - User registration hardcodes `Role.NORMAL_USER`.
   - Profile update endpoints do not allow changing roles.
   - The system administrator account is pre-seeded with BCrypt hashing and cannot self-register.

---

## 2. Web MVC Architecture & Routes

| HTTP Method | Route | Controller Method | Access Role | Description |
|---|---|---|---|---|
| `GET` | `/admin/dashboard` | `adminDashboard` | `ADMIN` | Global overview: metric counters, recent user registrations, category list. |
| `GET` | `/admin/statistics` | `statistics` | `ADMIN` | System analytics page with category vehicle breakdown. |
| `GET` | `/admin/users` | `manageUsers` | `ADMIN` | User listing table with search/filtering by name or email. |
| `POST` | `/admin/users/{id}/toggle` | `toggleUser` | `ADMIN` | Toggles user status (`active` ↔ `disabled`). Gated by primary admin check & CSRF. |
| `GET` | `/admin/categories` | `manageCategories` | `ADMIN` | Category management page listing categories and vehicle counts. |
| `POST` | `/admin/categories/add` | `addCategory` | `ADMIN` | Adds new vehicle category (name, emoji icon, description). Gated by CSRF. |
| `POST` | `/admin/categories/{id}/edit`| `editCategory` | `ADMIN` | Edits category details. Gated by CSRF. |
| `POST` | `/admin/categories/{id}/delete`| `deleteCategory` | `ADMIN` | Deletes category if no vehicles are assigned. Gated by CSRF. |

---

## 3. REST API Specification

| Method | Endpoint | Access Role | Status | Description |
|---|---|---|---|---|
| `GET` | `/api/admin/statistics` | `ADMIN` | `200 OK` / `403` | Returns JSON of system-wide counts. |
| `GET` | `/api/admin/users` | `ADMIN` | `200 OK` / `403` | Returns list of normal users (supports `?search=` filter). |
| `POST` | `/api/admin/users/{id}/toggle` | `ADMIN` | `200 OK` / `400` / `403` | Toggles user active status. Protected against admin deactivation. |
| `GET` | `/api/admin/categories` | `ADMIN` | `200 OK` / `403` | Returns all vehicle categories with vehicle associations. |
| `GET` | `/api/admin/categories/{id}` | `ADMIN` | `200 OK` / `404` / `403` | Returns category details by ID. |
| `POST` | `/api/admin/categories` | `ADMIN` | `201 Created` / `400` / `403` | Creates a new vehicle category. |
| `PUT` | `/api/admin/categories/{id}` | `ADMIN` | `200 OK` / `400` / `403` | Updates an existing vehicle category. |
| `DELETE` | `/api/admin/categories/{id}` | `ADMIN` | `200 OK` / `400` / `403` | Deletes category. Blocked if vehicles are assigned. |

---

## 4. Security & Access Matrix

| Operation / Path | Unauthenticated | NORMAL_USER | ADMIN |
|---|---|---|---|
| `GET /admin/dashboard` | Redirect `/login` | `403 Forbidden` | `200 OK` |
| `GET /admin/statistics` | Redirect `/login` | `403 Forbidden` | `200 OK` |
| `GET /admin/users` | Redirect `/login` | `403 Forbidden` | `200 OK` |
| `POST /admin/users/{id}/toggle` | Redirect `/login` | `403 Forbidden` | `302 Redirect` (CSRF verified) |
| `GET /admin/categories` | Redirect `/login` | `403 Forbidden` | `200 OK` |
| `POST /admin/categories/add` | Redirect `/login` | `403 Forbidden` | `302 Redirect` (CSRF verified) |
| `GET /api/admin/statistics` | Redirect `/login` | `403 Forbidden` | `200 OK` |
| `GET /api/admin/users` | Redirect `/login` | `403 Forbidden` | `200 OK` |
| `POST /api/admin/users/1/toggle`| Redirect `/login` | `403 Forbidden` | `400 Bad Request` (Protected) |
| `DELETE /api/admin/categories/{busyId}`| Redirect `/login` | `403 Forbidden` | `400 Bad Request` (Protected) |

---

## 5. Automated Test Coverage & Verification

Automated test suite [`AdminModuleTest.java`](file:///d:/HARIHARAN%20P/000_JAVA%20FULL%20STACK%20-%20Final%20Year%20Main_Projects%20-%202026_2027/019_MyGarage%20-%20A%20Vehicle%20Service%20History,%20Fuel%20Record%20and%20Maintenance%20Tracking%20Platform/backend/src/test/java/com/mygarage/admin/AdminModuleTest.java) executes **30 test cases**:

1. Security: ADMIN accesses `/admin/dashboard` (`200 OK`) — `PASS`
2. Security: ADMIN accesses `/admin/statistics` (`200 OK`) — `PASS`
3. Security: NORMAL_USER blocked from `/admin/dashboard` (`403 Forbidden`) — `PASS`
4. Security: NORMAL_USER blocked from `/admin/users` (`403 Forbidden`) — `PASS`
5. Security: NORMAL_USER blocked from `/admin/categories` (`403 Forbidden`) — `PASS`
6. Security: NORMAL_USER blocked from `/admin/statistics` (`403 Forbidden`) — `PASS`
7. Security: Unauthenticated to `/admin/dashboard` redirects to `/login` — `PASS`
8. Security: Unauthenticated to `/admin/users` redirects to `/login` — `PASS`
9. Web: User listing renders normal users — `PASS`
10. Web: User search filters by query string — `PASS`
11. Web: User toggle active → disabled — `PASS`
12. Web: User toggle disabled → active — `PASS`
13. Security: Primary Admin account cannot be deactivated (`errorMsg` flash) — `PASS`
14. Web: Category listing displays categories & counts — `PASS`
15. Web: Add category success — `PASS`
16. Web: Add duplicate category rejected with error message — `PASS`
17. Web: Edit category success — `PASS`
18. Web: Delete category without vehicles success — `PASS`
19. Web: Delete category with vehicles assigned blocked (`errorMsg` flash) — `PASS`
20. CSRF: Category add without CSRF rejected (`403 Forbidden`) — `PASS`
21. CSRF: User toggle without CSRF rejected (`403 Forbidden`) — `PASS`
22. REST: `GET /api/admin/statistics` returns system metrics — `PASS`
23. REST: `GET /api/admin/users` returns normal users — `PASS`
24. REST: `GET /api/admin/users?search=` filters users — `PASS`
25. REST: `POST /api/admin/users/{id}/toggle` toggles active state — `PASS`
26. REST: Primary Admin toggle rejected (`400 Bad Request`) — `PASS`
27. REST: Category CRUD (`GET`, `POST`, `PUT`, `DELETE`) — `PASS`
28. REST: Delete Category with vehicles assigned rejected (`400 Bad Request`) — `PASS`
29. REST: NORMAL_USER blocked from all `/api/admin/**` (`403 Forbidden`) — `PASS`
30. Security: Role escalation prevention (Normal user cannot register as ADMIN) — `PASS`

**Complete Regression Suite:** `mvn clean test "-Dspring.profiles.active=test"`:
- **Total Tests Executed:** **203**
- **Failures:** **0**
- **Errors:** **0**
- **Build Status:** **`BUILD SUCCESS`**