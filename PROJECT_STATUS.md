# PROJECT_STATUS.md — MyGarage (APPJFS19)

**Project:** MyGarage – A Vehicle Service History, Fuel Record and Maintenance Tracking Platform  
**Project ID:** APPJFS19  
**Last Updated:** 2026-09-03  
**Current Phase:** MILESTONE 4 (M4) COMPLETE & FULLY VERIFIED — Ready for M5

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

## Milestone 4 (M4) Vehicle Module Verification Matrix

| Check / Requirement | Specification | Status |
|---|---|---|
| **View My Vehicles** | Lists user vehicles with make/model, plate badge, year, color, odo, category icon | **PASS** |
| **Empty State** | User-friendly message and CTA when garage has 0 vehicles | **PASS** |
| **Search Filter** | Dynamic search by make, model, or plate number with clear button | **PASS** |
| **Add Vehicle Form & Submission** | Category selection dropdown, server-side validation, uppercase plate number | **PASS** |
| **Duplicate Plate Protection** | Duplicate plate within user's garage is rejected | **PASS** |
| **Cross-User Plate Uniqueness** | Different users can register the same plate number in separate garages | **PASS** |
| **View Vehicle Details & Timeline** | Displays summary statistics, quick action links, and chronological history | **PASS** |
| **Edit Vehicle** | Pre-populated form, preserves plate or validates new plate, updates fields | **PASS** |
| **Direct URL Access Protection** | User cannot view or edit another user's vehicle by guessing ID | **PASS** |
| **Vehicle Deletion & Cascade** | Deleting vehicle cleanly cascades and deletes all service, fuel, maintenance records | **PASS** |
| **Unauthorized Deletion Protection** | User cannot delete another user's vehicle | **PASS** |
| **REST API (`/api/vehicles`)** | Full CRUD (GET, GET /id, POST, PUT, DELETE) user-scoped with JSON | **PASS** |
| **ADMIN Garage Isolation** | ADMIN is barred from user vehicle web (`/vehicles`) and REST (`/api/vehicles`) | **PASS** |
| **Unauthenticated Protection** | Unauthenticated requests redirected to `/login` | **PASS** |
| **Regression Test Suite** | 63 automated tests (`VehicleModuleTest` [26], `AuthenticationAndAuthorizationTest` [23], `JpaRepositoryTest` [7], `MaintenanceServiceTest` [5], `PasswordEncoderTest` [1], `MyGarageApplicationTests` [1]) | **PASS (63/63, 0 failures, 0 errors)** |
| **Live End-to-End Verification** | Live execution against Spring Boot + MySQL verified all 10 user/admin flows | **PASS** |

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
| **M4** | Vehicle Module (Vehicle CRUD, Ownership Guarding, License Plate Uniqueness, Category Association, Odometer Tracking) | **COMPLETED & FULLY VERIFIED** |
| **M5** | Service Module (Service History, Costs & Garage Tracking) | **NEXT** |
| **M6** | Fuel Module (Fuel Logs & Estimated Mileage) | PENDING |
| **M7** | Maintenance Module (Status Logic & Reminders) | PENDING |
| **M8** | Dashboard & Reports (Real Analytics & Alert Aggregation) | PENDING |
| **M9** | Admin Module (User & Category Management) | PENDING |
| **M10** | Polish, Comprehensive 6-Layer QC & Documentation Finalization | PENDING |

---

## Architecture Inventory

- **Web MVC Controllers (8):**
  1. `VehicleWebController` (`/vehicles/**` - Complete CRUD, details, timeline, search)
  2. `AuthWebController` (`/`, `/login`, `/register`, `/access-denied`)
  3. `DashboardWebController` (`/dashboard/**`)
  4. `ServiceWebController` (`/service/**`)
  5. `FuelWebController` (`/fuel/**`)
  6. `MaintenanceWebController` (`/maintenance/**`)
  7. `ProfileWebController` (`/profile/**`)
  8. `AdminWebController` (`/admin/**`)
- **REST API Controllers (6):**
  1. `VehicleApiController` (`/api/vehicles/**` - Full user-scoped REST CRUD)
  2. `AdminApiController` (`/api/admin/**` - Gated strictly to `ROLE_ADMIN`)
  3. `ServiceApiController` (`/api/services/**`)
  4. `FuelApiController` (`/api/fuel/**`)
  5. `MaintenanceApiController` (`/api/maintenance/**`)
  6. `DashboardApiController` (`/api/dashboard/**`)
- **Documentation:**
  - `docs/REQUIREMENTS.md` (Functional Requirements Specification)
  - `docs/DATABASE_DESIGN.md` (ERD, Data Dictionary, Cascade & Index Rules)
  - `docs/SECURITY_DESIGN.md` (SecurityFilterChain Ordering, RBAC Matrix, CSRF)
  - `docs/VEHICLE_MODULE_DESIGN.md` (Vehicle Module CRUD, Ownership, Validation, APIs)
  - `PROJECT_STATUS.md` (Milestone Tracker)

---

## Next Action

Ready to proceed to **M5: Service Module (Service Record CRUD, Service History Tracking, Garage & Cost Tracking, and Vehicle Association)** upon user instruction.