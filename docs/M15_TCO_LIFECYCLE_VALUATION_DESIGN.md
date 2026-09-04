# M15 TECHNICAL DESIGN DOCUMENT — TOTAL COST OF OWNERSHIP (TCO) LIFECYCLE MODELING, DEPRECIATION VALUATION & ECONOMIC REPLACEMENT ADVISORY

**System ID:** APPJFS19  
**Milestone:** M15  
**Feature Title:** Total Cost of Ownership (TCO) Lifecycle Modeling, Depreciation Valuation & Economic Replacement Advisory Module (with Carbon ESG Intelligence)  
**Status:** **ACCEPTED & FULLY VERIFIED**  
**Cumulative Automated Tests:** **338 / 338 PASS (0 failures, 0 errors, 0 skipped)**  
**Live E2E Verification:** **22 / 22 PASS (MySQL 8.0 + Tomcat 8080)**

---

## 1. Overview & Business Rationale

Prior to Milestone 15, MyGarage provided operational tracking (services, fuel, maintenance), backward-looking reporting (resale dossiers, CSV exports), comparative multi-vehicle benchmarking (M13), and distance-based maintenance forecasting (M14). However, owners and fleet managers lacked an econometric capital planning model to answer fundamental lifecycle questions:
1. **The "Keep vs. Replace" Decision:** When should an owner stop sinking repair capital into an aging vehicle and replace it?
2. **True Operating Run-Rates:** What does it genuinely cost annually and monthly to operate each vehicle, and what is the real cost per kilometer ($\text{₹/km}$)?
3. **Asset Equity & Depreciation:** What is the estimated current residual market value of the vehicle given its age, category segment, and accumulated mileage intensity?
4. **Repair-to-Residual-Value Ratio (RRVR):** What percentage of the vehicle's market value was consumed by mechanical repairs over the trailing 12 months?
5. **Direct Tailpipe Carbon Footprint (ESG):** What is the vehicle's cumulative direct greenhouse gas impact in kilograms and metric tonnes of $\text{CO}_2$, and what is its driving intensity in $\text{g CO}_2/\text{km}$?

Milestone 15 delivers a complete econometric lifecycle advisory module without altering the frozen 6-table relational schema.

---

## 2. Architecture & Layering

```
[Browser / HTTP Client]
       │
       ├───────────────────────────────────────┬──────────────────────────────────────────┐
       ▼                                       ▼                                          ▼
[VehicleTcoWebController]              [VehicleTcoApiController]                  [SecurityFilterChain]
(GET /vehicles/{id}/tco,               (GET /api/vehicles/{id}/tco,               (Strict ROLE_NORMAL_USER,
 GET /vehicles/tco)                     GET /api/analytics/garage-tco)             Blocks ROLE_ADMIN,
       │                                       │                                   Session & RBAC)
       └───────────────────┬───────────────────┘
                           ▼
                  [VehicleTcoService]
               (VehicleTcoServiceImpl)
        - Two-Tier Ownership Validation for all IDs
        - Total Lifetime OPEX & Run-Rate Computation
        - Declining-Balance Depreciation Curve Engine
        - Trailing 12-Month Maintenance (T12M) Filter
        - Repair-to-Residual-Value Ratio (RRVR) Engine
        - Economic Replacement & Retention Advisory
        - Direct Tailpipe Carbon Footprint (ESG)
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

## 3. Mathematical Models & Business Safeguards

### A. Lifecycle Operating Cost (OPEX) & Run-Rate
- **Total Lifetime OPEX:**
  $$\text{Total Lifetime OPEX} = \text{Total Service Cost} + \text{Total Fuel Cost} + \text{Total Completed Maintenance Cost}$$
- **Operational Age:**
  $$\text{Age (Years)} = \max\left(1, \text{Current Year} - \text{Vehicle Year}\right)$$
- **Annualized & Monthly Run-Rate:**
  $$\text{Annualized Operating Cost} = \frac{\text{Total Lifetime OPEX}}{\text{Age (Years)}}$$
  $$\text{Monthly Operating Cost} = \frac{\text{Annualized Operating Cost}}{12}$$
- **Cost Per Kilometer:**
  $$\text{Operating Cost Per Km} = \begin{cases} \frac{\text{Total Lifetime OPEX}}{\text{Current Odometer}}, & \text{if Current Odometer} > 0 \\ 0.00, & \text{otherwise} \end{cases}$$

### B. Econometric Asset Depreciation & Residual Equity
- **Segment Baseline Valuation ($V_0$):**
  - Two-Wheeler / Bike / Scooter: ₹2,00,000.00
  - Hatchback: ₹8,00,000.00
  - Sedan: ₹15,00,000.00
  - SUV / Electric / Luxury: ₹25,00,000.00
  - Standard / Other: ₹12,00,000.00
- **Compounded Declining-Balance Depreciation Curve:**
  - Year 1: $15\%$ drop (retention factor $0.85$).
  - Years 2–5: $10\%$ drop per year ($0.90$).
  - Years 6+: $5\%$ drop per year ($0.95$).
- **Mileage Intensity Adjustment:**
  $$\text{Annual Km} = \frac{\text{Current Odometer}}{\text{Age (Years)}}$$
  If $\text{Annual Km} > 20,000\text{ km/yr}$, an accelerated mileage penalty applies:
  $$\text{Penalty Factor} = \max\left(0.70, 1.0 - \left(\frac{\text{Annual Km} - 20000}{5000}\right) \times 0.015\right)$$
- **Salvage Value Floor:**
  $$\text{Retention Factor} = \max\left(0.10, \text{Compounded Retention} \times \text{Penalty Factor}\right)$$
  $$\text{Estimated Residual Value} = V_0 \times \text{Retention Factor}$$

### C. Repair-to-Residual-Value Ratio (RRVR) & Advisory Status
- **Trailing 12-Month Maintenance Burden ($M_{\text{T12M}}$):**
  Sum of service and completed maintenance records with dates $\ge (\text{Today} - 365\text{ days})$.
- **Repair-to-Residual-Value Ratio:**
  $$\text{RRVR} = \left(\frac{M_{\text{T12M}}}{\text{Estimated Residual Value}}\right) \times 100\%$$
- **Advisory Classifications:**
  - $\text{RRVR} < 15\%$: **`HEALTHY_RETENTION`** — Optimal economic holding window; maintenance costs are well within normal operating parameters.
  - $15\% \le \text{RRVR} < 30\%$: **`MODERATE_EXPENSE`** — Normal wear-and-tear; repair costs are justifiable but warrant close monitoring.
  - $30\% \le \text{RRVR} < 50\%$: **`REPLACEMENT_WATCHLIST`** — High repair burden approaching asset equity; budget for replacement within 12–24 months.
  - $\text{RRVR} \ge 50\%$: **`DISPOSAL_RECOMMENDED`** — Diminishing returns; cumulative annual repairs exceed 50% of market value; disposal or trade-in is financially recommended over major overhauls.

### D. Direct Tailpipe Carbon Footprint (ESG Metrics)
- **Direct Fuel Combustion Emission Factors:**
  - Petrol: $2.31\text{ kg CO}_2 / \text{L}$
  - Diesel: $2.68\text{ kg CO}_2 / \text{L}$
  - CNG: $2.75\text{ kg CO}_2 / \text{kg}$
  - Hybrid: $1.60\text{ kg CO}_2 / \text{L}$
  - Electric (EV): $0.00\text{ kg CO}_2$ (Zero direct tailpipe emissions)
- **Direct Driving Carbon Intensity:**
  $$\text{Carbon Intensity} = \frac{\text{Total Direct Emissions (kg)} \times 1000}{\max(1, \text{Current Odometer})} \quad (\text{g CO}_2/\text{km})$$
- **Eco Tailpipe Rating:**
  - `ECO_EXCELLENT`: Electric EV or $< 100\text{ g/km}$
  - `ECO_MODERATE`: $100 - 180\text{ g/km}$
  - `HIGH_EMISSIONS`: $> 180\text{ g/km}$

---

## 4. REST API Reference

| HTTP Method | Endpoint | Description | Auth Required |
|:---|:---|:---|:---|
| `GET` | `/api/vehicles/{id}/tco` | Complete vehicle TCO, residual valuation, RRVR & replacement advisory | `ROLE_NORMAL_USER` |
| `GET` | `/api/analytics/garage-tco` | Garage-wide fleet asset equity, annual operating cost & carbon summary | `ROLE_NORMAL_USER` |

---

## 5. Security & Isolation Matrix

| Operation | Target / Endpoint | Normal User (Owner) | Cross-User Tampering | Admin User (`ROLE_ADMIN`) | Unauthenticated |
|:---|:---|:---|:---|:---|:---|
| Web TCO View | `GET /vehicles/{id}/tco` | `200 OK` | `302 Found` (Flash Error) | `403 Forbidden` | `302` (Login) |
| Web TCO Dispatcher | `GET /vehicles/tco` | `302 Found` (First Vehicle) | N/A | `403 Forbidden` | `302` (Login) |
| REST TCO API | `GET /api/vehicles/{id}/tco` | `200 OK` | `403 Forbidden` | `403 Forbidden` | `302` (Login) |
| REST Fleet TCO | `GET /api/analytics/garage-tco` | `200 OK` | N/A (Isolated to self) | `403 Forbidden` | `302` (Login) |

---

## 6. Verification Summary

### Automated Test Suite (`VehicleTcoLifecycleModuleTest.java`)
- Total M15 Dedicated Tests: **25 / 25 PASS**
- Cumulative Test Progression: **313 $\rightarrow$ 338 PASS (0 failures, 0 errors, 0 skipped)**
- Build Status: `BUILD SUCCESS`

### Live E2E Verification (`verify_m15.py`)
- Real MySQL 8.0 & Spring Boot / Tomcat 8080
- Total Checks Executed: **22 / 22 PASS**
- Zero schema modifications to frozen 6-table database.
