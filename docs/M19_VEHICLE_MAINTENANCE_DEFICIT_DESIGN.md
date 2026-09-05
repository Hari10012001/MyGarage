# M19 - Vehicle Maintenance Deficit Index (MDI), Deferred Backlog Debt & Compound Neglect Engine

**Milestone:** M19  
**Status:** IMPLEMENTED & VERIFIED  
**Baseline Commit:** `0bcd98d`  
**Database Schema:** Frozen 6-Table Architecture (Zero DDL, Zero Migrations)  

---

## 1. Executive Summary & Domain Objective

While prior milestones deliver predictive scheduling (M13/M14), reliability failure tracking (M17), and trip dispatch simulation (M18), vehicle owners previously had no specialized financial risk governance system to quantify **accrued deferred maintenance debt, maintenance deficit relative to asset value, and the compound cost of inaction**.

Milestone 19 introduces the **Vehicle Maintenance Deficit Index (MDI), Deferred Backlog Debt & Compound Neglect Engine**. It models deferred maintenance obligations as financial liabilities and projectable mechanical risks:
- **Maintenance Deficit Index (MDI %)**: A facility and physical asset management metric inspired by engineering backlog analytics, expressing deferred maintenance debt as a percentage of current vehicle residual asset value:
  \[
  \text{MDI (\%)} = \min\left(100.0, \frac{D_{\text{deferred}}}{\max(1000.0, \text{CurrentResidualValue})} \times 100.0\right)
  \]
- **Bifurcated Financial Posture**:
  - **Deferred Maintenance Debt (\(D_{\text{deferred}}\))**: Strictly includes overdue tasks (`scheduledDate < today` or status `OVERDUE`), tasks due today (`scheduledDate == today` or status `DUE_TODAY`), and unmanaged breached consumable intervals. Only this amount inflates the MDI % numerator.
  - **Near-Term Maintenance Exposure (\(E_{\text{nearTerm}}\))**: Planned maintenance scheduled within the next 30 days (`today < scheduledDate <= today + 30 days`). Displayed separately as budgetary advisory commitments.
- **Authoritative Benchmark Policy**: Single immutable policy for subsystem costs, intervals, cascade multipliers, and consequence definitions.
- **Deterministic Deduplication**: Prevents double-counting when an obligation exists in both `maintenance_records` and as an exhausted consumable from `service_records`.
- **Compound Neglect Cascade Exposure**: Models non-linear secondary mechanical failure costs resulting from deferred repairs using domain cascade multipliers (e.g., 5.5x for Engine Oil sludge/seizure, 3.2x for Braking rotor gouging/pressure loss).
- **Multi-Factor Risk Mitigation Efficiency (RME) & 4-Stage Tie-Breaking**: Ranks triage tasks by risk mitigation yield without algebraic collapse by weighting both empirical days overdue and subsystem criticality.
- **Garage Fleet Deficit Matrix**: Fleet-level aggregate debt, garage-wide MDI %, vehicle rankings, and consolidated multi-vehicle priority recovery roadmap.

---

## 2. Non-Negotiable Architectural Invariants

1. **Frozen 6-Table Database Architecture**:
   - Strictly preserves existing 6 tables: `users`, `vehicle_categories`, `vehicles`, `service_records`, `fuel_records`, `maintenance_records`.
   - Zero new tables, columns, foreign keys, or DDL migrations.
2. **Explicit Analytical Modeling Disclaimer**:
   - MDI and cascade projections are explicitly labeled as MyGarage analytical modeling heuristics derived from historical vehicle logs and configurable benchmark cost assumptions. They do not represent official ISO 55000 / NASA metrics or manufacturer-certified engineering warranties.
3. **Strict Two-Tier Ownership Security**:
   - Authenticated owners access only their own vehicles. Cross-user REST requests receive `HTTP 403 Forbidden`; Web MVC requests safely redirect to `/vehicles` with an `errorMsg` flash attribute.
   - `ROLE_ADMIN` is strictly forbidden (`HTTP 403`) from accessing user deficit endpoints.

---

## 3. Authoritative Immutable Benchmark Policy

| Subsystem Domain | Authoritative Benchmark Cost | Benchmark Interval (Km) | Benchmark Interval (Days) | Compound Cascade Multiplier | Priority Weight | Primary Consequence |
| :--- | :---: | :---: | :---: | :---: | :---: | :--- |
| **`ENGINE_OIL_AND_FILTER`** | **$95.00** | 10,000 km | 180 days | **5.5x** | 1.30 | Severe oil sludge, camshaft scoring, turbocharger bearing failure, or catastrophic engine seizure |
| **`BRAKING_SYSTEM`** | **$180.00** | 20,000 km | 365 days | **3.2x** | 1.30 | Metal-on-metal rotor gouging, caliper piston seizure, and hydraulic brake pressure failure |
| **`COOLING_SYSTEM_FLUIDS`** | **$140.00** | 40,000 km | 730 days | **4.8x** | 1.15 | Radiator boiling, blown head gasket, warped cylinder head, and engine overheating |
| **`TIRES_AND_SUSPENSION`** | **$160.00** | 30,000 km | 540 days | **2.5x** | 1.00 | High-speed tire tread separation, blowout risk, and strut mount / tie rod destruction |
| **`MAJOR_PMS`** | **$320.00** | 40,000 km | 730 days | **3.0x** | 1.15 | Broad multi-system degradation, timing belt snap, and loss of powertrain efficiency |
| **`GENERAL_MAINTENANCE`** | **$110.00** | 15,000 km | 365 days | **2.0x** | 1.00 | Premature secondary component wear and operational reliability degradation |

---

## 4. Multi-Factor RME Formula & 4-Stage Tie-Breaking

### 4.1 RME Formulation
\[
\text{NetDamagePrevented} = (\text{ImmediateCost} \times \text{CascadeMultiplier}) - \text{ImmediateCost}
\]
\[
\text{UrgencyWeight} = 1.0 + \min\left(1.5, \frac{\max(0, \text{daysOverdue})}{30.0} \times 0.5\right)
\]
\[
\text{RME Score} = \left(\frac{\text{NetDamagePrevented}}{\text{ImmediateCost}}\right) \times \text{UrgencyWeight} \times \text{PriorityWeight}
\]

### 4.2 Deterministic 4-Stage Tie-Breaking
1. `RME Score` descending (Highest risk-mitigation efficiency first).
2. `NetDamagePrevented` descending (Highest net monetary damage avoided).
3. `ImmediateCost` ascending (Cheaper repairs prioritized to clear risk rapidly).
4. `TaskTitle` case-insensitive ascending, then `RecordId` ascending.

---

## 5. Web MVC & REST Routes

| Route | Method | Access | Description |
| :--- | :---: | :--- | :--- |
| `/vehicles/{id}/maintenance-deficit` | `GET` | Authenticated Owner | Single vehicle MDI dashboard, cost-of-inaction comparison, and triage queue. |
| `/vehicles/maintenance-deficit` | `GET` | Authenticated Owner | Garage fleet deficit matrix, overall fleet MDI %, and consolidated triage. |
| `/api/vehicles/{id}/maintenance-deficit` | `GET` | Authenticated Owner | REST endpoint returning `VehicleMaintenanceDeficitReportDTO`. |
| `/api/analytics/garage-maintenance-deficit` | `GET` | Authenticated Owner | REST endpoint returning `GarageMaintenanceDeficitMatrixDTO`. |

---

## 6. Verification Results

- **Dedicated Unit & Security Tests**: 26/26 PASS (`VehicleMaintenanceDeficitModuleTest`)
- **Cumulative Maven Regression**: 439/439 PASS (0 failures, 0 errors, 0 skipped)
- **Live Spring Boot & MySQL E2E**: Verified on Tomcat 8080 + MySQL 8.0
- **Playwright Browser QC Suite**: 52/52 PASS (including S50, S51, S52)
- **Browser Console Errors**: 0
- **Network Failures**: 0
- **Database Schema**: Strictly frozen at 6 tables.
