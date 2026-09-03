# PROJECT_STATUS.md — MyGarage (APPJFS19)

**Project:** MyGarage – A Vehicle Service History, Fuel Record and Maintenance Tracking Platform  
**Project ID:** APPJFS19  
**Last Updated:** 2026-09-03  
**Current Phase:** MILESTONE 3 (M3) COMPLETE & FULLY VERIFIED — Ready for M4

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

## Milestone 3 (M3) Authentication & Access Control Verification Matrix

| Check / Requirement | Specification | Status |
|---|---|---|
| **NORMAL_USER Registration** | POST `/register` with validation, duplicate email check, BCrypt hashing, and redirect to `/login` | **PASS** |
| **NORMAL_USER Login** | POST `/login` authenticates user, establishes session, redirects to `/dashboard` | **PASS** |
| **ADMIN Login** | Authenticates seeded `admin@mygarage.com / Admin@123`, redirects to `/admin/dashboard` | **PASS** |
| **BCrypt Password Hashing** | All passwords hashed with BCrypt (strength 10), zero plaintext in MySQL | **PASS** |
| **Role-Based Authorization** | `/admin/**` restricted to `ROLE_ADMIN`; `/dashboard/**`, `/vehicles/**`, `/service/**`, etc. restricted to `ROLE_NORMAL_USER` | **PASS** |
| **Admin Route Protection** | `NORMAL_USER` accessing `/admin/**` is blocked with HTTP 403 / forwarded to `/access-denied` | **PASS** |
| **User Route Protection** | Unauthenticated users accessing `/dashboard`, `/vehicles`, etc. redirected to `/login` | **PASS** |
| **Invalid Login Handling** | Incorrect credentials redirect to `/login?error=true` | **PASS** |
| **Logout & Session Invalidation** | POST `/logout` invalidates HTTP session, flushes context, redirects to `/login?logout=true` | **PASS** |
| **REST Role Gating** | `/api/admin/**` enforces `hasRole('ADMIN')`; `/api/**` enforces `hasRole('NORMAL_USER')` | **PASS** |
| **Privilege Escalation Prevention** | Registration strictly assigns `Role.NORMAL_USER`, client cannot request `ADMIN` | **PASS** |
| **Access Denied Page** | Dedicated `/access-denied` view with HTTP 403 handling | **PASS** |
| **Automated Tests** | 27 automated tests passing (`AuthenticationAndAuthorizationTest`, `JpaRepositoryTest`, `MaintenanceServiceTest`, `PasswordEncoderTest`, `MyGarageApplicationTests`) | **PASS (27/27, 0 failures, 0 errors)** |
| **Live End-to-End Verification** | Real HTTP requests against MySQL verified all 5 core authentication and authorization flows | **PASS** |

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
| **M3** | Authentication & Role-Based Access Control (Spring Security, Form Login, RBAC, BCrypt) | **COMPLETED & VERIFIED** |
| **M4** | Vehicle Module (CRUD, Ownership Protection, Odometer Tracking, License Plate Uniqueness) | **NEXT** |
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
  2. `DashboardWebController` (`/dashboard/**` with user-specific vehicle injection)
  3. `VehicleWebController` (`/vehicles/**`)
  4. `ServiceWebController` (`/service/**`)
  5. `FuelWebController` (`/fuel/**`)
  6. `MaintenanceWebController` (`/maintenance/**`)
  7. `ProfileWebController` (`/profile/**`)
  8. `AdminWebController` (`/admin/**`)
- **REST API Controllers (5):**
  1. `VehicleApiController` (`/api/vehicles/**`)
  2. `ServiceApiController` (`/api/services/**`)
  3. `FuelApiController` (`/api/fuel/**`)
  4. `MaintenanceApiController` (`/api/maintenance/**`)
  5. `DashboardApiController` (`/api/dashboard/**`)
- **Documentation:**
  - `docs/REQUIREMENTS.md` (Full Functional Specification)
  - `docs/DATABASE_DESIGN.md` (Full ERD, Data Dictionary, and Integrity Constraints)
  - `docs/SECURITY_DESIGN.md` (RBAC Matrix, Security Architecture, and Test Coverage)
  - `PROJECT_STATUS.md` (Milestone Tracker)

---

## Next Action

Awaiting instruction to proceed to **M4: Vehicle Module (Vehicle CRUD, Ownership Guarding, License Plate Uniqueness per Garage, Category Association, and Odometer Tracking)**.