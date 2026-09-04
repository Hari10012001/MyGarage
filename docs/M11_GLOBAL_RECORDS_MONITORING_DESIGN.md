# M11 TECHNICAL DESIGN DOCUMENT — Global Record Logs, Cross-Vehicle Aggregation & System Monitoring

**Project Title:** MyGarage — A Vehicle Service History, Fuel Record and Maintenance Tracking Platform  
**System ID:** APPJFS19  
**Milestone:** **M11 — Global Record Logs, Cross-Vehicle Aggregation & System Monitoring Module**  
**Author:** Hariharan P  
**Date:** 2026-09-04  
**Status:** **ACCEPTED & FULLY VERIFIED**

---

## 1. Executive Summary

Milestone 11 (M11) completes the multi-vehicle user experience and enterprise administrative monitoring by introducing:
1. **Consolidated Cross-Vehicle User Views:** Enables vehicle owners managing multi-vehicle garages to view, search, and aggregate records across all their vehicles in unified interfaces:
   - `/services` & `GET /api/services`: Consolidated service log with search, total expenditure, and vehicle badges.
   - `/fuel` & `GET /api/fuel`: Consolidated fuel log with total litres, total spend, and fleet average economy.
   - `/maintenance` & `GET /api/maintenance`: Centralized maintenance planner with urgency filtering (`ALL`, `OVERDUE`, `DUE_TODAY`, `UPCOMING`, `COMPLETED`) and one-click task completion.
2. **Administrative Record Monitoring:**
   - `/admin/records` & `GET /api/admin/records`: High-level system monitoring providing fleet category distributions, volume counters, and operational health without breaching user privacy or garage isolation boundaries.
3. **Dedicated Test Harness & Full Regression:**
   - 20 dedicated automated tests in `GlobalRecordsModuleTest` covering positive, negative, boundary, authorization, cross-user isolation, tampering, CSRF, and REST API parity.
   - Total regression suite scaled to **238 automated tests with 100% pass rate**.

---

## 2. Architecture & Layered Design

```
[Web MVC Layer]                     [REST API Layer]
  - GlobalRecordsWebController        - ServiceApiController (/api/services)
  - AdminWebController (/records)     - FuelApiController (/api/fuel)
                                      - MaintenanceApiController (/api/maintenance)
                                      - AdminApiController (/api/admin/records)
                  │                                   │
                  ▼                                   ▼
        [Service Layer]
          - ServiceRecordService (getAllServicesForUser, sumCostByUserId)
          - FuelRecordService (getAllFuelForUser, sumQuantityLitres, avgMileage)
          - MaintenanceService (getAllMaintenanceForUser, countByStatus)
          - DashboardService (getAdminRecordMonitoring)
                  │
                  ▼
        [Persistence Layer (Spring Data JPA)]
          - ServiceRecordRepository (JOIN FETCH vehicle WHERE v.user.userId = :userId)
          - FuelRecordRepository (JOIN FETCH vehicle WHERE v.user.userId = :userId)
          - MaintenanceRecordRepository (JOIN FETCH vehicle WHERE v.user.userId = :userId)
                  │
                  ▼
        [Database Layer (MySQL 8.0)]
          - users, vehicle_categories, vehicles, service_records, fuel_records, maintenance_records
```

---

## 3. Security, RBAC & Privacy Isolation

- **Role Boundaries:**
  - `/services/**`, `/fuel/**`, `/maintenance/**` are strictly gated to `ROLE_NORMAL_USER`.
  - `/admin/records` & `/api/admin/records` are strictly gated to `ROLE_ADMIN`.
- **Administrative Isolation:**
  - Administrators cannot access `/services`, `/fuel`, `/maintenance` or any personal garage bills. Direct URL access returns HTTP `403 Forbidden`.
  - Record monitoring reports aggregated quantities only, ensuring zero personal data leakage.
- **Cross-User Data Isolation:**
  - Cross-vehicle queries use `JOIN FETCH` explicitly filtering by `v.user.userId = :userId`, preventing User A from seeing User B's records in the global list.
  - State-changing operations (such as `/maintenance/{id}/complete`) strictly verify ownership before mutation; cross-user tampering is rejected.
- **CSRF Protection:**
  - Enforced on all mutating endpoints (`/maintenance/{id}/complete`).

---

## 4. REST API Endpoint Catalog

| Endpoint | Method | Role | Query Params | Description |
|---|---|---|---|---|
| `/api/services` | `GET` | `NORMAL_USER` | `search` (optional) | Returns all services across all vehicles owned by user |
| `/api/fuel` | `GET` | `NORMAL_USER` | — | Returns all fuel records across all vehicles owned by user |
| `/api/maintenance` | `GET` | `NORMAL_USER` | `status` (optional) | Returns all maintenance tasks across all vehicles owned by user |
| `/api/admin/records` | `GET` | `ADMIN` | — | Returns system-wide record monitoring metadata & category metrics |

---

## 5. Automated Test Suite Metrics (`GlobalRecordsModuleTest`)

| # | Test Method Name | Focus / Scenario | Expected Result |
|---|---|---|---|
| 1 | `testAllServicesView` | View cross-vehicle services | 200 OK, complete list & total cost |
| 2 | `testAllServicesSearch` | Search keyword in services | 200 OK, matching service returned |
| 3 | `testAllFuelView` | View cross-vehicle fuel logs | 200 OK, total litres & spend aggregated |
| 4 | `testAllMaintenanceView` | View cross-vehicle maintenance | 200 OK, urgency counts matching |
| 5 | `testAllMaintenanceStatusFilter` | Filter maintenance by status | 200 OK, filtered tasks only |
| 6 | `testCompleteMaintenanceCentralized`| Complete task from planner | 302 Redirect, status = COMPLETED |
| 7 | `testAdminRecordMonitoringView` | Admin record monitoring view | 200 OK, monitoring model attribute |
| 8 | `testNormalUserBlockedFromAdminRecords`| Normal user security gate | 403 Forbidden |
| 9 | `testAdminBlockedFromUserRoutes` | Admin isolation from user routes | 403 Forbidden |
| 10 | `testCrossUserIsolationServices` | Service data isolation | User B service omitted |
| 11 | `testCrossUserIsolationFuel` | Fuel data isolation | User B fuel omitted |
| 12 | `testCrossUserIsolationMaintenance` | Maintenance data isolation | User B maintenance omitted |
| 13 | `testCrossUserCompleteTamperingRejected`| Complete tampering protection | Error flash, status unchanged |
| 14 | `testRestAllServices` | REST /api/services | 200 OK, JSON array |
| 15 | `testRestAllServicesSearch` | REST /api/services?search= | 200 OK, filtered JSON array |
| 16 | `testRestAllFuel` | REST /api/fuel | 200 OK, JSON array |
| 17 | `testRestAllMaintenance` | REST /api/maintenance | 200 OK, JSON array |
| 18 | `testRestAllMaintenanceStatusFilter`| REST /api/maintenance?status= | 200 OK, filtered JSON array |
| 19 | `testRestAdminRecordMonitoring` | REST /api/admin/records | 200 OK, monitoring JSON object |
| 20 | `testRestNormalUserBlockedFromAdminRecords`| REST admin security gate | 403 Forbidden |

---

## 6. Verification Summary

- **M11 Dedicated Tests:** 20/20 PASS
- **Total System Regression Tests:** 238/238 PASS
- **Failures / Errors:** 0 / 0
- **Regression Result:** `BUILD SUCCESS`
- **Live Verification:** Tested on MySQL 8.0 + Tomcat 8080.