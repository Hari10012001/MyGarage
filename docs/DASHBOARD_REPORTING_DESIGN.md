# DASHBOARD & REPORTING MODULE DESIGN & SPECIFICATION — MyGarage (APPJFS19)

**Project Name:** MyGarage — A Vehicle Service History, Fuel Record and Maintenance Tracking Platform  
**System ID:** APPJFS19  
**Module:** Milestone 8 (M8) — Dashboard & Reporting Module  
**Author:** Senior Full Stack Architect & Engineering Team  
**Last Updated:** 2026-09-03  
**Status:** **100% IMPLEMENTED, TESTED & VERIFIED**

---

## 1. Module Overview & Architectural Pillars

The Dashboard & Reporting Module acts as the central command center of MyGarage. It aggregates operational data across all 4 operational entities (`vehicles`, `service_records`, `fuel_records`, `maintenance_records`) into actionable real-time insights for vehicle owners, while providing system-level monitoring for administrators without violating user data boundaries.

### Architectural Pillars:
1. **Strict Data & Ownership Isolation:**
   - User dashboards and reporting endpoints query strictly through `user.userId`:
     - Fuel totals: `WHERE f.vehicle.user.userId = :userId`
     - Service totals: `WHERE s.vehicle.user.userId = :userId`
     - Maintenance totals: `WHERE m.vehicle.user.userId = :userId`
   - User B with 0 vehicles receives clean empty states and ₹0 spend metrics with zero data leakage from User A.
2. **Administrative Isolation (No Personal Garage Spying):**
   - The `ADMIN` role is blocked (`403 Forbidden`) from user-level dashboard routes (`/dashboard`, `/api/dashboard/**`).
   - The Admin dashboard (`/admin/dashboard`, `/api/admin/statistics`) displays aggregated platform-wide counts only:
     - `totalUsers`, `totalVehicles`, `totalServiceRecords`, `totalFuelRecords`, `totalMaintenanceRecords`.
3. **Comprehensive Financial & Operational Analytics:**
   - Four core counter cards: Vehicles, Service Records, Fuel Records, Maintenance Tasks.
   - Urgency status breakdown: `OVERDUE`, `DUE_TODAY`, `UPCOMING`, `COMPLETED`.
   - Garage Financial Summary: Total Fuel Spend, Total Service Spend, Total Maintenance Spend, Total Combined Spend.
   - Fuel Economy Analytics: Average estimated mileage ($\text{km/L}$) dynamically computed from verified fuel logs.
4. **Active Maintenance Alerts & Quick Action:**
   - Urgent tasks (`OVERDUE` and `DUE_TODAY`) are surfaced in an alert banner with direct deep-links to the vehicle's maintenance tab.
5. **High-Performance SQL Aggregations (No In-Memory Filtering):**
   - Repository queries use optimized database aggregate functions (`COALESCE(SUM(...), 0)`, `COUNT(...)`, `AVG(...)`).
   - Recent activity queries use `JOIN FETCH` to eliminate N+1 queries and guarantee Jackson serialization and Thymeleaf rendering safety.

---

## 2. Web MVC Architecture & Routes

| HTTP Method | Route | Controller Method | Access Role | Description |
|---|---|---|---|---|
| `GET` | `/dashboard` | `dashboard` | `NORMAL_USER` | Full dashboard view with stat cards, urgency status, financial summary, alerts, vehicle list, and recent activity. |
| `GET` | `/admin/dashboard` | `adminDashboard` | `ADMIN` | System administrator dashboard displaying global platform counts and recent user signups. |
| `GET` | `/admin/statistics` | `statistics` | `ADMIN` | Administrative statistics page with category breakdown and global record counts. |

---

## 3. REST API Specification

| Method | Endpoint | Access Role | Response Status | Description |
|---|---|---|---|---|
| `GET` | `/api/dashboard` | `NORMAL_USER` | `200 OK` / `403` | Returns JSON summary of user's garage (counts, spend, average mileage). |
| `GET` | `/api/dashboard/summary` | `NORMAL_USER` | `200 OK` / `403` | Comprehensive dashboard summary (detailed urgency & financial breakdown). |
| `GET` | `/api/dashboard/alerts` | `NORMAL_USER` | `200 OK` / `403` | Returns JSON array of overdue and due today maintenance alerts for user. |
| `GET` | `/api/dashboard/recent-services` | `NORMAL_USER` | `200 OK` / `403` | Returns latest 5 service records with vehicle metadata. |
| `GET` | `/api/dashboard/recent-fuel` | `NORMAL_USER` | `200 OK` / `403` | Returns latest 5 fuel records with estimated mileage and total costs. |
| `GET` | `/api/admin/statistics` | `ADMIN` | `200 OK` / `403` | Global platform metrics: total users, vehicles, services, fuel, maintenance. |

---

## 4. UI/UX Structure & Empty States

- **Top Stat Cards:** 4 clean cards with Bootstrap icons (`bi-car-front`, `bi-tools`, `bi-fuel-pump`, `bi-calendar-check`).
- **Urgency Status Panel:** 4 colored badges representing status breakdown (`OVERDUE` red, `DUE TODAY` yellow, `UPCOMING` blue, `COMPLETED` green).
- **Financial Spend Panel:** Grid showing Fuel Spend, Service Spend, Total Garage Spend, and Average Fuel Economy ($\text{km/L}$).
- **My Vehicles Summary:** Horizontal mini-cards displaying make, model, year, category icon, fuel type, and current odometer.
- **Recent Services & Fuel Logs:** Side-by-side / stacked responsive tables displaying latest activities.
- **Zero Data Handling:** Friendly empty state illustrations and CTA buttons (*"Add your first vehicle!"*) when no vehicles or activity exist.

---

## 5. Verification & Automated Test Coverage

The dedicated test suite [`DashboardModuleTest.java`](file:///d:/HARIHARAN%20P/000_JAVA%20FULL%20STACK%20-%20Final%20Year%20Main_Projects%20-%202026_2027/019_MyGarage%20-%20A%20Vehicle%20Service%20History,%20Fuel%20Record%20and%20Maintenance%20Tracking%20Platform/backend/src/test/java/com/mygarage/dashboard/DashboardModuleTest.java) executes **27 tests**:

1. Web: Authenticated NORMAL_USER Dashboard Access — `PASS`
2. Web: Dashboard Renders Stat Counts — `PASS`
3. Web: Dashboard Renders Maintenance Urgency Breakdown — `PASS`
4. Web: Dashboard Renders Financial Spend Summary — `PASS`
5. Web: Dashboard Renders Average Fuel Economy — `PASS`
6. Web: Dashboard Displays Active Maintenance Alerts — `PASS`
7. Web: Dashboard Displays Recent Service History — `PASS`
8. Web: Dashboard Displays Recent Fuel Activity — `PASS`
9. Web: Dashboard Displays Multiple Vehicles in Summary — `PASS`
10. Web: Empty-State Behavior When User Has Zero Vehicles — `PASS`
11. Web: Empty-State Behavior When User Has Vehicles But Zero Logs — `PASS`
12. Security: Unauthenticated Access to `/dashboard` Redirects to `/login` — `PASS`
13. Security: ADMIN Role Cannot Access User `/dashboard` (`403 Forbidden`) — `PASS`
14. Security: NORMAL_USER Cannot Access `/admin/dashboard` (`403 Forbidden`) — `PASS`
15. Security: ADMIN Role Can Access `/admin/dashboard` (`200 OK`) — `PASS`
16. Security: ADMIN Dashboard Displays System-Wide Statistics Only — `PASS`
17. Ownership Isolation: User 1 Sees Only User 1's Vehicles and Data — `PASS`
18. Ownership Isolation: User 2 Sees Only User 2's Vehicles and Data (Zero Leakage) — `PASS`
19. REST: `GET /api/dashboard` Returns 200 OK with User Metrics — `PASS`
20. REST: `GET /api/dashboard/summary` Returns Full Financial and Urgency Breakdowns — `PASS`
21. REST: `GET /api/dashboard/alerts` Returns User Maintenance Alerts — `PASS`
22. REST: `GET /api/dashboard/recent-services` Returns Latest User Services — `PASS`
23. REST: `GET /api/dashboard/recent-fuel` Returns Latest User Fuel Logs — `PASS`
24. REST: Unauthenticated API Access Blocked (Redirected to `/login`) — `PASS`
25. REST: ADMIN Role Forbidden from `/api/dashboard` (`403 Forbidden`) — `PASS`
26. REST: NORMAL_USER Forbidden from `/api/admin/statistics` (`403 Forbidden`) — `PASS`
27. REST: ADMIN Role Can Access `/api/admin/statistics` (`200 OK`) — `PASS`

**Full Regression Test Suite:** `mvn clean test "-Dspring.profiles.active=test"`:
- **Total Tests Executed:** **173**
- **Failures:** **0**
- **Errors:** **0**
- **Build Status:** **`BUILD SUCCESS`**