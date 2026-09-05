# M18 - Vehicle Operational Readiness, Journey Risk Simulator & Fleet Mission Dispatch Engine

**Milestone:** M18  
**Status:** IMPLEMENTED & VERIFIED  
**Baseline Commit:** `373f0e9`  
**Database Schema:** Frozen 6-Table Architecture (Zero DDL, Zero Migrations)  

---

## 1. Executive Summary & Objective

While previous milestones provided backward-looking reliability statistics (M17), fuel price arbitrage (M16), and calendar-scheduled maintenance forecasts (M13/M14), vehicle owners and fleet managers previously had no automated system to evaluate **immediate, forward-looking operational readiness for a planned trip or mission**.

Milestone 18 introduces the **Vehicle Operational Readiness, Journey Risk Simulator & Fleet Mission Dispatch Engine**. It functions as an active decision-support and road-trip safety simulation system:
- **Interactive Journey Simulator**: Users simulate any planned trip distance (10 to 10,000 km), trip duration (1 to 30 days), and driving regime (`HIGHWAY_CRUISE`, `MIXED_BALANCED`, `CITY_CONGESTED`, `MOUNTAIN_SEVERE`).
- **Trip Readiness Index (TRI 0–100%)**: Multi-factor composite readiness rating (`MISSION_READY`, `GOOD_CONDITION`, `CAUTION_REQUIRED`, `HIGH_RISK`) strictly clamped between 0 and 100.
- **Mid-Journey Service Breach Engine**: Pinpoints the exact kilometer milestone where a pending maintenance task or scheduled interval will be exceeded mid-trip.
- **Consumable Reserve Margins**: Quantifies distance and time reserves across 4 critical vehicle subsystems:
  - Engine Oil & Filter (Model assumption: 10,000 km / 180 days)
  - Braking System & Fluid (Model assumption: 20,000 km / 365 days)
  - Cooling System & Fluids (Model assumption: 40,000 km / 730 days)
  - Tires & Suspension (Model assumption: 30,000 km / 540 days)
- **Trip Fuel & Range Staging**: Calculates regime-adjusted fuel consumption, estimated trip fuel spend ($), cruising range per full tank, and minimum required fuel stops with a 15% safety buffer.
- **Dynamic Pre-Trip Inspection Checklist**: Multi-tiered actionable checklist (`CRITICAL_ACTION_REQUIRED`, `ADVISORY_CHECK`, `PASSED_VERIFIED`).
- **Garage Fleet Mission Dispatch Comparator**: Benchmarks all garage vehicles for a trip to designate `OPTIMAL_CHOICE`, `VIABLE_ALTERNATIVE`, `HIGH_RISK`, or `NOT_RECOMMENDED`.

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
2. **Explicit Analytical Readiness Disclaimer**:
   - The Trip Readiness Index (TRI) is an **analytical readiness estimate based on recorded vehicle history, deterministic software rules, and benchmark model assumptions**. It is NOT an engineering or mechanical safety guarantee, and does not guarantee that the vehicle will safely complete any journey.
3. **Model Benchmark Interval Assumptions**:
   - Benchmark maintenance intervals (Oil: 10k km / 180d; Brakes: 20k km / 365d; Coolant: 40k km / 730d; Tires: 30k km / 540d) are configurable model assumptions, not universal OEM manufacturer specifications. Missing records produce conservative estimates.
4. **Security & Two-Tier Ownership**:
   - Normal users are strictly isolated to their own vehicles. Cross-user attempts redirect to `/vehicles` with a flash error in Web MVC, and return `HTTP 403 Forbidden` in the REST API.
   - `ROLE_ADMIN` is strictly blocked with `HTTP 403 Forbidden` from personal readiness routes and APIs.

---

## 3. Mathematical & Algorithmic Specifications

### 3.1 Trip Readiness Index (TRI) Composite Scoring
The Trip Readiness Index \(\text{TRI} \in [0, 100]\) is calculated as:
\[
\text{TRI} = \text{clamp}\left(100 - (P_{\text{maint}} + P_{\text{consumables}} + P_{\text{stress}}), 0, 100\right)
\]
Where:
- **Maintenance Penalty (\(P_{\text{maint}}\))**:
  - `OVERDUE` maintenance records: \(15\text{ pts}\) penalty each.
  - `DUE_TODAY` maintenance records: \(12\text{ pts}\) penalty each.
  - `UPCOMING` task breaching within trip duration (\(\le \text{currentDate} + \text{tripDays}\)): \(10\text{ pts}\) penalty.
  - Capped at \(45\text{ pts}\) maximum.
- **Consumable Exhaustion Penalty (\(P_{\text{consumables}}\))**:
  - Subsystem post-trip margin \(< 0\) (\(\text{BREACHED}\)): \(10\text{ pts}\) penalty each.
  - Subsystem post-trip margin \(< 15\%\) of interval (\(\text{LOW\_RESERVE}\)): \(5\text{ pts}\) penalty each.
  - Capped at \(30\text{ pts}\) maximum.
- **Vehicle Age & Odometer Stress Penalty (\(P_{\text{stress}}\))**:
  - Vehicle age \(> 10\text{ years}\) and trip distance \(> 1,000\text{ km}\): \(8\text{ pts}\).
  - Current odometer \(> 200,000\text{ km}\): \(5\text{ pts}\).
  - Capped at \(15\text{ pts}\) maximum.

### 3.2 Consumable Reserve Margin & Breach Kilometer
For each subsystem \(k\):
\[
\text{kmSinceLastService} = \text{currentOdometer} - \text{lastServiceOdometer}
\]
\[
\text{remainingMarginKm} = \max(0, \text{IntervalKm}_k - \text{kmSinceLastService})
\]
\[
\text{postTripMarginKm} = \text{remainingMarginKm} - \text{tripDistanceKm}
\]
If \(\text{postTripMarginKm} < 0\), a **Mid-Trip Interval Breach** occurs at:
\[
\text{breachAtTripKm} = \max(0, \text{remainingMarginKm})
\]

### 3.3 Fuel Staging & Cruising Range Model
1. **Regime Multiplier (\(M_{\text{regime}}\))**:
   - `HIGHWAY_CRUISE`: \(0.90\) (\(10\%\) discount)
   - `MIXED_BALANCED`: \(1.00\) (Baseline)
   - `CITY_CONGESTED`: \(1.15\) (\(15\%\) penalty)
   - `MOUNTAIN_SEVERE`: \(1.25\) (\(25\%\) penalty)
2. **Fuel Needed**:
   \[
   Q_{\text{trip}} = \frac{\text{tripDistanceKm} \times (\eta_{\text{baseline}} \times M_{\text{regime}})}{100}
   \]
3. **Trip Cost**:
   \[
   C_{\text{trip}} = Q_{\text{trip}} \times P_{\text{avgCostPerLiter}}
   \]
4. **Fuel Stops**:
   \[
   \text{Stops} = \max\left(0, \left\lceil \frac{\text{tripDistanceKm}}{\text{CruisingRange} \times 0.85} \right\rceil - 1\right)
   \]

---

## 4. API & Route Inventory

| Route | Type | Method | Access | Purpose |
| :--- | :--- | :---: | :--- | :--- |
| `/vehicles/{id}/readiness` | Web MVC | GET | Normal User (Owner) | Interactive Journey Simulator & Vehicle Readiness page |
| `/vehicles/dispatch` | Web MVC | GET | Normal User | Garage Fleet Mission Dispatch & Vehicle Selector |
| `/api/vehicles/{id}/readiness` | REST API | GET | Normal User (Owner) | JSON simulation report for single vehicle |
| `/api/analytics/garage-dispatch` | REST API | GET | Normal User | JSON dispatch rankings across user garage |

---

## 5. Verification & Acceptance Summary

- **Dedicated Test Suite**: `VehicleReadinessEngineModuleTest.java` (25/25 PASS).
- **Cumulative Maven Regression**: 413/413 PASS across 20 test classes.
- **Playwright QC Suite**: 49/49 PASS (S1–S49) with 0 console errors and 0 network failures.
- **Database Schema**: Frozen 6 tables strictly preserved.
