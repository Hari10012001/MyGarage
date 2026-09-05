# Milestone 21: Service Center Ecosystem, Workshop Benchmarking & Vendor Cost Intelligence Engine

## 1. Architectural Overview & Business Scope
Milestone 21 introduces the **Service Center Ecosystem, Workshop Benchmarking & Vendor Cost Intelligence Engine** into MyGarage.

While prior milestones deliver predictive wear schedules (M14), asset lifecycle depreciation (M15), component reliability failure rates (M17), road-trip readiness (M18), asset deficit debt (M19), and operational calendar cash-flow forecasts (M20), vehicle owners face an immediate marketplace challenge: **mechanic billing transparency, vendor price inflation, and repair quality inconsistency**.

M21 transforms historical `service_records.garage_name`, `service_type`, `description`, and `cost` data into a comprehensive **automotive vendor intelligence and procurement analytics platform**. It computes the **Workshop Price Index (WPI)**, evaluates the **Vendor Rework Probability (VRP %)** as a deterministic analytical proxy, classifies service centers into standardized **Workshop Quality & Value Tiers** with deterministic multi-factor precedence, analyzes fleet vendor spend concentration (HHI index), and recommends optimal vendor routing.

### Scope Clarification on Labor and Parts Intelligence:
The frozen relational database schema contains a single aggregated `cost` column in `service_records` and does **not** possess dedicated numeric columns for `labor_cost` or `parts_cost`. Therefore, M21 vendor cost intelligence operates on the total service visit cost and leverages **textual qualitative signals** from `service_type`, `description`, and `notes` (e.g., identifying replaced components, maintenance procedures, and inspection scopes) for domain classification and contextual display. It **strictly does not perform speculative numerical labor-versus-parts cost decomposition**.

### MyGarage Reference Benchmark Cost Disclaimer:
All workshop price indices, vendor ratings, rework probabilities, and quality tiers are model-derived analytical heuristics based on the owner's recorded logbook history and configurable application-level **MyGarage Reference Benchmark Costs**. They do **not** represent official OEM manufacturer warranty schedules, government-certified rates, Better Business Bureau ratings, or legally binding commercial audits of independent repair shops.

---

## 2. Frozen Relational Schema Invariant
M21 strictly enforces the **frozen 6-table database schema**:
1. `users`
2. `vehicle_categories`
3. `vehicles`
4. `service_records`
5. `fuel_records`
6. `maintenance_records`

**Zero new tables, zero new columns, zero foreign keys, zero migrations, zero DDL execution.**
All calculations are performed in-memory from existing JPA repositories.

---

## 3. Mathematical Models & Deterministic Algorithms

### 3.1 Location-Preserving Garage Name Normalization (`GarageNameNormalizer`)
- Null, empty, `"N/A"`, or `"null"` map to `INDEPENDENT_UNSPECIFIED`.
- Punctuation characters `[,.\-/\'\"()#&@_]` are replaced with space.
- Text is capitalized and multiple spaces collapsed.
- Trailing corporate suffixes (`PVT LTD`, `PRIVATE LIMITED`, `LTD`, `LIMITED`, `INC`, `INCORPORATED`, `LLC`, `CORP`, `CORPORATION`, `CO`) are pruned from the end only.
- Intermediate locations (e.g. `Anna Nagar`, `Guindy`, `Downtown`) are strictly preserved.
- Normalized Key: uppercase string with underscores (e.g., `TOYOTA_SERVICE_CENTER_ANNA_NAGAR`).
- Canonical Display Name: Selected deterministically from matching raw strings by:
  1. Occurrence frequency descending.
  2. String length descending (favors descriptive names).
  3. Lexicographical ascending.
  Winning string is cleaned and formatted in Title Case.

### 3.2 Service Subsystem Classification & Benchmarks
Completed service records are classified into 6 automotive subsystem domains:

| Subsystem Domain | MyGarage Reference Benchmark Cost | Keywords |
| :--- | :---: | :--- |
| `ENGINE_OIL_AND_FILTER` | $95.00 | `oil`, `engine oil`, `oil filter`, `lube`, `synthetic`, `10w40`, `5w30`, `0w20` |
| `BRAKING_SYSTEM` | $180.00 | `brake`, `pad`, `rotor`, `caliper`, `brake fluid`, `disc`, `shoe`, `lining`, `abs` |
| `COOLING_SYSTEM_FLUIDS` | $140.00 | `coolant`, `radiator`, `antifreeze`, `water pump`, `thermostat`, `hose`, `flush` |
| `TIRES_AND_SUSPENSION` | $160.00 | `tire`, `tyre`, `alignment`, `balancing`, `rotation`, `strut`, `shock`, `bushing`, `wheel` |
| `MAJOR_PMS` | $320.00 | `pms`, `periodic`, `timing belt`, `major service`, `tune up`, `spark plug`, `transmission overhaul` |
| `GENERAL_MAINTENANCE` | $110.00 | Fallback for general service, inspection, electrical |

### 3.3 Authoritative Shared Eligible-Cost Policy
- **Null / Negative Costs:** Excluded from spend sums, averages, and HHI.
- **Zero Costs ($0.00 — Warranty, Free Inspection, Recall):** Included as `$0.00` spend in total spend, fleet total spend, spend shares, and HHI; tracked separately in `warrantyVisitCount`; excluded only from the commercial WPI denominator ($\sum B_{k(i)}$).
- **Positive Costs ($> 0.00$):** Fully included in all calculations.

### 3.4 Workshop Price Index (WPI)
$$\text{WPI} = \left(\frac{\sum_{i=1}^{M} C_i}{\sum_{i=1}^{M} B_{k(i)}}\right) \times 100.0$$
- $\text{WPI} < 85.0$: `COMPETITIVE_DISCOUNT`
- $85.0 \le \text{WPI} \le 115.0$: `MARKET_PARITY`
- $115.0 < \text{WPI} \le 140.0$: `PREMIUM_PRICING`
- $\text{WPI} > 140.0$: `HIGH_COST_OUTLIER`
- If no priced visits ($M=0$): $\text{WPI} = 100.0$.

### 3.5 Vendor Rework Probability (VRP %) & False-Positive Suppression
- Evaluated as an analytical rework proxy on the same vehicle within 60 calendar days.
- If both odometers are valid and monotonic, distance delta must also satisfy $\le 3000\text{ km}$.
- If odometer data is missing or rollback ($O_B < O_A$), distance check is bypassed; 60-day window governs.
- **False-Positive Suppression:** If follow-up visit $B$ matches routine scheduled keywords and contains zero corrective failure keywords, it is classified as planned maintenance recurrence and is never flagged as rework.
- Otherwise, rework is confirmed if $B$ matches corrective keywords OR operates on the same subsystem as $A$.
- $\text{VRP} (\%) = (\text{Visits followed by rework} / \text{Total visits}) \times 100.0$.

### 3.6 Workshop Value Score (WVS) & 5-Stage Deterministic Tier Precedence
$$\text{WVS} = \max\left(0.0, \min\left(100.0, 100.0 - (\text{WPI} - 100.0) \times 0.35 - (\text{VRP} \times 0.65)\right)\right)$$
1. **Rule 1 (Statistical Significance):** $\text{totalVisits} < 2 \implies \mathbf{TIER\_3\_EVALUATING}$.
2. **Rule 2 (Safety & Quality Failure):** $\text{totalVisits} \ge 2 \land \text{VRP} > 25.0\% \implies \mathbf{TIER\_5\_CAUTION\_HIGH\_REWORK}$.
3. **Rule 3 (Commercial Cost Outlier):** $\text{totalVisits} \ge 2 \land \text{VRP} \le 25.0\% \land \text{WPI} > 135.0 \implies \mathbf{TIER\_4\_CAUTION\_EXPENSIVE}$.
4. **Rule 4 (Preferred Value Partner):** $\text{totalVisits} \ge 2 \land \text{VRP} \le 10.0\% \land \text{WVS} \ge 80.0 \land \text{WPI} \le 135.0 \implies \mathbf{TIER\_1\_PREFERRED}$.
5. **Rule 5 (Standard Market Provider):** All other multi-visit workshops $\implies \mathbf{TIER\_2\_APPROVED}$.

### 3.7 Fleet Vendor Spend Concentration (HHI Index)
$$\text{HHI} = \sum_{W=1}^{K} (\text{Spend Share}_W)^2$$
- $\text{HHI} \ge 5000.0$: `HIGHLY_CONCENTRATED`
- $2500.0 \le \text{HHI} < 5000.0$: `MODERATELY_CONCENTRATED`
- $\text{HHI} < 2500.0$: `HIGHLY_FRAGMENTED`
- Empty fleet (0 visits or $0 spend): $\text{HHI} = 0.0$, `HIGHLY_FRAGMENTED`, `topPreferredWorkshopName = "None (No Workshop History)"`.

---

## 4. Security & Tenant Isolation
- Authenticated normal users access only vendor data derived from their own owned vehicles.
- Cross-user REST requests return `HTTP 403 Forbidden`.
- Nonexistent workshop REST requests return `HTTP 404 Not Found`.
- System Administrator (`ROLE_ADMIN`) is strictly forbidden (`HTTP 403 Forbidden` on REST, redirect on Web) from viewing private user vendor analytics.
- Web MVC cross-user requests redirect to `/workshops` with flash error.
