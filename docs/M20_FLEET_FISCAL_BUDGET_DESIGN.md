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
- **Expense Volatility & Risk Index (EVRI, 0–100)**: Evaluates monthly burn variation via the coefficient of variation ($\text{CV} = s / \bar{B}$), scaled to a 0–100 index with mathematical boundary guards ($\bar{B} == 0 \implies 0.0$; $N == 1 \implies 0.0$ with `LOW` confidence).
- **12-Month Forward Cash-Flow Projections**: Forecasts exact monthly operational outlays across discrete calendar months $[M+1, M+12]$ combining 4-tier fuel forecast fallbacks and predictive maintenance obligations.
- **Shared Analytical Dependency**: Reuses M14 `PredictiveMaintenanceService` directly for velocity and wear projections without duplicate wear engines.
- **Deterministic Future Obligation Deduplication**: Deduplicates maintenance obligations in the same calendar month using `NormalizedSubsystem + NormalizedScopeCode`.
- **Recommended Liquidity Reserve Advisory**: Calculates necessary working capital buffer to absorb peak cash-flow crunches and expense variance.
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
   \text{CV} = \frac{s}{\bar{B}}
   \]
4. **EVRI Calculation**:
   \[
   \text{EVRI} = \min\left(100.0, \frac{\text{CV}}{1.5} \times 100.0\right)
   \]

### 4.1 Boundary Guards & Tiers
- **Zero-Mean Guard**: If $\bar{B} == 0.0 \implies \text{EVRI} = 0.0$.
- **Single-Month Guard**: If $N == 1 \implies s = 0.0, \text{EVRI} = 0.0$, and confidence tier is clamped to `LOW`.
- **Volatility Tiers**:
  - `LOW` (0.00 – 29.99): Highly predictable operational cash-flows.
  - `MODERATE` (30.00 – 59.99): Typical operational fluctuations.
  - `HIGH` (60.00 – 84.99): Significant expense spikes requiring attention.
  - `CRITICAL` (85.00 – 100.00): Extreme variance and cash-flow unpredictability.

---

## 5. 12-Month Forward Cash-Flow Projections

Forward cash-flows are generated for 12 discrete calendar months $[M+1, M+12]$:

### 5.1 Fuel Cost Forecast Hierarchy
1. **Tier 1 (Empirical 90-Day Velocity)**: If $\ge 2$ fuel records exist within the past 90 days, use the rolling 90-day per-km fuel expenditure rate multiplied by projected monthly mileage.
2. **Tier 2 (Empirical Lifetime Velocity)**: If $\ge 2$ fuel records exist overall, use the lifetime per-km fuel expenditure rate multiplied by projected monthly mileage.
3. **Tier 3 (Category Benchmark Assumption)**: If daily velocity is available but fuel data is sparse, apply application-level fuel cost benchmarks:
   - Car: $\$89.29$ / month (based on 1,000 km/mo @ 7.0 L/100km, $1.25/L)
   - SUV: $\$122.73$ / month (1,000 km/mo @ 9.5 L/100km, $1.25/L)
   - Motorcycle / Bike: $\$16.45$ / month (1,000 km/mo @ 3.5 L/100km, $1.25/L)
   - Scooter: $\$14.29$ / month (1,000 km/mo @ 2.5 L/100km, $1.25/L)
   - Electric (EV): $\$30.00$ / month (1,000 km/mo @ 16 kWh/100km, $0.18/kWh)
   - Default: $\$100.00$ / month
4. **Tier 4 (Static Category Default)**: If mileage velocity is zero or unavailable, apply the static monthly category benchmark.

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

## 6. Recommended Liquidity Reserve Advisory

To ensure fleet financial resilience against operational volatility and unforeseen repair spikes:
\[
\text{RecommendedLiquidityReserve} = \text{PeakHistoricalMonthlySpend} + (1.5 \times s) + \text{UrgentNearTerm30DaySpend}
\]
Where:
- $\text{PeakHistoricalMonthlySpend}$ is the highest single-month expense in the historical window.
- $s$ is the historical sample standard deviation.
- $\text{UrgentNearTerm30DaySpend}$ represents maintenance tasks due or overdue within the next 30 days.

---

## 7. Garage Fleet Budget Matrix

The fleet matrix aggregates all active vehicles owned by the user:
- **Consolidated Monthly Cash Flows**: Sum of projected fuel and maintenance costs across the entire fleet for each of the next 12 months.
- **Fleet Allocation Metrics**: Total projected 12-month spend, average monthly fleet burn, fleet-wide liquidity reserve requirements, and risk distribution.
- **Fleet Vehicle Ranking**: Ranked by total 12-month budget descending to identify highest-cost assets.
