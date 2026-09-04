# M10 TECHNICAL DESIGN DOCUMENT — Polish, 6-Layer QC & Documentation Finalization

**Project Title:** MyGarage — A Vehicle Service History, Fuel Record and Maintenance Tracking Platform  
**System ID:** APPJFS19  
**Milestone:** **M10 — Polish, Comprehensive 6-Layer QC & Documentation Finalization**  
**Author:** Hariharan P  
**Date:** 2026-09-04  
**Status:** **ACCEPTED & FULLY VERIFIED**

---

## 1. Executive Overview

Milestone 10 (M10) represents the final engineering, quality control, and packaging phase of the MyGarage enterprise platform. Building directly on the verified baselines of M1 through M9, M10 delivers:
1. **Full REST API Parity for User Profiles:** Added `ProfileApiController` for authenticated user profile retrieval, profile updates, and secure password changes.
2. **Global Exception Handling Hardening:** Standardized RFC 7807/Problem JSON details for all REST endpoints and user-friendly error views for Thymeleaf web controllers, preventing stack trace exposure.
3. **6-Layer Quality Control (QC) Framework:** Multi-tiered verification covering code architecture, database integrity, Spring Security RBAC, business logic, UI/REST polish, and automation.
4. **End-to-End Regression Harness:** 15 dedicated M10 tests in `QualityControlModuleTest`, scaling the test suite to **218 tests with 100% pass rate**.
5. **Academic & Viva Delivery Kit:** Complete production documentation (`README.md`, `API_DOCUMENTATION.md`, `TESTING.md`, `STUDENT_GUIDE_TANGLISH.md`, `FINAL_STATUS.md`) and automated Windows batch scripts (`scripts/*.bat`).

---

## 2. Test Count Progression Across Milestones

To maintain absolute transparency and resolve any historical count ambiguities:

| Milestone | Commit Hash | Module Implemented | Milestone Test Suite | Suite Tests | Cumulative Tests | Status |
|---|---|---|---|---|---|---|
| **M1** | `91ea28f` | Project Setup & Smoke QC | `MyGarageApplicationTests` | 1 | 1 | **PASS** |
| **M2** | `7c83dcd` | Database & JPA Verification | `JpaRepositoryTest`, `PasswordEncoderTest` | 8 | 14 | **PASS** |
| **M3** | `6127ff4` / `bda9c05` | Authentication, RBAC & CSRF | `AuthenticationAndAuthorizationTest` | 23 | 37 | **PASS** |
| **M4** | `fed6a45` | Vehicle Management Module | `VehicleModuleTest` | 26 | 63 | **PASS** |
| **M5** | `d73b9e0` | Service Record Module | `ServiceModuleTest` | 25 | 88 | **PASS** |
| **M6** | `e8f78a8` | Fuel Record & Mileage Module | `FuelModuleTest` | 28 | 116 | **PASS** |
| **M7** | `a5d9940` | Maintenance Reminder Module | `MaintenanceModuleTest`, `MaintenanceServiceTest` | 30 | 146 | **PASS** |
| **M8** | `26a76d2` | Dashboard & Analytics Module | `DashboardModuleTest` | 27 | 173 | **PASS** |
| **M9** | `8a28750` | Admin Governance Module | `AdminModuleTest` | 30 | **203** | **PASS** |
| **M10** | `2b48ae9` | Polish & Comprehensive 6-Layer QC | `QualityControlModuleTest` | 15 | **218** | **PASS** |

*Note: Commit `8a28750` contained exactly 203 automated tests. The addition of `QualityControlModuleTest` (15 tests) in M10 brought the cumulative test count to 218.*

---

## 3. The 6-Layer Quality Control Framework

```
┌────────────────────────────────────────────────────────┐
│  Layer 1: Code Architecture & Quality                  │
│  - Clean 3-tier layering (Web/API -> Service -> JPA)   │
│  - Safe transactions (@Transactional boundaries)       │
├────────────────────────────────────────────────────────┤
│  Layer 2: Database & Persistence Integrity             │
│  - 6 tables, foreign keys, cascade deletes, indexes    │
│  - Eager JOIN FETCH queries eliminating N+1 overhead   │
├────────────────────────────────────────────────────────┤
│  Layer 3: Security, RBAC & Privacy Isolation           │
│  - Matcher ordering: /api/admin/** before /api/**      │
│  - BCrypt cost 10, CSRF on mutating MVC endpoints      │
│  - Primary admin protection, zero user data leakage    │
├────────────────────────────────────────────────────────┤
│  Layer 4: Business Logic Verification                  │
│  - Dynamic fuel mileage (km/L) between fill-ups        │
│  - Deterministic maintenance urgency alerts            │
│  - Category deletion protection when vehicles assigned │
├────────────────────────────────────────────────────────┤
│  Layer 5: Web UI & REST API Polish                     │
│  - Full REST parity (/api/profile, /api/vehicles, etc.)│
│  - RFC 7807 error JSON & user-friendly error views     │
├────────────────────────────────────────────────────────┤
│  Layer 6: Automation & Academic Documentation          │
│  - Batch scripts (setup-db, start, stop, test, health) │
│  - Complete Viva guide in Tanglish for final review    │
└────────────────────────────────────────────────────────┘
```

---

## 4. REST API Additions (`/api/profile/**`)

### 1. Get Profile
- **Endpoint:** `GET /api/profile`
- **Security:** `hasAnyRole('NORMAL_USER', 'ADMIN')`
- **Response:** `200 OK`
  ```json
  {
    "userId": 1,
    "fullName": "John Doe",
    "email": "user@test.com",
    "phone": "9876543210",
    "role": "NORMAL_USER",
    "createdAt": "2026-09-01T12:00:00"
  }
  ```

### 2. Update Profile
- **Endpoint:** `PUT /api/profile`
- **Security:** `hasAnyRole('NORMAL_USER', 'ADMIN')`
- **Request Body:**
  ```json
  {
    "fullName": "John Updated",
    "phone": "9123456780"
  }
  ```
- **Response:** `200 OK`

### 3. Change Password
- **Endpoint:** `POST /api/profile/change-password`
- **Security:** `hasAnyRole('NORMAL_USER', 'ADMIN')`
- **Request Body:**
  ```json
  {
    "currentPassword": "OldPassword@123",
    "newPassword": "NewPassword@456"
  }
  ```
- **Response:** `200 OK` (`{"message": "Password changed successfully."}`)
- **Error:** `400 Bad Request` if current password is wrong or new password is under 6 characters.

---

## 5. Automated Tests Suite (`QualityControlModuleTest.java`)

Contains 15 automated integration tests:
1. `testViewProfilePage` — Web profile index view.
2. `testUpdateProfileSuccess` — Web profile name and phone update.
3. `testChangePasswordSuccess` — Web password update with verification.
4. `testChangePasswordIncorrectCurrentRejected` — Rejection of wrong current password.
5. `testChangePasswordMismatchedRejected` — Rejection of mismatched passwords.
6. `testVehicleDetailsDisplaysCompleteRecords` — Unified record rendering on vehicle details.
7. `testVehicleTimelineDisplaysRecords` — Chronological timeline sequence.
8. `testFuelHistoryDisplaysMileage` — Fuel record mileage display.
9. `testMaintenanceStatusCalculationInView` — Dynamic status evaluation (`OVERDUE`, `COMPLETED`).
10. `testRestGetProfile` — `GET /api/profile` JSON contract.
11. `testRestPutProfile` — `PUT /api/profile` mutation.
12. `testRestChangePasswordSuccess` — `POST /api/profile/change-password`.
13. `testRestProfileUnauthenticatedBlocked` — Unauthenticated REST access redirects to login.
14. `testRestStructuredErrorHandling` — Structured error payload verification.
15. `testFullGarageLifecycleIntegration` — Complete multi-module integration (Vehicle $\rightarrow$ Service $\rightarrow$ Fuel $\rightarrow$ Maintenance $\rightarrow$ Dashboard calculations).

---

## 6. Verification Summary

- **M10 Automated Tests:** 15/15 PASS
- **Total Regression Tests:** 218/218 PASS
- **Failures / Errors:** 0 / 0
- **Regression Result:** `BUILD SUCCESS`
- **Live Verification:** Tested on MySQL 8.0 + Tomcat 8080.