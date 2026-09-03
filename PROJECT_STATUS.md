# PROJECT_STATUS.md — MyGarage (APPJFS19)

**Project:** MyGarage – A Vehicle Service History, Fuel Record and Maintenance Tracking Platform  
**Project ID:** APPJFS19  
**Last Updated:** 2026-09-03  
**Current Phase:** MILESTONE 6 (M6) COMPLETE & FULLY VERIFIED — Ready for M7

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

## Milestone 6 (M6) Fuel Module Verification Matrix

| Check / Requirement | Specification | Status |
|---|---|---|
| **Fuel History Listing** | Listed on vehicle detail page ordered by `fuelDate DESC` | **PASS** |
| **Empty State** | Clear empty state displayed when vehicle has 0 fuel records | **PASS** |
| **Add Fuel Record** | Form validation, persists record, updates vehicle current odometer if higher | **PASS** |
| **Validation Constraints** | Negative quantity (<0), negative rate (<0), negative odometer (<0) rejected | **PASS** |
| **Total Cost Computation** | Computed automatically on persist/update ($Qty \times Rate$) and live JS | **PASS** |
| **Estimated Mileage** | Dynamically computed ($\Delta \text{Distance} / \text{Litres}$) and displayed as `km/L (est.)` | **PASS** |
| **Edit Fuel Record** | Pre-populated form, preserves vehicle linkage, updates attributes | **PASS** |
| **Delete Fuel Record** | Deletes record with CSRF verification | **PASS** |
| **Two-Tier Ownership Check** | `User -> Vehicle -> FuelRecord` enforced across all operations | **PASS** |
| **Cross-User Fuel Access** | Blocked when User B attempts to access User A's fuel records | **PASS** |
| **Cross-User Fuel Creation** | Blocked when User B attempts to add fuel to User A's vehicle | **PASS** |
| **Cross-User Fuel Mutation** | Blocked when User B attempts to update User A's fuel record | **PASS** |
| **Cross-User Fuel Deletion** | Blocked when User B attempts to delete User A's fuel record | **PASS** |
| **REST API (`/api/fuel/**`)** | Full CRUD (GET list, GET by ID, POST, PUT, DELETE) user-scoped | **PASS** |
| **ADMIN Fuel Isolation** | ADMIN role blocked (`403 Forbidden`) from user fuel routes | **PASS** |
| **Unauthenticated Protection**| Unauthenticated requests redirected to `/login` | **PASS** |
| **Cascade Deletion** | Deleting a vehicle cleanly removes all associated fuel records | **PASS** |
| **Regression Test Suite** | 116 automated tests (`FuelModuleTest` [28], `ServiceModuleTest` [25], `VehicleModuleTest` [26], `AuthenticationAndAuthorizationTest` [23], `JpaRepositoryTest` [7], `MaintenanceServiceTest` [5], `PasswordEncoderTest` [1], `MyGarageApplicationTests` [1]) | **PASS (116/116, 0 failures, 0 errors)** |
| **Live End-to-End Verification** | Live execution against Spring Boot + MySQL verified all fuel flows | **PASS** |

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
| **M5** | Service Module (Service History, Costs & Garage Tracking, Two-Tier Ownership) | **COMPLETED & FULLY VERIFIED** |
| **M6** | Fuel Module (Fuel Logs, Estimated Mileage, Auto Total Cost, Two-Tier Ownership) | **COMPLETED & FULLY VERIFIED** |
| **M7** | Maintenance Module (Status Logic & Reminders) | **NEXT** |
| **M8** | Dashboard & Reports (Real Analytics & Alert Aggregation) | PENDING |
| **M9** | Admin Module (User & Category Management) | PENDING |
| **M10** | Polish, Comprehensive 6-Layer QC & Documentation Finalization | PENDING |

---

## Architecture Inventory

- **Web MVC Controllers (8):**
  1. `FuelWebController` (`/vehicles/{id}/fuel/**` - Add, edit, delete, list, detail)
  2. `ServiceWebController` (`/vehicles/{id}/services/**` - Add, edit, delete, list, detail)
  3. `VehicleWebController` (`/vehicles/**` - CRUD, details, timeline, search)
  4. `AuthWebController` (`/`, `/login`, `/register`, `/access-denied`)
  5. `DashboardWebController` (`/dashboard/**`)
  6. `MaintenanceWebController` (`/maintenance/**`)
  7. `ProfileWebController` (`/profile/**`)
  8. `AdminWebController` (`/admin/**`)
- **REST API Controllers (6):**
  1. `FuelApiController` (`/api/vehicles/{id}/fuel`, `/api/fuel/{id}`)
  2. `ServiceApiController` (`/api/vehicles/{id}/services`, `/api/services/{id}`)
  3. `VehicleApiController` (`/api/vehicles/**`)
  4. `AdminApiController` (`/api/admin/**`)
  5. `MaintenanceApiController` (`/api/maintenance/**`)
  6. `DashboardApiController` (`/api/dashboard/**`)
- **Documentation:**
  - `docs/REQUIREMENTS.md` (Functional Requirements Specification)
  - `docs/DATABASE_DESIGN.md` (ERD, Data Dictionary, Cascade & Index Rules)
  - `docs/SECURITY_DESIGN.md` (SecurityFilterChain Ordering, RBAC Matrix, CSRF)
  - `docs/VEHICLE_MODULE_DESIGN.md` (Vehicle Module CRUD, Ownership, Validation, APIs)
  - `docs/SERVICE_MODULE_DESIGN.md` (Service Module Two-Tier Ownership, Odometer, APIs)
  - `docs/FUEL_MODULE_DESIGN.md` (Fuel Module Mileage Calculations, Total Costs, APIs)
  - `PROJECT_STATUS.md` (Milestone Tracker)

---

## Next Action

Ready to proceed to **M7: Maintenance Module (Status Logic, Scheduled vs Completed Tasks, Priority Logic, and Reminders)** upon user instruction.