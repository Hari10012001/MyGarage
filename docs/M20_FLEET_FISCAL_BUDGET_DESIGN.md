# M20 - Vehicle Operational Budgeting, Predictive Cash-Flow Forecast & Maintenance Expense Burn-Rate Engine (Fleet Fiscal Management)

**Milestone:** M20  
**Status:** IMPLEMENTED & VERIFIED  
**Baseline Commit:** `eada1cc`  
**Database Schema:** Frozen 6-Table Architecture (Zero DDL, Zero Migrations)  

---

## 1. Executive Summary & Domain Objective

While prior milestones deliver predictive maintenance wear dates (M14), reliability failure metrics (M17), readiness trip simulation (M18), and asset deficit debt (M19), fleet owners previously lacked a forward-looking **fiscal cash-flow forecasting, expense burn-rate modeling, and liquidity reserve advisory engine**.

Milestone 20 introduces the **Vehicle Operational Budgeting, Predictive Cash-Flow Forecast & Maintenance Expense Burn-Rate Engine**:
- **Discrete Completed Calendar Window**: Analyzes historical spend across $N$ full completed calendar months ($N = \min(12, \max(1, \text{monthsActive}))$, excluding the current partial month) and explicitly materializes zero-spend months with $0.00.
- **Deterministic Historical Event Deduplication**: Prevents double-counting when completed service records and maintenance records represent the same physical service event (matching same vehicle, $\le 2$ calendar days apart, mandatory identical normalized subsystem, and cost difference $< \$0.01$ or missing-cost resolution).
- **Expense Volatility & Risk Index (EVRI, 0–100)**: Evaluates monthly burn variation via the coefficient of variation ($\text{CV} = s / \bar{B}$), scaled to a 0–100 index with mathematical boundary guards ($\bar{B} == 0 \implies 0.0$; $N == 1 \implies 0.0$ with `LOW` confidence) and four standardized tiers: `STABLE`, `MODERATE`, `ELEVATED`, and `VOLATILE`.
- **12-Month Forward Cash-Flow Projections**: Forecasts exact monthly operational outlays across discrete calendar months $[M+1, M+12]$ combining 4-tier fuel forecast fallbacks and predictive maintenance obligations.
- **Shared Analytical Dependency**: Reuses M14 `PredictiveMaintenanceService` directly for velocity and wear projections without duplicate wear engines.
- **Deterministic Future Obligation Deduplication**: Deduplicates maintenance obligations in the same calendar month using `NormalizedSubsystem + NormalizedScopeCode`.
- **Recommended Liquidity Buffer Advisory**: Calculates necessary working capital buffer using the approved multi-factor formula:
  \[
  L_{\text{rec}} = \max\left(\$250.00, \max(\text{historicalPeakSingleSpend}, \bar{B} \times 1.5) \times \left(1 + \frac{\text{EVRI}}{100}\right) \times \text{AgeFactor}\right)
  \]
- **Evidence-Based Data Confidence Rating**: Non-persistent data confidence classification (`HIGH`, `MEDIUM`, `LOW`, `BASELINE_ONLY`) reflecting the volume and continuity of underlying records.
- **Garage Fleet Budget Matrix**: Aggregates fleet-wide monthly cash flows, identifies highest burn vehicles, and summarizes 12-month consolidated liquidity requirements.

---

## 2. Non-Negotiable Architectural Invariants

1. **Frozen 6-Table Database Architecture**:
   - Strictly preserves existing 6 tables: `users`, `vehicle_categories`, `vehicles`, `service_records`, `fuel_records`, `maintenance_records`.
   - Zero new tables, columns, foreign keys, or DDL migrations.
2. **Direct Reuse of M14 Predictive Engine**:
   - Directly injects and leverages `PredictiveMaintenanceService` for mileage velocity and consumable wear date calculations.
   - Prohibits independent or divergent consumable wear calculations.
3. **Currency Standard**:
   - Expressed uniformly in **USD ($)** across all analytical models, DTOs, and Thymeleaf UI views.
4. **Explicit Analytical Modeling Disclaimer**:
   - All budget forecasts, burn rates, EVRI indices, and liquidity recommendations are explicitly labeled as MyGarage analytical modeling heuristics derived from historical vehicle logs and configurable benchmark cost assumptions. They do not constitute certified accounting, financial planning, or tax advice.
5. **Strict Two-Tier Ownership Security**:
   - Authenticated owners access only their own vehicles. Cross-user REST requests receive `HTTP 403 Forbidden`; Web MVC requests safely redirect to `/vehicles` with an `errorMsg` flash attribute.
   - `ROLE_ADMIN` is strictly forbidden (`HTTP 403`) from accessing user fiscal budget endpoints.

---

## 3. Historical Burn Rate Engine & Calendar Window

### 3.1 Completed Calendar Window
- Only fully completed calendar months are included in the baseline window $N$:
  \[
  N = \min(12, \max(1, \text{monthsActive}))
  \]
- The current partial month is strictly excluded from historical variance calculation.
- `historyStartDate` is determined deterministically:
  \[
  \text{historyStartDate} = \begin{cases}
  \text{vehicle.createdAt.toLocalDate()}, & \text{if vehicle.createdAt is present} \\
  \min(\text{earliest dated record}), & \text{if records exist} \\
  \text{LocalDate.now()}, & \text{otherwise}
  \end{cases}
  \]
- `monthsActive` is the number of full calendar months between `historyStartDate` and the start of the current month.
- Every month in the window $[M - N, M - 1]$ is represented; months with zero recorded expenses are explicitly populated with $\$0.00$.

### 3.2 Historical Service & Maintenance Deduplication
To prevent historical double-counting between `service_records` and `maintenance_records`:
- Match condition:
  1. Same vehicle.
  2. Completion/service dates within $\le 2$ calendar days.
  3. Same normalized subsystem domain is **mandatory**.
  4. Cost resolution:
     - Both positive costs $\implies$ merge only if $|\text{cost}_m - \text{cost}_s| < 0.01$ (deduplicate, retain single cost).
     - One positive cost and one null/zero $\implies$ merge and retain the positive cost.
     - Both null/zero $\implies$ merge and apply authoritative benchmark cost for that subsystem.
     - Different normalized subsystems $\implies$ **never merge**, regardless of cost similarity.

---

## 4. Expense Volatility & Risk Index (EVRI)

The EVRI quantifies financial predictability of monthly operational cash-flows on a normalized scale of 0 to 100:

1. **Mean Monthly Burn ($\bar{B}$)**:
   \[
   \bar{B} = \frac{1}{N} \sum_{i=1}^{N} B_i
   \]
2. **Sample Standard Deviation ($s$)**:
   \[
   s = \sqrt{\frac{1}{N - 1} \sum_{i=1}^{N} (B_i - \bar{B})^2} \quad (\text{for } N > 1)
   \]
3. **Coefficient of Variation ($\text{CV}$)**:
   \[
   \text{CV} = \frac{s}{\max(1.0, \bar{B})}
   \]
4. **EVRI Calculation**:
   \[
   \text{EVRI} = \min\left(100.0, \max\left(0.0, \text{CV} \times 50.0\right)\right)
   \]

### 4.1 Boundary Guards & Standard Tiers
- **Zero-Mean Guard**: If $\bar{B} == 0.0 \implies \text{EVRI} = 0.0$ (`STABLE`).
- **Single-Month Guard**: If $N == 1 \implies s = 0.0, \text{EVRI} = 0.0$ (`STABLE`), and confidence tier is clamped to `LOW`.
- **Approved Volatility Tiers**:
  - `STABLE` ($0.0 \le \text{EVRI} < 25.0$): Highly predictable operational cash-flows.
  - `MODERATE` ($25.0 \le \text{EVRI} < 50.0$): Typical operational fluctuations.
  - `ELEVATED` ($50.0 \le \text{EVRI} < 75.0$): Significant expense spikes requiring attention.
  - `VOLATILE` ($75.0 \le \text{EVRI} \le 100.0$): Extreme variance and cash-flow unpredictability.

---

## 5. 12-Month Forward Cash-Flow Projections

Forward cash-flows are generated for 12 discrete calendar months $[M+1, M+12]$:

### 5.1 Fuel Cost Forecast Hierarchy
1. **Tier 1 (Empirical 90-Day Velocity - `TRAILING_90_DAYS`)**: If $\ge 2$ fuel records exist within the past 90 days, use the rolling 90-day per-km fuel expenditure rate multiplied by projected monthly mileage.
2. **Tier 2 (Empirical Lifetime Velocity - `LIFETIME_AVERAGE`)**: If $\ge 1$ fuel records exist overall, use the lifetime per-km fuel expenditure rate multiplied by projected monthly mileage.
3. **Tier 3 (Category Benchmark Assumption - `CATEGORY_BENCHMARK`)**: If 0 fuel records exist, apply application-level category monthly fuel benchmarks:
   - Car / Sedan: $\$89.29$ / month
   - SUV / Truck: $\$122.73$ / month
   - Motorcycle / Bike: $\$16.45$ / month
   - Scooter: $\$14.29$ / month
   - Electric (EV): $\$30.00$ / month
   - Default: $\$100.00$ / month

### 5.2 Authoritative Immutable Maintenance Benchmark Costs
| Subsystem Domain | Benchmark Cost |
| :--- | :---: |
| `ENGINE_OIL_AND_FILTER` | **$95.00** |
| `BRAKING_SYSTEM` | **$180.00** |
| `COOLING_SYSTEM_FLUIDS` | **$140.00** |
| `TIRES_AND_SUSPENSION` | **$160.00** |
| `MAJOR_PMS` | **$320.00** |
| `GENERAL_MAINTENANCE` | **$110.00** |

### 5.3 Future Obligation Deduplication
Obligations scheduled in the same calendar month are deduplicated using:
\[
\text{Identity Key} = \text{NormalizedSubsystem} + \text{":"} + \text{NormalizedScopeCode}
\]
Explicit scheduled maintenance records take precedence over consumable wear projections for identical identity keys in the same month.

---

## 6. Recommended Liquidity Buffer Advisory

To ensure fleet financial resilience against operational volatility and unforeseen repair spikes, the engine calculates the recommended liquidity buffer:
\[
L_{\text{rec}} = \max\left(\$250.00, \max(\text{historicalPeakSingleSpend}, \bar{B} \times 1.5) \times \left(1 + \frac{\text{EVRI}}{100}\right) \times \text{AgeFactor}\right)
\]
Where:
- $L_{\text{floor}} = \$250.00$ (minimum reserve floor).
- $\text{historicalPeakSingleSpend}$ is the highest single deduplicated historical expenditure event.
- $\bar{B}$ is the rolling monthly burn rate.
- $\text{AgeFactor} = 1.0 + \min\left(0.50, \frac{\max(0, \text{CurrentYear} - \text{VehicleYear})}{20.0}\right)$.
- $\text{EVRIFactor} = 1.0 + \left(\frac{\text{EVRI}}{100.0}\right)$.

---

## 7. Data Confidence & Evidence Metadata

Non-persistent data confidence communicates forecast reliability based on historical record volume and integrity:
- **`confidenceRating`**:
  - `HIGH`: $\ge 6$ completed months evaluated, $\ge 3$ fuel records, and valid distance metric available.
  - `MEDIUM`: $\ge 3$ completed months evaluated and $\ge 1$ fuel record.
  - `LOW`: $< 3$ completed months evaluated or single record.
  - `BASELINE_ONLY`: 0 historical records logged for the vehicle.
- **`fuelEstimationTier`**: `TRAILING_90_DAYS`, `LIFETIME_AVERAGE`, or `CATEGORY_BENCHMARK`.
- **`odometerStatus`**: `NORMAL`, `ROLLBACK_DETECTED`, `ZERO_DELTA`, or `NO_DATA`.

---

## 8. Garage Fleet Budget Matrix

The fleet matrix aggregates all active vehicles owned by the user:
- **Consolidated Monthly Cash Flows**: Sum of projected fuel and maintenance costs across the entire fleet for each of the next 12 months.
- **Fleet Allocation Metrics**: Total portfolio monthly burn, total 12-month annual projected outlay, and consolidated recommended fleet liquidity buffer.
- **Fleet Vehicle Ranking**: Ranked with individual monthly burn, annual projection, liquidity buffer, budget share percentage, and EVRI volatility tier.
