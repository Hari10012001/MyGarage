# PROJECT_STATUS.md — MyGarage (APPJFS19)

**Project:** MyGarage – A Vehicle Service History, Fuel Record and Maintenance Tracking Platform  
**Project ID:** APPJFS19  
**Last Updated:** 2026-09-03  
**Current Phase:** MILESTONE 3 (M3) FINAL ACCEPTANCE & VERIFICATION COMPLETE — Ready for M4

---

## Environment Snapshot

| Item | Value | Status |
|------|-------|--------|
| Operating System | Windows 10 x64 | Active |
| Java (Compiler / Maven) | JDK 21.0.7 (Oracle) | Verified (`release 21`) |
| Apache Maven | 3.9.10 | Verified |
| MySQL Server | 8.0.43 Community Server | Active & Connected |
| Database Name | `mygarage_db` | Created & Initialized |
| Version Control | Git 2.45+ | Active |
| Framework | Spring Boot 3.3.5 / Spring Security 6.x | Verified |
| Application Port | 8080 | Tested & Clean |

---

## Milestone 3 (M3) Final Security & REST Authorization Matrix

| Check / Requirement | Specification | Status |
|---|---|---|
| **SecurityFilterChain Ordering** | `/api/admin/**` evaluated strictly BEFORE `/api/**` | **PASS** |
| **NORMAL_USER Registration** | Validated, unique email enforced, BCrypt hashed, hardcoded to `NORMAL_USER` | **PASS** |
| **NORMAL_USER Login** | Form login authenticated, session created, redirects to `/dashboard` | **PASS** |
| **ADMIN Login** | Authenticates seeded `admin@mygarage.com / Admin@123`, redirects to `/admin/dashboard` | **PASS** |
| **Unauth -> /api/vehicles** | Blocked / Redirected to `/login` | **PASS** |
| **NORMAL_USER -> /api/vehicles** | Allowed (`HTTP 200 OK`) | **PASS** |
| **ADMIN -> /api/vehicles** | Blocked (`HTTP 403 Forbidden` - User vehicle isolation preserved) | **PASS** |
| **NORMAL_USER -> /api/admin/statistics** | Blocked (`HTTP 403 Forbidden`) | **PASS** |
| **ADMIN -> /api/admin/statistics** | Allowed (`HTTP 200 OK`) | **PASS** |
| **NORMAL_USER -> /api/admin/** | Blocked (`HTTP 403 Forbidden` on users, categories, toggle) | **PASS** |
| **ADMIN -> /api/admin/** | Allowed (`HTTP 200 OK` on users, categories, toggle) | **PASS** |
| **State-Changing REST Control** | POST/PUT/DELETE API endpoints enforce RBAC without privilege bypass | **PASS** |
| **CSRF Web Protection** | CSRF enabled on MVC forms (missing token returns 403/405; valid token succeeds) | **PASS** |
| **Logout & Session Termination** | POST `/logout` invalidates session, clears cookies, redirects to `/login?logout=true` | **PASS** |
| **Regression Test Suite** | 37 automated tests (`AuthenticationAndAuthorizationTest` [23], `JpaRepositoryTest` [7], `MaintenanceServiceTest` [5], `PasswordEncoderTest` [1], `MyGarageApplicationTests` [1]) | **PASS (37/37, 0 failures, 0 errors)** |
| **Live End-to-End Verification** | Live HTTP requests against running Spring Boot and MySQL verified all 7 combinations | **PASS** |

---

## Milestone Progress

| Milestone | Description | Status |
|-----------|-------------|--------|
| **Phase 0** | Repository Inspection | **DONE** |
| **Phase 1** | Requirements Engineering (`docs/REQUIREMENTS.md`) | **DONE** |
| **Phase 2** | UI/UX Specification (20 Pages) | **DONE** |
| **Approval Gate 1** | Plan & Scope Approval | **APPROVED** |
| **M1** | Project Setup, Maven, Git, DB Config, Base Architecture & Smoke QC | **COMPLETED & VERIFIED** |
| **M2** | Database & JPA Entities (Deep Verification, Indexes, Cascades & Mappings) | **COMPLETED & VERIFIED** |
| **M3** | Authentication & Role-Based Access Control (SecurityFilterChain Matcher Order, REST RBAC, CSRF, BCrypt) | **COMPLETED & FULLY VERIFIED** |
| **M4** | Vehicle Module (Vehicle CRUD, Ownership Guarding, License Plate Uniqueness, Category Association, Odometer Tracking) | **NEXT** |
| **M5** | Service Module (Service History & Tracking) | PENDING |
| **M6** | Fuel Module (Fuel Logs & Estimated Mileage) | PENDING |
| **M7** | Maintenance Module (Status Logic & Reminders) | PENDING |
| **M8** | Dashboard & Reports (Real Analytics & Alert Aggregation) | PENDING |
| **M9** | Admin Module (User & Category Management) | PENDING |
| **M10** | Polish, Comprehensive 6-Layer QC & Documentation Finalization | PENDING |

---

## Architecture Inventory

- **Web MVC Controllers (8):**
  1. `AuthWebController` (Landing `/`, Login `/login`, Register `/register`, Access Denied `/access-denied`)
  2. `DashboardWebController` (`/dashboard/**`)
  3. `VehicleWebController` (`/vehicles/**`)
  4. `ServiceWebController` (`/service/**`)
  5. `FuelWebController` (`/fuel/**`)
  6. `MaintenanceWebController` (`/maintenance/**`)
  7. `ProfileWebController` (`/profile/**`)
  8. `AdminWebController` (`/admin/**`)
- **REST API Controllers (6):**
  1. `AdminApiController` (`/api/admin/**` - Gated strictly to `ROLE_ADMIN`)
  2. `VehicleApiController` (`/api/vehicles/**`)
  3. `ServiceApiController` (`/api/services/**`)
  4. `FuelApiController` (`/api/fuel/**`)
  5. `MaintenanceApiController` (`/api/maintenance/**`)
  6. `DashboardApiController` (`/api/dashboard/**`)
- **Documentation:**
  - `docs/REQUIREMENTS.md` (Full Functional Specification)
  - `docs/DATABASE_DESIGN.md` (Full ERD, Data Dictionary, and Integrity Constraints)
  - `docs/SECURITY_DESIGN.md` (SecurityFilterChain Ordering, RBAC Matrix, CSRF, Jackson Annotations)
  - `PROJECT_STATUS.md` (Milestone Tracker)

---

## Next Action

Awaiting instruction to proceed to **M4: Vehicle Module (Vehicle CRUD, Ownership Guarding, License Plate Uniqueness per Garage, Category Association, and Odometer Tracking)**.