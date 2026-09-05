# PROJECT_STATUS.md — MyGarage (APPJFS19)

**Project:** MyGarage – A Vehicle Service History, Fuel Record and Maintenance Tracking Platform  
**Project ID:** APPJFS19  
**Last Updated:** 2026-09-05  
**Current Phase:** **M19 COMPLETE & FULLY VERIFIED — 439/439 TESTS PASS — 52/52 PLAYWRIGHT SCENARIOS PASS**

---

## 1. Environment Snapshot

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

## 2. Milestone Test Progression Matrix

| Milestone | Commit | Deliverable / Test Focus | Milestone Suite | Tests | Cumulative | Status |
|---|---|---|---|---|---|---|
| **M1** | `91ea28f` | Architecture & Smoke QC | `MyGarageApplicationTests` | 1 | 1 | **PASS** |
| **M2** | `7c83dcd` | Database & JPA Entities | `JpaRepositoryTest`, `PasswordEncoderTest` | 13 | 14 | **PASS** |
| **M3** | `6127ff4` / `bda9c05` | Auth, RBAC & Matcher Order | `AuthenticationAndAuthorizationTest` | 23 | 37 | **PASS** |
| **M4** | `fed6a45` | Vehicle CRUD & Ownership | `VehicleModuleTest` | 26 | 63 | **PASS** |
| **M5** | `d73b9e0` | Service Module & History | `ServiceModuleTest` | 25 | 88 | **PASS** |
| **M6** | `e8f78a8` | Fuel Module & Mileage Calc | `FuelModuleTest` | 28 | 116 | **PASS** |
| **M7** | `a5d9940` | Maintenance Status & Alerts | `MaintenanceModuleTest`, `MaintenanceServiceTest` | 30 | 146 | **PASS** |
| **M8** | `26a76d2` | Dashboard & Real Analytics | `DashboardModuleTest` | 27 | 173 | **PASS** |
| **M9** | `8a28750` | Admin Governance & System Mgmt | `AdminModuleTest` | 30 | 203 | **PASS** |
| **M10** | `2b48ae9` / `5bef344` | Polish, 6-Layer QC & Profile APIs | `QualityControlModuleTest` | 15 | 218 | **PASS** |
| **M11** | `f11e861` | Global Record Logs & Monitoring | `GlobalRecordsModuleTest` | 20 | 238 | **PASS** |
| **M12** | `c9cf4d0` | Vehicle Resale Dossier & CSV Export | `VehicleReportAndExportModuleTest` | 26 | 264 | **PASS** |
| **M13** | `e00c8cb` | Comparative Analytics & Fleet Benchmarking | `VehicleComparisonModuleTest` | 24 | 288 | **PASS** |
| **M14** | `d26f52b` | Predictive Maintenance Forecasting & Health Scoring | `PredictiveMaintenanceModuleTest` | 25 | 313 | **PASS** |
| **M15** | `c4a5bd6` | TCO Lifecycle Modeling, Depreciation Valuation & Replacement Advisory | `VehicleTcoLifecycleModuleTest` | 25 | 338 | **PASS** |
| **M16** | `a23a131` | Fuel Efficiency Intelligence & Price Analytics Engine | `FuelAnalyticsModuleTest` | 25 | 363 | **PASS** |
| **M17** | `373f0e9` | Vehicle Reliability Engineering, Component Failure Risk & Chronic Defect Intelligence | `VehicleReliabilityEngineModuleTest` | 25 | 388 | **PASS** |
| **M18** | `0bcd98d` | Vehicle Operational Readiness, Journey Risk Simulator & Fleet Mission Dispatch Engine | `VehicleReadinessEngineModuleTest` | 25 | 413 | **PASS** |
| **M19** | `HEAD` | Vehicle Maintenance Deficit Index (MDI), Deferred Backlog Debt & Compound Neglect Engine | `VehicleMaintenanceDeficitModuleTest` | 26 | 439 | **PASS** |

---

## 3. Milestone 14 (M14) Verification Matrix

| Check / Requirement | Specification | Status |
|---|---|---|
| **Web MVC Routes** | `GET /vehicles/{id}/forecast`, `GET /vehicles/planner`, `POST /vehicles/{id}/forecast/schedule` | **PASS** |
| **REST APIs** | `GET /api/vehicles/{id}/forecast`, `GET /api/analytics/garage-forecast`, `POST /api/vehicles/{id}/forecast/schedule` | **PASS** |
| **Driving Velocity Engine** | Historical distance / days velocity with strict rollback & negative delta anomaly suppression | **PASS** |
| **Repeating PMS Milestones** | OEM intervals at 5k, 10k, 20k, 40k, 60k multiples with dynamic odometer & projected date calculations | **PASS** |
| **Vehicle Health Index (VHI)** | Multi-factor composite health score strictly clamped between 0 and 100 with qualitative grading | **PASS** |
| **Horizon Expense Forecasting** | Forward expense projections strictly filtering milestones within 30, 60, 90, and 180-day windows | **PASS** |
| **Duplicate Prevention** | Graceful blocking of duplicate pending milestones (flash warning in Web MVC, 409 Conflict in REST API) | **PASS** |
| **Security & RBAC** | Admin blocked from forecast views & APIs (403); Normal user isolated to owned vehicles | **PASS** |
| **Data Isolation & Tampering** | Cross-user tampering in forecast redirects with flash error (Web) or returns 403 (REST) | **PASS** |
| **Automated Tests** | 25 dedicated tests in `PredictiveMaintenanceModuleTest` | **PASS (25/25)** |
| **Full Regression Suite** | 313 automated tests across 16 test classes | **PASS (313/313, 0 failures, 0 errors)** |
| **Live Verification** | End-to-end Tomcat + MySQL verification passed across all 22 acceptance criteria checks | **PASS (22/22)** |

---

## 4. Milestone 15 (M15) Verification Matrix

| Check / Requirement | Specification | Status |
|---|---|---|
| **Web MVC Routes** | `GET /vehicles/{id}/tco`, `GET /vehicles/tco` | **PASS** |
| **REST APIs** | `GET /api/vehicles/{id}/tco`, `GET /api/analytics/garage-tco` | **PASS** |
| **Lifecycle OPEX Aggregation** | Fuel + Service + Maintenance cumulative, monthly, annualized run-rates, and cost/km | **PASS** |
| **Depreciation Curve Engine** | Category benchmark MSRPs, age-based declining balance, mileage intensity factor, 10% salvage floor | **PASS** |
| **Defensive Boundary Guards** | Zero odometer, zero residual, missing service/fuel/maintenance logs, vintage vehicles | **PASS** |
| **Economic Replacement Advisory** | Trailing 12-month maintenance filtering, RRVR calculation, actionable advisory status & rationale | **PASS** |
| **Direct Tailpipe Carbon ESG** | Litres consumed * fuel density * emission factor (kg/L) with zero emission for EVs | **PASS** |
| **Security & RBAC** | `ROLE_ADMIN` blocked from personal TCO views & APIs (403); Normal users isolated to owned vehicles | **PASS** |
| **Cross-User Tampering** | Tampering redirected with flash error (Web MVC) or returns 403 Forbidden (REST API) | **PASS** |
| **Dedicated Automated Tests** | 25 dedicated tests in `VehicleTcoLifecycleModuleTest` | **PASS (25/25)** |
| **Full Regression Suite** | 338 automated tests across 17 test classes | **PASS (338/338, 0 failures, 0 errors)** |
| **Live Verification** | End-to-end Tomcat + MySQL verification passed across all 22 acceptance criteria checks | **PASS (22/22)** |

---

## 5. Documentation Inventory

1. [`README.md`](README.md) — Comprehensive technical overview, architecture, quick-start guide, and badges.
2. [`PROJECT_STATUS.md`](PROJECT_STATUS.md) — Milestone tracking and test progression matrix.
3. [`docs/REQUIREMENTS.md`](docs/REQUIREMENTS.md) — Detailed functional and non-functional requirements specification.
4. [`docs/DATABASE_DESIGN.md`](docs/DATABASE_DESIGN.md) — ERD, data dictionary, cascade and index specifications.
5. [`docs/SECURITY_DESIGN.md`](docs/SECURITY_DESIGN.md) — Security filter chain ordering, RBAC matrix, and CSRF specification.
6. [`docs/VEHICLE_MODULE_DESIGN.md`](docs/VEHICLE_MODULE_DESIGN.md) — Vehicle module technical specification.
7. [`docs/SERVICE_MODULE_DESIGN.md`](docs/SERVICE_MODULE_DESIGN.md) — Service module technical specification.
8. [`docs/FUEL_MODULE_DESIGN.md`](docs/FUEL_MODULE_DESIGN.md) — Fuel module technical specification.
9. [`docs/MAINTENANCE_MODULE_DESIGN.md`](docs/MAINTENANCE_MODULE_DESIGN.md) — Maintenance module technical specification.
10. [`docs/DASHBOARD_REPORTING_DESIGN.md`](docs/DASHBOARD_REPORTING_DESIGN.md) — Dashboard and financial analytics specification.
11. [`docs/ADMIN_MODULE_DESIGN.md`](docs/ADMIN_MODULE_DESIGN.md) — Admin governance and primary admin protection specification.
12. [`docs/M10_POLISH_AND_QC_DESIGN.md`](docs/M10_POLISH_AND_QC_DESIGN.md) — M10 polish, 6-layer QC framework, and profile API specification.
13. [`docs/M11_GLOBAL_RECORDS_MONITORING_DESIGN.md`](docs/M11_GLOBAL_RECORDS_MONITORING_DESIGN.md) — M11 cross-vehicle records and monitoring specification.
14. [`docs/M12_REPORT_AND_EXPORT_DESIGN.md`](docs/M12_REPORT_AND_EXPORT_DESIGN.md) — M12 vehicle resale dossier, CSV export, and reporting specification.
15. [`docs/M13_VEHICLE_COMPARISON_ANALYTICS_DESIGN.md`](docs/M13_VEHICLE_COMPARISON_ANALYTICS_DESIGN.md) — M13 comparative analytics and fleet efficiency benchmarking specification.
16. [`docs/M14_PREDICTIVE_MAINTENANCE_PLANNER_DESIGN.md`](docs/M14_PREDICTIVE_MAINTENANCE_PLANNER_DESIGN.md) — M14 predictive maintenance forecasting, vehicle health scoring, and smart service planner specification.
17. [`docs/M15_TCO_LIFECYCLE_VALUATION_DESIGN.md`](docs/M15_TCO_LIFECYCLE_VALUATION_DESIGN.md) — M15 Total Cost of Ownership (TCO) lifecycle modeling, depreciation valuation, economic replacement advisory, and carbon ESG intelligence specification.
18. [`docs/API_DOCUMENTATION.md`](docs/API_DOCUMENTATION.md) — REST API endpoint catalog and schemas.
19. [`docs/TESTING.md`](docs/TESTING.md) — Automated testing strategy and regression metrics (Layers 1–12).
20. [`docs/PLAYWRIGHT_QC_M1_M15.md`](docs/PLAYWRIGHT_QC_M1_M15.md) — Playwright browser QC audit report (40 scenarios, M1–M15).
21. [`docs/STUDENT_GUIDE_TANGLISH.md`](docs/STUDENT_GUIDE_TANGLISH.md) — Tanglish viva preparation and demo guide.
22. [`FINAL_STATUS.md`](FINAL_STATUS.md) — Final production readiness sign-off report.

---

## 6. Playwright Browser QC Audit (M1–M15) — Post-M15 Quality Gate

| Metric | Result |
|---|---|
| **Suite** | `e2e/playwright_qc_suite.py` |
| **Engine** | Playwright 1.49+ / Headless Chromium |
| **Target** | http://localhost:8080 (live MySQL 8.0 + Spring Boot/Tomcat) |
| **Scenarios** | 46 |
| **Passed** | **46** |
| **Failed** | **0** |
| **Console Errors** | **0** |
| **Network Failures (unexpected)** | **0** |
| **Verdict** | **CLEAN PASS** |

### Defects Found & Fixed During QC
| # | Component | Defect | Fix |
|---|---|---|---|
| D1 | `fuel/index.html` | CSS not loading: missing `th:` prefix on `href` | Added `th:href` |
| D2 | S15 | Wrong form field name `odometerReading` | Corrected to `odometerAtService` |
| D3 | S24 | CSV test used wrong URL patterns | Fixed to match `ReportWebController` routes |
| D4 | S28 | Schedule modal submit not visible | Added modal trigger click before submit |
| D5 | S29 | Disclaimer assertion too literal | Updated to match actual TCO disclaimer text |
| D6 | S34 | Admin toggle test used wrong approach | Rewrote as browser-context form POST |
| D7 | S40 | Admin session leaked across test scenarios | Explicit User A re-authentication |
| D8 | Suite | Console listener per-scenario inflation | Refactored to single page-level listener |
| D9 | Suite | All HTTP 4xx counted as network failures | Added context-aware status-code filter |

---

## Project Status: PLAYWRIGHT QC COMPLETE — READY FOR M16 PLANNING