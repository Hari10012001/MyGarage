# M17 — Vehicle Reliability Engineering, Component Failure Risk & Chronic Defect Intelligence Engine

**Milestone:** M17  
**Status:** IMPLEMENTED & VERIFIED  
**Baseline Commit:** `a23a131`  
**Database Schema:** Frozen 6-Table Architecture (Zero DDL, Zero Migrations)  

---

## 1. Executive Summary & Objective

While MyGarage currently provides forward predictive maintenance milestone planning (M14), asset depreciation and total cost of ownership (M15), and fuel efficiency volatility (M16), vehicle owners previously had no automated system to evaluate **retrospective component reliability, breakdown intervals, and recurring mechanical defects**.

Milestone 17 introduces the **Vehicle Reliability Engineering, Component Failure Risk & Chronic Defect Intelligence Engine**. It operates as a deterministic, rule-based mathematical modeling engine that ingests historical service visits and maintenance records to derive industrial reliability metrics:
- **Mean Distance Between Failures (MDBF)** and **Mean Time Between Services (MTBS)**.
- **Subsystem Failure Taxonomy** across 6 automotive domains.
- **Corrective Service Ratio (CSR)** distinguishing unscheduled breakdown repairs from routine care.
- **Chronic Defect Clustering ("Lemon Engine")** identifying repeated failures within operational windows (<180 days or <5,000 km).
- **Vehicle Reliability Index (VRI 0–100)** with qualitative grading (`EXCELLENT`, `GOOD`, `MODERATE`, `POOR`, `CRITICAL_RISK`).
- **Workshop Quality & Mean Return Interval (MRI)** benchmarking service center repair longevity.
- **Garage Fleet Reliability Matrix & Leaderboard** ranking garage vehicles by reliability and diagnosing top fleet vulnerabilities.

---

## 2. Non-Negotiable Architectural Invariants

1. **Frozen 6-Table Database Architecture**:
   - Strictly preserves the existing 6 tables:
     - `users`
     - `vehicle_categories`
     - `vehicles`
     - `service_records`
     - `fuel_records`
     - `maintenance_records`
   - Zero new tables, zero new columns, zero schema changes, zero DDL migrations.
2. **Analytical Classification Disclaimer**:
   - All classifications (routine vs corrective, subsystem mapping, chronic defects, and VRI scores) are **deterministic rule-based analytical models derived from historical service and maintenance records**, not OEM diagnostic scanner (OBD-II) or sensor-confirmed failure telemetry.
3. **Security & Two-Tier Ownership**:
   - Normal users are strictly isolated to their own vehicles. Cross-user attempts redirect to `/vehicles` with a flash error in Web MVC, and return `HTTP 403 Forbidden` in the REST API.
   - `ROLE_ADMIN` is strictly blocked with `HTTP 403 Forbidden` from personal reliability routes and APIs.

---

## 3. Mathematical & Algorithmic Specifications

### 3.1 Subsystem Failure Taxonomy
Historical records are classified into 6 standardized automotive domains using word-boundary regular expressions:
1. `POWERTRAIN_ENGINE`: Engine block, oil, oil/fuel/air filters, timing belt/chain, spark plugs, pistons, cylinders, injectors, turbochargers, coolant, radiator, catalytic converter, fuel pump.
2. `TRANSMISSION_DRIVETRAIN`: Transmission, gearbox, clutch assembly, drive shafts, differential, axles, gears, CV joints.
3. `BRAKING_TIRES`: Brake pads, discs, rotors, calipers, ABS modules, tires, wheel balancing, alignment.
4. `SUSPENSION_STEERING`: Shock absorbers, struts, springs, steering rack, tie rods, ball joints, bushings, sway bars.
5. `ELECTRICAL_BATTERY`: 12V/EV battery, alternator, starter motor, wiring harness, lighting/bulbs, fuses, sensors, ECU, ignition.
6. `HVAC_BODY_AUXILIARY`: Air conditioning compressor, cabin filter, refrigerant, heater core, blower, wiper mechanisms, bodywork, glass, doors, locks.
7. `GENERAL_ROUTINE`: Fallback category for general inspections, checkups, and standard maintenance.

### 3.2 Mean Distance Between Failures (MDBF) & Mean Time Between Services (MTBS)
For a sorted sequence of service records $(S_1, S_2, \dots, S_N)$ where $N \ge 2$:
$$\Delta \text{days}_i = \text{Date}_i - \text{Date}_{i-1}, \quad \Delta \text{km}_i = \text{Odo}_i - \text{Odo}_{i-1}$$
Defensive guards ignore negative odometer deltas ($\Delta \text{km}_i \le 0$) caused by typos or gauge rollbacks.
$$\text{MTBS} = \frac{1}{N-1} \sum_{i=2}^{N} \Delta \text{days}_i, \quad \text{MDBF} = \frac{1}{M} \sum \text{valid } \Delta \text{km}_i$$
If $N < 2$, MDBF and MTBS are gracefully reported as `null` ("Insufficient Data").

### 3.3 Corrective Service Ratio (CSR)
Distinguishes unscheduled corrective repairs ($C_{\text{unscheduled}}$) from scheduled routine maintenance ($C_{\text{routine}}$):
$$\text{CSR} = \frac{\sum \text{Cost}_{\text{unscheduled}}}{\sum \text{Cost}_{\text{total}}} \times 100\%$$

### 3.4 Chronic Defect Clustering ("Lemon Detector")
Records within the same subsystem occurring with $\Delta \text{days} \le 180$ or $\Delta \text{km} \le 5,000$ trigger a chronic alert:
- 2 incidents: `MODERATE` risk
- 3 incidents: `HIGH` risk
- 4+ incidents: `CRITICAL` risk

### 3.5 Vehicle Reliability Index (VRI 0–100)
$$\text{VRI} = \text{clamp}\Big(100 - P_{\text{MDBF}} - P_{\text{CSR}} - P_{\text{Chronic}} + B_{\text{Resilience}}, 0, 100\Big)$$
- Frequency Penalty ($P_{\text{MDBF}}$): up to 30 pts if MDBF < 2,500 km; 20 pts if < 5,000 km; 10 pts if < 8,000 km.
- CSR Penalty ($P_{\text{CSR}}$): up to 25 pts if CSR > 75%; 15 pts if > 50%; 8 pts if > 25%.
- Chronic Penalty ($P_{\text{Chronic}}$): 10 pts per chronic alert (max 30 pts).
- Aging Resilience Bonus ($B_{\text{Resilience}}$): +10 pts for vehicles $\ge 5$ years or $\ge 75,000$ km maintaining CSR $\le 40\%$.

---

## 4. REST API & Web MVC Routes

| Method | Route | Access | Purpose |
|---|---|---|---|
| `GET` | `/vehicles/{id}/reliability` | `ROLE_NORMAL_USER` (Owner) | Per-vehicle reliability engineering dashboard |
| `GET` | `/vehicles/reliability` | `ROLE_NORMAL_USER` | Garage-wide fleet reliability matrix & leaderboard |
| `GET` | `/api/vehicles/{id}/reliability` | `ROLE_NORMAL_USER` (Owner) | JSON reliability report for vehicle |
| `GET` | `/api/analytics/garage-reliability` | `ROLE_NORMAL_USER` | JSON fleet reliability matrix |

---

## 5. Verification Matrix & Quality Gates

- **M17 Dedicated Tests (`VehicleReliabilityEngineModuleTest`)**: 25 / 25 PASS
- **Cumulative Maven Regression**: 388 / 388 PASS (0 failures, 0 errors, 0 skipped)
- **Playwright QC Scenarios**: 46 / 46 PASS (S01–S46)
- **Console Errors**: 0
- **Unexpected Network Failures**: 0
