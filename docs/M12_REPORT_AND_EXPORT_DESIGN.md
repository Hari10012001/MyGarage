# M12 TECHNICAL DESIGN DOCUMENT — VEHICLE RESALE DOSSIER, DATA EXPORT & ADVANCED REPORTING

**System ID:** APPJFS19  
**Milestone:** M12  
**Feature Title:** Vehicle Resale Dossier, Multi-Format Data Export & Advanced Reporting Module  
**Status:** **ACCEPTED & FULLY VERIFIED**  
**Cumulative Automated Tests:** **264 / 264 PASS (0 failures, 0 errors, 0 skipped)**

---

## 1. Overview & Business Rationale

Prior to Milestone 12, vehicle records were distributed across individual entity views and centralized feeds. However, vehicle owners in real-world environments frequently require official verification documents when:
1. **Reselling a vehicle:** Providing prospective buyers with a certified, chronological logbook of all maintenance, oil changes, parts replacements, and fuel economy.
2. **Insurance audits:** Handing over verifiable service documentation to substantiate market value or claim validity.
3. **Data portability & offline backups:** Exporting structured CSV spreadsheets of vehicle services, fuel receipts, and maintenance checklists compliant with standard spreadsheet tools (Excel, LibreOffice, Google Sheets).

Milestone 12 addresses these core needs by introducing:
- **Vehicle Resale Dossier (`/vehicles/{id}/report`):** A dedicated, print-optimized (`@media print`) digital lifecycle certificate summarizing vehicle specifications, ownership credentials, lifetime expenditures, efficiency calculations, and a verified history ledger.
- **Centralized Reports & Export Hub (`/reports`):** A garage-wide analytics and export center providing financial breakdown cards, fleet average fuel mileage, and quick export actions.
- **RFC 4180 CSV Data Streaming:** High-performance, streaming CSV generation for Services, Fuel Logs, Maintenance Tasks, Unified Master History, and Complete Garage Portfolios.
- **REST Reporting & Export APIs:** Programmatic endpoints delivering structured JSON dossier summaries and CSV streams under `/api/**`.

---

## 2. Architecture & Layering

```
[Browser / HTTP Client]
       │
       ├───────────────────────────────────────┬──────────────────────────────────────────┐
       ▼                                       ▼                                          ▼
[ReportWebController]                 [ReportApiController]                      [SecurityFilterChain]
(GET /reports, /vehicles/{id}/report, (GET /api/reports/garage-summary,          (Enforces ROLE_NORMAL_USER,
 CSV downloads)                        GET /api/vehicles/{id}/export/*)           CSRF & matchers)
       │                                       │
       └───────────────────┬───────────────────┘
                           ▼
                    [ReportService]
            (ReportServiceImpl implementation)
           - Ownership Validation (Two-Tier)
           - Financial Ledger Calculations
           - RFC 4180 CSV Escaping & Encoding
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

### A. `VehicleDossierDTO`
Encapsulates complete vehicle metadata, registered owner credentials, lifetime financial sums, operational efficiency averages, and chronological record lists:
```java
public class VehicleDossierDTO {
    private Long vehicleId;
    private String plateNumber;
    private String make;
    private String model;
    private Integer year;
    private String color;
    private Integer currentOdometer;
    private String fuelType;
    private String categoryName;
    private String notes;

    private String ownerName;
    private String ownerEmail;
    private String ownerPhone;

    private BigDecimal totalServiceCost;
    private BigDecimal totalFuelCost;
    private BigDecimal totalMaintenanceCost;
    private BigDecimal totalOwnershipCost;

    private BigDecimal totalFuelLitres;
    private Double averageMileageKmpl;
    private long totalServicesCount;
    private long totalFuelLogsCount;
    private long totalMaintenanceTasksCount;

    private List<ServiceRecord> services;
    private List<FuelRecord> fuelLogs;
    private List<MaintenanceRecord> maintenanceRecords;

    private LocalDateTime generatedAt;
    private String verificationStatus;
}
```

### B. `GarageReportSummaryDTO`
Provides multi-vehicle portfolio totals and a per-vehicle cost/mileage breakdown:
```java
public class GarageReportSummaryDTO {
    private long totalVehicles;
    private long totalServices;
    private long totalFuelLogs;
    private long totalMaintenanceTasks;

    private BigDecimal totalServiceCost;
    private BigDecimal totalFuelCost;
    private BigDecimal totalMaintenanceCost;
    private BigDecimal totalGarageCost;

    private Double averageMileage;

    private List<VehicleReportItemDTO> vehicleBreakdown;
}
```

---

## 4. Endpoints & Route Specifications

### A. Web MVC Routes

| HTTP Method | Path | Role Required | Description |
|---|---|---|---|
| `GET` | `/reports` | `ROLE_NORMAL_USER` | Renders central Reports & Export Hub with garage cards and table |
| `GET` | `/vehicles/{id}/report` | `ROLE_NORMAL_USER` | Renders print-ready Vehicle Resale Dossier & Service Certificate |
| `GET` | `/vehicles/{id}/export/services/csv` | `ROLE_NORMAL_USER` | Streams CSV download of vehicle service records |
| `GET` | `/vehicles/{id}/export/fuel/csv` | `ROLE_NORMAL_USER` | Streams CSV download of vehicle fuel logs |
| `GET` | `/vehicles/{id}/export/maintenance/csv` | `ROLE_NORMAL_USER` | Streams CSV download of vehicle maintenance tasks |
| `GET` | `/vehicles/{id}/export/all/csv` | `ROLE_NORMAL_USER` | Streams CSV download of vehicle master chronological history |
| `GET` | `/export/garage/csv` | `ROLE_NORMAL_USER` | Streams CSV download of user's complete garage portfolio |

### B. REST API Endpoints

| HTTP Method | Path | Role Required | Output Format | Description |
|---|---|---|---|---|
| `GET` | `/api/reports/garage-summary` | `ROLE_NORMAL_USER` | `application/json` | Lifetime garage financials & vehicle breakdown |
| `GET` | `/api/vehicles/{id}/export/summary` | `ROLE_NORMAL_USER` | `application/json` | Comprehensive vehicle dossier payload |
| `GET` | `/api/vehicles/{id}/export/services` | `ROLE_NORMAL_USER` | `text/csv` | CSV stream of vehicle services |
| `GET` | `/api/vehicles/{id}/export/fuel` | `ROLE_NORMAL_USER` | `text/csv` | CSV stream of vehicle fuel logs |
| `GET` | `/api/vehicles/{id}/export/maintenance` | `ROLE_NORMAL_USER` | `text/csv` | CSV stream of vehicle maintenance tasks |
| `GET` | `/api/vehicles/{id}/export/all` | `ROLE_NORMAL_USER` | `text/csv` | CSV stream of vehicle master history |

---

## 5. Security & Isolation Matrix

1. **Two-Tier Ownership Protection:**
   - Every report and export request verifies that the target `Vehicle` is owned by the currently authenticated user (`vehicle.getUser().getUserId().equals(currentUserId)`).
   - Cross-user vehicle access strictly throws `AccessDeniedException`, returning `403 Forbidden`.
2. **Administrative Privacy Isolation:**
   - `ROLE_ADMIN` accounts are strictly prohibited from viewing user vehicle dossiers or triggering user CSV exports (`403 Forbidden`).
3. **Unauthenticated Redirects:**
   - Unauthenticated requests to Web routes redirect to `/login` (`302 Found`).
   - Unauthenticated requests to REST routes are rejected or redirected to login.
4. **RFC 4180 CSV Escaping:**
   - All string outputs (descriptions, garage names, notes) containing commas, quotes, or newlines are escaped with double quotes to prevent CSV injection or row distortion.

---

## 6. Automated Testing & Verification

A dedicated suite with 26 automated tests was implemented in `VehicleReportAndExportModuleTest.java`:
- Web MVC rendering and model verification: **PASS**
- Ownership isolation and cross-user URL tampering: **PASS**
- Non-existent vehicle handling: **PASS**
- RFC 4180 CSV streaming and content header checks: **PASS**
- REST API payloads and HTTP response status: **PASS**
- RBAC rules and ADMIN isolation: **PASS**
- Full regression suite across 14 test classes: **264 / 264 PASS, 0 failures, 0 errors, 0 skipped, BUILD SUCCESS**.
