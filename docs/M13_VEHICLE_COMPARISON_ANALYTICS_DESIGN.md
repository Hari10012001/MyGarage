# M13 TECHNICAL DESIGN DOCUMENT — MULTI-VEHICLE COMPARATIVE ANALYTICS & FLEET EFFICIENCY BENCHMARKING

**System ID:** APPJFS19  
**Milestone:** M13  
**Feature Title:** Multi-Vehicle Comparative Analytics & Fleet Efficiency Benchmarking Module  
**Status:** **ACCEPTED & FULLY VERIFIED**  
**Cumulative Automated Tests:** **288 / 288 PASS (0 failures, 0 errors, 0 skipped)**  
**Live E2E Verification:** **22 / 22 PASS (MySQL 8.0 + Tomcat 8080)**

---

## 1. Overview & Business Rationale

Prior to Milestone 13, MyGarage users could track and view records for individual vehicles or export reports in isolation, but lacked the analytical capability to benchmark multiple vehicles against each other. Vehicle fleet owners, households with multiple automobiles/two-wheelers, and commercial fleet managers routinely face practical questions:
1. **Running Cost Comparison:** Which vehicle in the fleet costs more per kilometer to operate when factoring in combined fuel, services, and scheduled maintenance?
2. **Fuel Economy Benchmarking:** Which vehicle delivers the highest real-world mileage ($\text{km/L}$) based on actual fill-up history?
3. **Maintenance Reliability & Health:** Which vehicle incurs the most repair downtime or scheduled maintenance backlog?
4. **Budget Allocation Analysis:** How is the total garage expenditure distributed across individual vehicles and spending categories?

Milestone 13 delivers a comprehensive solution without altering the underlying database schema:
- **Interactive Multi-Vehicle Comparison Hub (`/vehicles/compare`):** Select 2 to 4 owned vehicles and generate an instant, side-by-side analytical matrix.
- **Fleet Efficiency Highlights & Automated Badging:** Identifies and highlights "Most Economical", "Highest Mileage", "Lowest Running Cost", "Lowest Maintenance", and "Fleet Workhorse".
- **Garage Expenditure Contribution Analysis:** Computes proportional spending shares across fuel, services, and maintenance with dynamic visual progress distribution.
- **REST Comparative Analytics Endpoints:** Fully exposed programmatic APIs for multi-vehicle comparison, pairwise head-to-head analysis, single-vehicle running cost metrics, and fleet breakdown summaries.

---

## 2. Architecture & Layering

```
[Browser / HTTP Client]
       │
       ├───────────────────────────────────────┬──────────────────────────────────────────┐
       ▼                                       ▼                                          ▼
[VehicleComparisonWebController]      [VehicleComparisonApiController]           [SecurityFilterChain]
(GET /vehicles/compare)               (GET /api/analytics/compare,               (Strict ROLE_NORMAL_USER,
                                       GET /api/analytics/fleet-breakdown,        Blocks ROLE_ADMIN,
                                       GET /api/vehicles/{id1}/compare/{id2},     CSRF & matchers)
                                       GET /api/vehicles/{id}/analytics)
       │                                       │
       └───────────────────┬───────────────────┘
                           ▼
              [VehicleComparisonService]
         (VehicleComparisonServiceImpl implementation)
        - Two-Tier Ownership Validation for all IDs
        - Running Cost / km & Fuel Cost / km Computation
        - Fleet Aggregations & Percentage Distributions
        - Automated Performance Badging Engine
                           │
       ┌───────────────────┼───────────────────┬───────────────────┐
       ▼                   ▼                   ▼                   ▼
[VehicleRepository] [ServiceRecordRepo] [FuelRecordRepo] [MaintenanceRecordRepo]
       │                   │                   │                   │
       └───────────────────┴─────────┬─────────┴───────────────────┘
                                     ▼
                                [MySQL 8.0]
```

---

## 3. Data Transfer Objects (DTOs)

### A. `VehicleComparisonDTO`
Captures vehicle specifications, lifetime financial totals, operational efficiency metrics, maintenance reliability statistics, and automated evaluation badges:
- **Specifications:** `vehicleId`, `plateNumber`, `make`, `model`, `year`, `color`, `categoryName`, `fuelType`, `currentOdometer`.
- **Financial Metrics:** `serviceCost`, `fuelCost`, `maintenanceCost`, `totalOwnershipCost`.
- **Operational Efficiency:** `avgMileageKmpl`, `totalFuelLitres`, `runningCostPerKm` ($\text{Total Cost} / \text{Current Odometer}$), `fuelCostPerKm` ($\text{Fuel Cost} / \text{Current Odometer}$).
- **Maintenance Health:** `totalServicesCount`, `totalFuelLogsCount`, `totalMaintenanceTasksCount`, `maintenanceCompletedCount`, `maintenanceOverdueCount`, `maintenanceCompletionRate`.
- **Automated Performance Badges:** `mostFuelEfficient`, `lowestCostPerKm`, `lowestMaintenanceCost`, `fleetWorkhorse`.

### B. `FleetComparisonReportDTO`
Encapsulates comparison report data for a selected subset of vehicles:
- `comparedVehicleCount`: Number of compared vehicles (2 to 4).
- `vehicles`: List of populated `VehicleComparisonDTO` instances.
- `fleetTotalSpend`: Combined lifetime spend across compared vehicles.
- `fleetAverageMileage`: Average fuel economy across vehicles with mileage records.
- `fleetAverageRunningCostPerKm`: Average running cost per km across compared vehicles.
- `mostEfficientVehiclePlate`, `mostEconomicalVehiclePlate`, `lowestMaintenanceVehiclePlate`, `fleetWorkhorsePlate`.

### C. `FleetExpenseBreakdownDTO`
Represents the entire garage's financial distribution:
- `totalVehicles`: Total number of active vehicles owned.
- `totalFleetSpend`: Cumulative garage expenditure.
- `serviceCostPercentage`, `fuelCostPercentage`, `maintenanceCostPercentage`.
- `vehicleShares`: List of `VehicleExpenseShareDTO` items with percentage of fleet spend and color badges.

---

## 4. REST API Reference

| HTTP Method | Endpoint | Description | Auth Required |
|:---|:---|:---|:---|
| `GET` | `/api/analytics/compare?vehicleIds=1,2` | Multi-vehicle comparative analytics report (2–4 vehicles) | `ROLE_NORMAL_USER` |
| `GET` | `/api/vehicles/{id1}/compare/{id2}` | Head-to-head pairwise comparative analytics report | `ROLE_NORMAL_USER` |
| `GET` | `/api/vehicles/{id}/analytics` | Single-vehicle operational metrics & running cost breakdown | `ROLE_NORMAL_USER` |
| `GET` | `/api/analytics/fleet-breakdown` | Garage-wide expenditure breakdown and vehicle budget shares | `ROLE_NORMAL_USER` |

---

## 5. Security & Isolation Matrix

1. **Ownership Isolation (Two-Tier):**
   - The comparison service validates each requested `vehicleId` against the authenticated user's ID via `vehicleRepository.findByVehicleIdAndUserUserId(vId, userId)`.
   - If any ID belongs to another user, execution immediately halts and throws `AccessDeniedException`.
   - In REST APIs, `GlobalExceptionHandler` translates this to **HTTP 403 Forbidden**.
   - In Web MVC, the user is safely redirected back to `/vehicles/compare` with a flash error alert message.
2. **Admin Privacy Isolation:**
   - Administrators (`ROLE_ADMIN`) do not manage personal vehicle fleets.
   - Spring Security configuration explicitly restricts `/vehicles/compare` and `/api/analytics/**` to `hasRole('NORMAL_USER')`.
   - Any access attempt by an administrator returns **HTTP 403 Forbidden**.
3. **Unauthenticated Access:**
   - Unauthenticated requests to `/vehicles/compare` are redirected to `/login` with HTTP 302.
4. **Boundary Validation:**
   - Requests with fewer than 2 or greater than 4 vehicle IDs throw `IllegalArgumentException` and return **HTTP 400 Bad Request**.

---

## 6. Verification & Test Evidence

- **Dedicated Test Suite:** `VehicleComparisonModuleTest.java` (24 test cases):
  - Service logic tests: valid multi-vehicle comparison, pairwise comparison, running cost calculation, automated badge awards, fleet breakdown percentages.
  - Web MVC tests: selection hub rendering, comparison table rendering, flash redirect on cross-user tampering, unauthenticated redirect.
  - REST API tests: `/api/analytics/compare`, `/api/vehicles/{id1}/compare/{id2}`, `/api/vehicles/{id}/analytics`, `/api/analytics/fleet-breakdown`.
  - Security & boundary tests: cross-user tampering (403), admin isolation (403), boundary validation (<2 and >4 vehicles -> 400).
- **Cumulative Regression Suite:** **288 / 288 PASS** (`mvn clean test "-Dspring.profiles.active=test"`).
- **Live E2E Verification:** **22 / 22 PASS** against running MySQL 8.0 and Spring Boot 3.3.5 / Tomcat on port 8080 (`scratch/verify_m13.py`).
