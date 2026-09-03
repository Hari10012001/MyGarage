# PROJECT_STATUS.md — MyGarage (APPJFS19)

**Project:** MyGarage – A Vehicle Service History, Fuel Record and Maintenance Tracking Platform  
**Project ID:** APPJFS19  
**Last Updated:** 2026-09-03  
**Current Phase:** MILESTONE 2 (M2) COMPLETE & FULLY VERIFIED — Ready for M3

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
| Framework | Spring Boot 3.3.5 | Verified |
| Application Port | 8080 | Tested & Clean |

---

## Milestone 2 (M2) Verification & Database Mapping Matrix

| Check / Requirement | Specification | Status |
|---|---|---|
| **Entity Table Mappings** | All 6 entities (`User`, `VehicleCategory`, `Vehicle`, `ServiceRecord`, `FuelRecord`, `MaintenanceRecord`) mapped to exact table names | **PASS** |
| **Primary Keys & Generated IDs** | Standardized `GenerationType.IDENTITY` on all 6 tables (`user_id`, `category_id`, `vehicle_id`, `service_id`, `fuel_id`, `maintenance_id`) | **PASS** |
| **Unique Constraints** | `users(email)`, `vehicle_categories(name)`, `vehicles(user_id, plate_number)` | **PASS** |
| **Indexes** | Optimized indexes on foreign keys, email, role, dates, plate number, maintenance status | **PASS** |
| **Enum Mappings** | `EnumType.STRING` on `Role` (`users`), `FuelType` (`fuel_records`), `MaintenanceStatus` (`maintenance_records`) | **PASS** |
| **Monetary Precision** | `BigDecimal` with `DECIMAL(10, 2)` or `DECIMAL(8, 2)` for zero rounding error | **PASS** |
| **Temporal Data Types** | `LocalDate` for service/fuel/maintenance dates, `LocalDateTime` for audit timestamps | **PASS** |
| **Cascade & Orphan Removal** | Vehicle deletion cascades to `ServiceRecord`, `FuelRecord`, `MaintenanceRecord` (no orphan records left) | **PASS** |
| **Category Protection** | Category deletion blocked if vehicles are assigned (`countByCategoryCategoryId`) | **PASS** |
| **Ownership Isolation** | Strict repository ownership queries: `findBy...AndVehicleUserUserId(..., userId)` | **PASS** |
| **BCrypt Admin Seed** | Verified BCrypt hash for `admin@mygarage.com / Admin@123` in `data.sql` and database | **PASS** |
| **Automated Tests** | 14 automated tests executed (`JpaRepositoryTest`, `MaintenanceServiceTest`, `PasswordEncoderTest`, `MyGarageApplicationTests`) | **PASS (14/14, 0 failures, 0 errors)** |

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
| **M3** | Authentication & Role-Based Access Control | **NEXT** |
| **M4** | Vehicle Module (CRUD & Ownership Protection) | PENDING |
| **M5** | Service Module (Service History & Tracking) | PENDING |
| **M6** | Fuel Module (Fuel Logs & Estimated Mileage) | PENDING |
| **M7** | Maintenance Module (Status Logic & Reminders) | PENDING |
| **M8** | Dashboard & Reports (Real Analytics & Alert Aggregation) | PENDING |
| **M9** | Admin Module (User & Category Management) | PENDING |
| **M10** | Polish, Comprehensive 6-Layer QC & Documentation Finalization | PENDING |

---

## Architecture Inventory

- **Web MVC Controllers (8):**
  1. `AuthWebController` (Login, Register, Logout)
  2. `DashboardWebController` (User Dashboard)
  3. `VehicleWebController` (Vehicle List, Add, Edit, Detail, Timeline, Delete)
  4. `ServiceWebController` (Service Log Add, Edit, Delete)
  5. `FuelWebController` (Fuel Fill-up Log Add, Edit, Delete)
  6. `MaintenanceWebController` (Maintenance Task Add, Edit, Complete, Delete)
  7. `ProfileWebController` (Profile & Password Change)
  8. `AdminWebController` (Admin Dashboard, Users, Categories, Statistics)
- **REST API Controllers (5):**
  1. `VehicleApiController` (`/api/vehicles/**`)
  2. `ServiceApiController` (`/api/services/**`)
  3. `FuelApiController` (`/api/fuel/**`)
  4. `MaintenanceApiController` (`/api/maintenance/**`)
  5. `DashboardApiController` (`/api/dashboard/**`)
- **Documentation:**
  - `docs/REQUIREMENTS.md` (Full Specification)
  - `docs/DATABASE_DESIGN.md` (Full ERD, Data Dictionary, and Integrity Constraints)
  - `PROJECT_STATUS.md` (Milestone Tracker)

---

## Next Action

Awaiting instruction to proceed to **M3: Authentication & Role-Based Access Control (Spring Security, BCrypt, Session Auth, Role Guarding & Ownership Verification)**.