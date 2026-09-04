# M14 TECHNICAL DESIGN DOCUMENT — PREDICTIVE MAINTENANCE FORECASTING, VEHICLE HEALTH SCORING & SMART SERVICE PLANNER

**System ID:** APPJFS19  
**Milestone:** M14  
**Feature Title:** Predictive Maintenance Forecasting, Vehicle Health Scoring & Smart Service Planner Module  
**Status:** **ACCEPTED & FULLY VERIFIED**  
**Cumulative Automated Tests:** **313 / 313 PASS (0 failures, 0 errors, 0 skipped)**  
**Live E2E Verification:** **22 / 22 PASS (MySQL 8.0 + Tomcat 8080)**

---

## 1. Overview & Business Rationale

Prior to Milestone 14, MyGarage tracked reactive maintenance: records were entered after servicing occurred, or manually scheduled as static tasks with explicit calendar dates. In real-world vehicle ownership and fleet operation, vehicle degradation and service intervals are primarily driven by **distance driven (odometer progression)** rather than arbitrary calendar passage alone.

Milestone 14 introduces an intelligent, predictive, and proactive maintenance layer without modifying the frozen 6-table relational schema:
1. **Driving Velocity Engine (km/day):** Evaluates longitudinal odometer readings across historical service and fuel logs to compute a vehicle's empirical daily driving rate, featuring strict odometer rollback/anomaly filtering.
2. **Repeating Periodic Maintenance Schedule (PMS) Forecasting:** Automatically projects the next due odometer and calendar date for recurring OEM service intervals (5,000 km, 10,000 km, 20,000 km, 40,000 km, 60,000 km multiples) and calculates remaining distance and days.
3. **Vehicle Health Index (VHI) Scoring Engine:** Generates a real-time 0–100 composite health grade based on scheduled task punctuality, service recency, driving age, and fuel log frequency, complete with status ratings (`EXCELLENT`, `GOOD`, `FAIR`, `ATTENTION_REQUIRED`, `CRITICAL`).
4. **Horizon-Based Expense Forecasting (30 / 60 / 90 / 180 Days):** Aggregates projected upcoming service milestones and existing pending tasks into realistic forward-looking maintenance expenditure budgets.
5. **One-Click Smart Service Conversion with Duplicate Prevention:** Allows owners to promote projected PMS milestones into actual tracked `MaintenanceRecord` tasks with automatic title deduplication and state validation.

---

## 2. Architecture & Layering

```
[Browser / HTTP Client]
       │
       ├───────────────────────────────────────┬──────────────────────────────────────────┐
       ▼                                       ▼                                          ▼
[PredictiveMaintenanceWebController]  [PredictiveMaintenanceApiController]       [SecurityFilterChain]
(GET  /vehicles/{id}/forecast,        (GET  /api/vehicles/{id}/forecast,          (Strict ROLE_NORMAL_USER,
 GET  /vehicles/planner,               GET  /api/analytics/garage-forecast,       Blocks ROLE_ADMIN,
 POST /vehicles/{id}/forecast/schedule) POST /api/vehicles/{id}/forecast/schedule) CSRF on POST)
       │                                       │
       └───────────────────┬───────────────────┘
                           ▼
             [PredictiveMaintenanceService]
         (PredictiveMaintenanceServiceImpl)
        - Historical Odometer Velocity Estimation
        - Odometer Rollback & Anomaly Suppression
        - Repeating PMS Interval Projection
        - Composite VHI Health Calculation (0–100 clamp)
        - Horizon-Bounded Forward Expense Aggregation
        - Deduplicated Task Creation & Conflict Handling
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

## 3. Core Algorithms & Mathematical Models

### A. Driving Velocity Estimation & Anomaly Filtering
- Gathers all timestamped odometer readings from `ServiceRecord` and `FuelRecord` instances.
- Rejects negative deltas ($\Delta \text{km} < 0$) as odometer rollbacks or typographical anomalies; rollback distance is never counted as positive travel.
- If span between earliest and latest valid readings $\ge 7$ days:
  $$\text{Daily Velocity} = \frac{\text{Latest Odometer} - \text{Earliest Valid Odometer}}{\text{Days Elapsed}}$$
- If history is insufficient ($< 7$ days or $< 2$ readings), falls back to default vehicle baseline ($35.0 \text{ km/day}$).
- Velocity confidence is marked `HIGH` ($\ge 3$ points, $\ge 14$ days), `MEDIUM` ($\ge 2$ points, $\ge 7$ days), or `DEFAULT_ESTIMATE`.

### B. Repeating PMS Interval Projection Engine
Standard automotive maintenance intervals repeat periodically. For any interval $I \in \{5000, 10000, 20000, 40000, 60000\}$:
$$\text{Next Due Odometer} = \left(\left\lfloor \frac{\text{Current Odometer}}{I} \right\rfloor + 1\right) \times I$$
$$\text{Remaining Km} = \text{Next Due Odometer} - \text{Current Odometer}$$
$$\text{Days Remaining} = \max\left(1, \left\lceil \frac{\text{Remaining Km}}{\text{Daily Velocity}} \right\rceil\right)$$
$$\text{Projected Due Date} = \text{Today} + \text{Days Remaining}$$

Standard repeating catalog:
- **5,000 km:** Minor Service / Oil & Filter Inspection ($\approx \$1,500$)
- **10,000 km:** Standard PMS / Full Oil & Filter Change ($\approx \$3,500$)
- **20,000 km:** Major Service / Brake Fluid & Air/Cabin Filters ($\approx \$7,500$)
- **40,000 km:** Comprehensive Overhaul / Spark Plugs & Coolant Flush ($\approx \$12,000$)
- **60,000 km:** Critical Drive Belt / Timing Belt & Transmission Service ($\approx \$18,000$)

### C. Vehicle Health Index (VHI) Multi-Factor Scoring
The composite score starts from a base of $100$ points with strict clamping to $[0, 100]$:
1. **Overdue Penalty:** $-25$ points per overdue maintenance record.
2. **Punctuality Bonus/Penalty:** $+10$ points if completion rate $> 80\%$, $-10$ points if $< 50\%$.
3. **Recent Service Score:**
   - Service logged within 90 days: $+10$ points.
   - Service logged within 180 days: $+5$ points.
   - No service logged $> 365$ days: $-15$ points.
4. **Mileage & Age Factor:**
   - Odometer $> 100,000\text{ km}$: $-10$ points.
   - Odometer $> 150,000\text{ km}$: $-15$ points.
   - Vehicle age $> 10$ years: $-10$ points.
5. **Score Grading:**
   - $85 - 100$: `EXCELLENT`
   - $70 - 84$: `GOOD`
   - $50 - 69$: `FAIR`
   - $30 - 49$: `ATTENTION_REQUIRED`
   - $0 - 29$: `CRITICAL`

### D. Forward Expense Horizon Projection
Forecast horizons filter only projected milestones whose `projectedDueDate` falls strictly within the respective horizon window $[ \text{Today}, \text{Today} + H ]$ where $H \in \{30, 60, 90, 180\}$ days, added to existing pending scheduled tasks due within that same horizon.

### E. Duplicate Scheduling Prevention
Before converting a forecast milestone to a tracked maintenance task:
- Checks `MaintenanceRecordRepository` for existing pending tasks (`UPCOMING`, `DUE_TODAY`, `OVERDUE`) matching the exact title and vehicle.
- Web MVC: Gracefully blocks creation with a warning alert and redirects back to forecast view.
- REST API: Returns `HTTP 409 CONFLICT` with error description.

---

## 4. REST API Reference

| HTTP Method | Endpoint | Description | Auth Required |
|:---|:---|:---|:---|
| `GET` | `/api/vehicles/{id}/forecast` | Complete predictive maintenance report, VHI score & PMS schedule | `ROLE_NORMAL_USER` |
| `GET` | `/api/analytics/garage-forecast` | Garage-wide fleet health summary & forecast highlights | `ROLE_NORMAL_USER` |
| `POST` | `/api/vehicles/{id}/forecast/schedule` | Convert predicted milestone into an active scheduled maintenance task | `ROLE_NORMAL_USER` |

---

## 5. Security & Isolation Matrix

| Operation | Target / Endpoint | Normal User (Owner) | Cross-User Tampering | Admin User (`ROLE_ADMIN`) | Unauthenticated |
|:---|:---|:---|:---|:---|:---|
| Web Forecast View | `GET /vehicles/{id}/forecast` | `200 OK` | `302 Found` (Flash Error) | `403 Forbidden` | `302` (Login) |
| Web Service Planner | `GET /vehicles/planner` | `302 Found` (First Vehicle) | N/A | `403 Forbidden` | `302` (Login) |
| Web Convert Milestone | `POST /vehicles/{id}/forecast/schedule` | `302` (CSRF verified) | `302 Found` (Flash Error) | `403 Forbidden` | `302` (Login) |
| REST Forecast API | `GET /api/vehicles/{id}/forecast` | `200 OK` | `403 Forbidden` | `403 Forbidden` | `302` (Login) |
| REST Fleet Forecast | `GET /api/analytics/garage-forecast` | `200 OK` | N/A (Isolated to self) | `403 Forbidden` | `302` (Login) |
| REST Convert Milestone | `POST /api/vehicles/{id}/forecast/schedule` | `201 Created` / `409 Conflict` | `403 Forbidden` | `403 Forbidden` | `302` (Login) |

---

## 6. Verification Summary

### Automated Test Suite (`PredictiveMaintenanceModuleTest.java`)
- Total M14 Dedicated Tests: **25 / 25 PASS**
- Cumulative Test Progression: **288 $\rightarrow$ 313 PASS (0 failures, 0 errors, 0 skipped)**
- Build Status: `BUILD SUCCESS`

### Live E2E Verification (`verify_m14.py`)
- Real MySQL 8.0 & Spring Boot / Tomcat 8080
- Total Checks Executed: **22 / 22 PASS**
- Zero schema modifications to frozen 6-table database.
