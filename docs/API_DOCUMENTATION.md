# REST API DOCUMENTATION — MyGarage (APPJFS19)

**System ID:** APPJFS19  
**Base URL:** `http://localhost:8080/api`  
**Authentication:** Spring Security Session Cookie (`JSESSIONID`)  
**Error Format:** RFC 7807 compatible JSON structure

---

## 1. Global Error Format

All error responses from `/api/**` return a structured JSON body with HTTP status codes:

```json
{
  "timestamp": "2026-09-03T17:30:59.991902800",
  "status": 400,
  "error": "Bad Request",
  "message": "Detailed error message"
}
```

---

## 2. API Endpoints Catalog

### A. Vehicle Management (`/api/vehicles`)
*Accessible by: `ROLE_NORMAL_USER`*

#### 1. List User Vehicles
- **Method:** `GET /api/vehicles`
- **Response:** `200 OK`
  ```json
  [
    {
      "vehicleId": 1,
      "plateNumber": "KA01MG1001",
      "make": "Honda",
      "model": "City",
      "year": 2023,
      "color": "White",
      "fuelType": "PETROL",
      "currentOdometer": 15000,
      "category": {
        "categoryId": 1,
        "name": "Sedan",
        "icon": "🚗"
      }
    }
  ]
  ```

#### 2. Get Vehicle by ID
- **Method:** `GET /api/vehicles/{id}`
- **Response:** `200 OK` (if owned by user), `403 Forbidden` (if owned by another user)

#### 3. Create Vehicle
- **Method:** `POST /api/vehicles`
- **Body:**
  ```json
  {
    "plateNumber": "KA01MG2002",
    "categoryId": 1,
    "make": "Hyundai",
    "model": "Verna",
    "year": 2024,
    "color": "Black",
    "fuelType": "PETROL",
    "currentOdometer": 5000
  }
  ```
- **Response:** `201 Created`

#### 4. Update Vehicle
- **Method:** `PUT /api/vehicles/{id}`
- **Response:** `200 OK`

#### 5. Delete Vehicle (Cascades to all child records)
- **Method:** `DELETE /api/vehicles/{id}`
- **Response:** `200 OK`

---

### B. Service Records (`/api/vehicles/{vehicleId}/services`, `/api/services/{id}`)
*Accessible by: `ROLE_NORMAL_USER`*

#### 1. List All Services for User (Cross-Vehicle)
- **Method:** `GET /api/services`
- **Query Params:** `search` (optional)
- **Response:** `200 OK` (list of services across all vehicles owned by user)

#### 2. List Services for Single Vehicle
- **Method:** `GET /api/vehicles/{vehicleId}/services`
- **Response:** `200 OK` (sorted by serviceDate descending)

#### 3. Get Service by ID
- **Method:** `GET /api/services/{id}`
- **Response:** `200 OK`

#### 4. Create Service Record
- **Method:** `POST /api/vehicles/{vehicleId}/services`
- **Body:**
  ```json
  {
    "serviceDate": "2026-08-15",
    "serviceType": "Full Service",
    "description": "Oil filter and synthetic oil change",
    "garageName": "City Motors",
    "cost": 4500.00,
    "odometerAtService": 15500,
    "nextServiceDueDate": "2027-02-15"
  }
  ```
- **Response:** `201 Created`

#### 5. Update Service Record
- **Method:** `PUT /api/services/{id}`
- **Response:** `200 OK`

#### 6. Delete Service Record
- **Method:** `DELETE /api/services/{id}`
- **Response:** `200 OK`

---

### C. Fuel Records (`/api/fuel`, `/api/vehicles/{vehicleId}/fuel`, `/api/fuel/{id}`)
*Accessible by: `ROLE_NORMAL_USER`*

#### 1. List All Fuel Records for User (Cross-Vehicle)
- **Method:** `GET /api/fuel`
- **Response:** `200 OK` (list of fuel records across all vehicles owned by user)

#### 2. List Fuel Records for Vehicle
- **Method:** `GET /api/vehicles/{vehicleId}/fuel`
- **Response:** `200 OK`

#### 3. Create Fuel Record
- **Method:** `POST /api/vehicles/{vehicleId}/fuel`
- **Body:**
  ```json
  {
    "fuelDate": "2026-08-20",
    "fuelType": "PETROL",
    "quantityLitres": 35.00,
    "costPerLitre": 100.00,
    "odometerAtFill": 16000
  }
  ```
- **Response:** `201 Created` *(Automatically computes total cost and estimated mileage)*

#### 4. Update Fuel Record
- **Method:** `PUT /api/fuel/{id}`
- **Response:** `200 OK`

#### 5. Delete Fuel Record
- **Method:** `DELETE /api/fuel/{id}`
- **Response:** `200 OK`

---

### D. Maintenance Tasks (`/api/maintenance`, `/api/vehicles/{vehicleId}/maintenance`, `/api/maintenance/{id}`)
*Accessible by: `ROLE_NORMAL_USER`*

#### 1. List All Maintenance Tasks for User (Cross-Vehicle)
- **Method:** `GET /api/maintenance`
- **Query Params:** `status` (optional, e.g. OVERDUE, DUE_TODAY, UPCOMING, COMPLETED)
- **Response:** `200 OK` (list of maintenance tasks across all vehicles owned by user)

#### 2. List Maintenance for Vehicle
- **Method:** `GET /api/vehicles/{vehicleId}/maintenance`
- **Response:** `200 OK`

#### 3. Create Maintenance Task
- **Method:** `POST /api/vehicles/{vehicleId}/maintenance`
- **Body:**
  ```json
  {
    "title": "Brake Fluid Flush",
    "description": "Replace DOT 4 brake fluid",
    "scheduledDate": "2026-09-15",
    "cost": 1200.00
  }
  ```
- **Response:** `201 Created` *(Status automatically set to UPCOMING, DUE_TODAY, or OVERDUE)*

#### 4. Mark Task Completed
- **Method:** `PATCH /api/vehicles/{vehicleId}/maintenance/{id}/complete`
- **Response:** `200 OK`

#### 5. Update Maintenance Task
- **Method:** `PUT /api/vehicles/{vehicleId}/maintenance/{id}`
- **Response:** `200 OK`

#### 6. Delete Maintenance Task
- **Method:** `DELETE /api/vehicles/{vehicleId}/maintenance/{id}`
- **Response:** `200 OK`

---

### E. Dashboard & Operational Analytics (`/api/dashboard/**`)
*Accessible by: `ROLE_NORMAL_USER` (Blocked for `ADMIN`)*

#### 1. Dashboard Overview
- **Method:** `GET /api/dashboard`
- **Response:** `200 OK`

#### 2. Dashboard Summary
- **Method:** `GET /api/dashboard/summary`
- **Response:** `200 OK`
  ```json
  {
    "vehicleCount": 2,
    "serviceCount": 5,
    "fuelCount": 8,
    "maintenanceCount": 3,
    "overdueCount": 1,
    "dueTodayCount": 0,
    "upcomingCount": 1,
    "completedCount": 1,
    "totalFuelCost": 12500.00,
    "totalServiceCost": 18200.00,
    "totalMaintenanceCost": 2400.00,
    "totalGarageCost": 33100.00,
    "averageMileage": 16.8
  }
  ```

#### 3. Active Maintenance Alerts
- **Method:** `GET /api/dashboard/alerts`
- **Response:** `200 OK` (Array of OVERDUE and DUE_TODAY tasks)

#### 4. Recent Fuel Fill-ups
- **Method:** `GET /api/dashboard/recent-fuel`
- **Response:** `200 OK` (Latest 5 records with mileage calculations)

---

### F. User Profile Management (`/api/profile/**`)
*Accessible by: `ROLE_NORMAL_USER` and `ROLE_ADMIN`*

#### 1. Get Current User Profile
- **Method:** `GET /api/profile`
- **Response:** `200 OK`
  ```json
  {
    "userId": 2,
    "fullName": "Alice Walker",
    "email": "alice@example.com",
    "phone": "9876543210",
    "role": "NORMAL_USER",
    "createdAt": "2026-09-01T10:00:00"
  }
  ```

#### 2. Update Profile
- **Method:** `PUT /api/profile`
- **Body:** `{"fullName": "Alice Walker", "phone": "9876543210"}`
- **Response:** `200 OK`

#### 3. Change Password
- **Method:** `POST /api/profile/change-password`
- **Body:** `{"currentPassword": "OldPassword@123", "newPassword": "NewPassword@456"}`
- **Response:** `200 OK`

---

### G. Vehicle Resale Dossiers & Data Export (`/api/reports/**`, `/api/vehicles/{vehicleId}/export/**`)
*Accessible by: `ROLE_NORMAL_USER` (Blocked for `ADMIN`)*

#### 1. Garage Portfolio Lifetime Summary
- **Method:** `GET /api/reports/garage-summary`
- **Response:** `200 OK`
  ```json
  {
    "totalVehicles": 2,
    "totalServices": 5,
    "totalFuelLogs": 12,
    "totalMaintenanceTasks": 4,
    "totalServiceCost": 12500.00,
    "totalFuelCost": 18200.00,
    "totalMaintenanceCost": 3400.00,
    "totalGarageCost": 34100.00,
    "averageMileage": 15.8,
    "vehicleBreakdown": [
      {
        "vehicleId": 1,
        "plateNumber": "TN01AB1234",
        "make": "Honda",
        "model": "City",
        "year": 2022,
        "category": "Sedan",
        "currentOdometer": 25000,
        "serviceCost": 7500.00,
        "fuelCost": 12000.00,
        "maintenanceCost": 1500.00,
        "totalCost": 21000.00,
        "avgMileage": 16.2,
        "totalRecords": 15
      }
    ]
  }
  ```

#### 2. Vehicle Resale Dossier JSON Payload
- **Method:** `GET /api/vehicles/{vehicleId}/export/summary`
- **Response:** `200 OK` (Full specifications, owner credentials, lifetime financials, and record lists)

#### 3. Vehicle Services CSV Stream
- **Method:** `GET /api/vehicles/{vehicleId}/export/services`
- **Response:** `200 OK` (`Content-Type: text/csv;charset=UTF-8`, RFC 4180 CSV)

#### 4. Vehicle Fuel Logs CSV Stream
- **Method:** `GET /api/vehicles/{vehicleId}/export/fuel`
- **Response:** `200 OK` (`Content-Type: text/csv;charset=UTF-8`)

#### 5. Vehicle Maintenance Tasks CSV Stream
- **Method:** `GET /api/vehicles/{vehicleId}/export/maintenance`
- **Response:** `200 OK` (`Content-Type: text/csv;charset=UTF-8`)

#### 6. Vehicle Master Unified History CSV Stream
- **Method:** `GET /api/vehicles/{vehicleId}/export/all`
- **Response:** `200 OK` (`Content-Type: text/csv;charset=UTF-8`, combines services, fuel, maintenance)

---

### H. System Administration (`/api/admin/**`)
*Accessible strictly by: `ROLE_ADMIN` (Blocked for `NORMAL_USER`)*

#### 1. System Platform Statistics
- **Method:** `GET /api/admin/statistics`
- **Response:** `200 OK`
  ```json
  {
    "totalUsers": 12,
    "totalVehicles": 24,
    "totalServiceRecords": 45,
    "totalFuelRecords": 82,
    "totalMaintenanceRecords": 19
  }
  ```

#### 2. List Registered Users
- **Method:** `GET /api/admin/users?search={optionalQuery}`
- **Response:** `200 OK`

#### 3. Toggle User Active Status
- **Method:** `POST /api/admin/users/{userId}/toggle`
- **Response:** `200 OK` *(Attempting to toggle Primary Admin account returns 400 Bad Request)*

#### 4. Category CRUD
- **List:** `GET /api/admin/categories`
- **Get by ID:** `GET /api/admin/categories/{id}`
- **Create:** `POST /api/admin/categories`
- **Update:** `PUT /api/admin/categories/{id}`
- **Delete:** `DELETE /api/admin/categories/{id}` *(Blocked with 400 Bad Request if vehicles are assigned)*

#### 5. Record Monitoring Summary
- **Method:** `GET /api/admin/records`
- **Response:** `200 OK`
  ```json
  {
    "totalServices": 45,
    "totalFuelLogs": 82,
    "totalMaintenanceTasks": 19,
    "categories": [
      {
        "id": 1,
        "name": "Sedan",
        "description": "Passenger car",
        "icon": "car-front",
        "vehicleCount": 12
      }
    ]
  }
  ```

---

### I. Comparative Analytics & Fleet Benchmarking (`/api/analytics/**`)
*Accessible by: `ROLE_NORMAL_USER` (Blocked for `ROLE_ADMIN` with 403 Forbidden)*

#### 1. Multi-Vehicle Comparative Report
- **Method:** `GET /api/analytics/compare?vehicleIds=1,2,3`
- **Params:** `vehicleIds` (comma-separated or repeated parameter, 2 to 4 IDs required)
- **Response:** `200 OK`
  ```json
  {
    "comparedVehicleCount": 2,
    "fleetTotalSpend": 10300.00,
    "fleetAverageMileage": 22.5,
    "fleetAverageRunningCostPerKm": 0.40,
    "mostEfficientVehiclePlate": "TN13A1001",
    "mostEconomicalVehiclePlate": "TN13B1002",
    "lowestMaintenanceVehiclePlate": "TN13B1002",
    "fleetWorkhorsePlate": "TN13A1001",
    "vehicles": [
      {
        "vehicleId": 1,
        "plateNumber": "TN13A1001",
        "make": "Honda",
        "model": "City",
        "year": 2022,
        "color": "Silver",
        "categoryName": "Sedan",
        "fuelType": "PETROL",
        "currentOdometer": 20000,
        "serviceCost": 5000.00,
        "fuelCost": 2000.00,
        "maintenanceCost": 1000.00,
        "totalOwnershipCost": 8000.00,
        "avgMileageKmpl": 18.5,
        "totalFuelLitres": 20.00,
        "runningCostPerKm": 0.40,
        "fuelCostPerKm": 0.10,
        "totalServicesCount": 1,
        "totalFuelLogsCount": 1,
        "totalMaintenanceTasksCount": 1,
        "maintenanceCompletedCount": 1,
        "maintenanceOverdueCount": 0,
        "maintenanceCompletionRate": 100.0,
        "mostFuelEfficient": false,
        "lowestCostPerKm": true,
        "lowestMaintenanceCost": false,
        "fleetWorkhorse": true
      }
    ]
  }
  ```
- **Error Responses:** `400 Bad Request` (< 2 or > 4 vehicles), `403 Forbidden` (cross-user vehicle tampering or admin access)

#### 2. Head-to-Head Pairwise Vehicle Comparison
- **Method:** `GET /api/vehicles/{id1}/compare/{id2}`
- **Response:** `200 OK` (Structured `FleetComparisonReportDTO` for the two vehicles)

#### 3. Single Vehicle Operational Analytics & Running Cost
- **Method:** `GET /api/vehicles/{id}/analytics`
- **Response:** `200 OK` (Detailed `VehicleComparisonDTO` with running cost per km, fuel cost per km, and completion rate)

#### 4. Garage-Wide Fleet Expense Breakdown
- **Method:** `GET /api/analytics/fleet-breakdown`
- **Response:** `200 OK`
  ```json
  {
    "totalVehicles": 2,
    "totalFleetSpend": 10300.00,
    "serviceCostPercentage": 63.1,
    "fuelCostPercentage": 27.2,
    "maintenanceCostPercentage": 9.7,
    "vehicleShares": [
      {
        "vehicleId": 1,
        "plateNumber": "TN13A1001",
        "make": "Honda",
        "model": "City",
        "totalSpend": 8000.00,
        "percentageOfFleetSpend": 77.7
      }
    ]
  }
  ```

---

### J. Predictive Maintenance & Vehicle Health APIs (`/api/vehicles/{id}/forecast`, `/api/analytics/garage-forecast`)
*Accessible by: `ROLE_NORMAL_USER` (Blocked for `ROLE_ADMIN` with 403 Forbidden)*

#### 1. Vehicle Predictive Maintenance & Health Report
- **Method:** `GET /api/vehicles/{id}/forecast`
- **Response:** `200 OK`
  ```json
  {
    "vehicleId": 1,
    "plateNumber": "TN14A1001",
    "make": "Honda",
    "model": "City",
    "year": 2022,
    "currentOdometer": 14500,
    "estimatedDailyKm": 45.0,
    "velocityConfidence": "HIGH",
    "historicalDataPoints": 3,
    "healthIndex": {
      "score": 85,
      "grade": "EXCELLENT",
      "statusDescription": "Vehicle is in prime mechanical shape.",
      "overdueDeductions": 0,
      "serviceRecencyScore": 10,
      "mileageFactorScore": 0,
      "completionRateScore": 10
    },
    "milestones": [
      {
        "milestoneId": "PMS_5000_15000",
        "title": "Minor Service / Inspection",
        "description": "Engine oil check, air filter cleaning, tyre rotation, and safety inspections.",
        "intervalKm": 5000,
        "dueOdometer": 15000,
        "remainingKm": 500,
        "estimatedDaysRemaining": 11,
        "projectedDueDate": "2026-09-15",
        "estimatedCost": 1500.00,
        "urgencyLevel": "SOON",
        "alreadyScheduled": false
      }
    ],
    "expenseForecast": {
      "forecast30Days": 1500.00,
      "forecast60Days": 5000.00,
      "forecast90Days": 5000.00,
      "forecast180Days": 12500.00
    }
  }
  ```
- **Error Responses:** `403 Forbidden` (cross-user tampering or admin access), `404 Not Found` (vehicle does not exist)

#### 2. Garage-Wide Fleet Health Summary
- **Method:** `GET /api/analytics/garage-forecast`
- **Response:** `200 OK`
  ```json
  [
    {
      "vehicleId": 1,
      "plateNumber": "TN14A1001",
      "make": "Honda",
      "model": "City",
      "currentOdometer": 14500,
      "estimatedDailyKm": 45.0,
      "healthIndex": { "score": 85, "grade": "EXCELLENT" },
      "expenseForecast": { "forecast30Days": 1500.00, "forecast90Days": 5000.00 }
    }
  ]
  ```

#### 3. Schedule Projected Forecast Milestone
- **Method:** `POST /api/vehicles/{id}/forecast/schedule`
- **Body:**
  ```json
  {
    "title": "Minor Service / Inspection",
    "description": "Engine oil check, air filter cleaning, tyre rotation",
    "scheduledDate": "2026-09-15",
    "cost": 1500.00
  }
  ```
- **Response:** `201 Created` (returns created `MaintenanceRecordDTO`)
- **Error Responses:** `400 Bad Request` (validation error), `403 Forbidden` (cross-user tampering), `409 Conflict` (milestone already scheduled and pending)

---

### K. Total Cost of Ownership (TCO) & Asset Advisory APIs (`/api/vehicles/{id}/tco`, `/api/analytics/garage-tco`)
*Accessible by: `ROLE_NORMAL_USER` (Blocked for `ROLE_ADMIN` with 403 Forbidden)*

#### 1. Vehicle TCO, Depreciation & Replacement Advisory Report
- **Method:** `GET /api/vehicles/{id}/tco`
- **Response:** `200 OK`
  ```json
  {
    "vehicleId": 1,
    "plateNumber": "TN15A1001",
    "make": "Honda",
    "model": "City",
    "year": 2022,
    "category": "Sedan",
    "currentOdometer": 15000,
    "vehicleAgeYears": 4,
    "cumulativeFuelCost": 12000.00,
    "cumulativeServiceCost": 8000.00,
    "cumulativeMaintenanceCost": 3500.00,
    "totalOpex": 23500.00,
    "estimatedOriginalMSRP": 1500000.00,
    "estimatedCurrentResidualValue": 786000.00,
    "cumulativeDepreciation": 714000.00,
    "lifecycleTco": 737500.00,
    "opexPerKm": 1.57,
    "tcoPerKm": 49.17,
    "annualizedOpex": 5875.00,
    "monthlyOpexRunRate": 489.58,
    "trailing12MonthsMaintenanceCost": 3500.00,
    "repairToResidualValueRatio": 0.45,
    "replacementAdvisoryStatus": "OPTIMAL_RETENTION",
    "replacementAdvisoryRationale": "Vehicle demonstrates strong economic health. Trailing maintenance is comfortably below residual value thresholds.",
    "fuelType": "PETROL",
    "totalFuelLitres": 200.00,
    "estimatedTailpipeCo2Kg": 462.00,
    "tailpipeCo2GramsPerKm": 30.80,
    "financialDisclaimer": "Note: Asset values and depreciation figures are econometric models calibrated against industry benchmark parameters and do not represent guaranteed cash appraisal values.",
    "carbonDisclaimer": "Note: Carbon emission calculations represent direct tailpipe combustion estimates (Tank-to-Wheel) and do not include upstream well-to-tank fuel production lifecycle emissions."
  }
  ```
- **Error Responses:** `403 Forbidden` (cross-user tampering or admin access), `404 Not Found` (vehicle not found)

#### 2. Garage-Wide TCO Portfolio Summary
- **Method:** `GET /api/analytics/garage-tco`
- **Response:** `200 OK`
  ```json
  {
    "totalVehicles": 2,
    "totalFleetOpex": 35000.00,
    "totalFleetFuelCost": 18000.00,
    "totalFleetServiceCost": 12000.00,
    "totalFleetMaintenanceCost": 5000.00,
    "totalEstimatedPortfolioValue": 1450000.00,
    "totalEstimatedPortfolioMSRP": 2700000.00,
    "totalCumulativeDepreciation": 1250000.00,
    "totalFleetLifecycleTco": 1285000.00,
    "totalFleetTailpipeCo2Kg": 693.00,
    "averageOpexPerKm": 1.45,
    "vehicles": [
      {
        "vehicleId": 1,
        "plateNumber": "TN15A1001",
        "make": "Honda",
        "model": "City",
        "totalOpex": 23500.00,
        "estimatedCurrentResidualValue": 786000.00,
        "lifecycleTco": 737500.00,
        "opexPerKm": 1.57,
        "repairToResidualValueRatio": 0.45,
        "replacementAdvisoryStatus": "OPTIMAL_RETENTION"
      }
    ]
  }
  ```
- **Error Responses:** `403 Forbidden` (admin access)

---

### L. Fuel Efficiency Intelligence & Historical Price Analytics APIs (M16)

#### 1. Vehicle Fuel Analytics Report
- **Method:** `GET /api/vehicles/{id}/fuel-analytics`
- **Access:** `ROLE_NORMAL_USER` (Vehicle Owner only)
- **Response:** `200 OK`
  ```json
  {
    "vehicleId": 1,
    "plateNumber": "TN01-AB-1234",
    "make": "Honda",
    "model": "City",
    "fuelType": "PETROL",
    "currentOdometer": 15000,
    "lifetimeFuelCost": 12500.00,
    "lifetimeLiters": 125.00,
    "lifetimeAvgMileageKmpl": 14.8,
    "bestMileageKmpl": 17.5,
    "worstMileageKmpl": 12.2,
    "mileageStdDev": 1.45,
    "rolling30DayAvgKmpl": 15.2,
    "rolling90DayAvgKmpl": 14.9,
    "monthlyBreakdowns": [
      {
        "yearMonth": "2025-08",
        "totalVolumeLiters": 45.00,
        "totalCost": 4500.00,
        "fillUpCount": 2,
        "averagePricePerLiter": 100.00
      }
    ]
  }
  ```
- **Error Responses:** `403 Forbidden` (cross-user tampering or admin access)

#### 2. Garage-Wide Fleet Fuel Intelligence
- **Method:** `GET /api/analytics/garage-fuel`
- **Access:** `ROLE_NORMAL_USER`
- **Response:** `200 OK`
  ```json
  {
    "garageTotalFuelCost": 25000.00,
    "garageTotalLiters": 250.00,
    "garageAverageKmpl": 15.1,
    "vehicles": [
      {
        "vehicleId": 1,
        "plateNumber": "TN01-AB-1234",
        "make": "Honda",
        "model": "City",
        "averageKmpl": 14.8,
        "efficiencyBadge": "ECO_CHAMPION"
      }
    ]
  }
  ```

---

### M. Vehicle Reliability Engineering, Component Failure Risk & Chronic Defect APIs (M17)

#### 1. Vehicle Reliability Engineering Report
- **Method:** `GET /api/vehicles/{id}/reliability`
- **Access:** `ROLE_NORMAL_USER` (Vehicle Owner only)
- **Response:** `200 OK`
  ```json
  {
    "vehicleId": 1,
    "plateNumber": "TN07-RELIAB01",
    "make": "Toyota",
    "model": "Innova",
    "year": 2020,
    "currentOdometer": 45000,
    "vriScore": 92,
    "reliabilityGrade": "EXCELLENT",
    "totalServiceVisits": 4,
    "totalServiceSpend": 18500.00,
    "mdbfKm": 11250.0,
    "mtbsDays": 120.5,
    "unscheduledBreakdownCount": 1,
    "unscheduledBreakdownSpend": 4500.00,
    "correctiveServiceRatio": 24.3,
    "routineMaintenanceCount": 3,
    "routineMaintenanceSpend": 14000.00,
    "subsystemBreakdowns": [
      {
        "subsystem": "BRAKING_TIRES",
        "displayName": "Braking & Tires",
        "recordCount": 1,
        "totalCost": 4500.00,
        "spendPercentage": 24.3,
        "lastRepairDate": "2025-08-15"
      },
      {
        "subsystem": "POWERTRAIN_ENGINE",
        "displayName": "Powertrain & Engine",
        "recordCount": 3,
        "totalCost": 14000.00,
        "spendPercentage": 75.7,
        "lastRepairDate": "2025-06-10"
      }
    ],
    "chronicDefectAlerts": [],
    "workshops": [
      {
        "workshopName": "Toyota Authorized Dealership",
        "visitCount": 3,
        "totalSpend": 14000.00,
        "averageCostPerVisit": 4666.67,
        "meanReturnIntervalDays": 130.0
      }
    ],
    "serviceAccelerationStatus": "STABLE",
    "disclaimer": "Reliability classification, subsystem identification, chronic-defect detection, and VRI are deterministic rule-based analytical models derived from existing service and maintenance records, not OEM diagnostic or sensor-confirmed failure data."
  }
  ```
- **Error Responses:** `403 Forbidden` (cross-user tampering or admin access)

#### 2. Garage-Wide Fleet Reliability Matrix
- **Method:** `GET /api/analytics/garage-reliability`
- **Access:** `ROLE_NORMAL_USER`
- **Response:** `200 OK`
  ```json
  {
    "averageGarageVri": 88,
    "fleetReliabilityGrade": "GOOD",
    "fleetMdbfKm": 9800.5,
    "fleetTotalBreakdownSpend": 12000.00,
    "fleetTotalServiceVisits": 8,
    "mostReliableVehicle": {
      "vehicleId": 1,
      "plateNumber": "TN07-RELIAB01",
      "make": "Toyota",
      "model": "Innova",
      "year": 2020,
      "vriScore": 92,
      "reliabilityGrade": "EXCELLENT",
      "totalVisits": 4,
      "mdbfKm": 11250.0,
      "breakdownRisk": "LOW"
    },
    "highestRiskVehicle": {
      "vehicleId": 2,
      "plateNumber": "TN07-OTHER99",
      "make": "Hyundai",
      "model": "Verna",
      "year": 2021,
      "vriScore": 65,
      "reliabilityGrade": "MODERATE",
      "totalVisits": 4,
- **Error Responses:** `403 Forbidden` (admin access)

---

### L. Fuel Efficiency Intelligence & Historical Price Analytics APIs (M16)

#### 1. Vehicle Fuel Analytics Report
- **Method:** `GET /api/vehicles/{id}/fuel-analytics`
- **Access:** `ROLE_NORMAL_USER` (Vehicle Owner only)
- **Response:** `200 OK`
  ```json
  {
    "vehicleId": 1,
    "plateNumber": "TN01-AB-1234",
    "make": "Honda",
    "model": "City",
    "fuelType": "PETROL",
    "currentOdometer": 15000,
    "lifetimeFuelCost": 12500.00,
    "lifetimeLiters": 125.00,
    "lifetimeAvgMileageKmpl": 14.8,
    "bestMileageKmpl": 17.5,
    "worstMileageKmpl": 12.2,
    "mileageStdDev": 1.45,
    "rolling30DayAvgKmpl": 15.2,
    "rolling90DayAvgKmpl": 14.9,
    "monthlyBreakdowns": [
      {
        "yearMonth": "2025-08",
        "totalVolumeLiters": 45.00,
        "totalCost": 4500.00,
        "fillUpCount": 2,
        "averagePricePerLiter": 100.00
      }
    ]
  }
  ```
- **Error Responses:** `403 Forbidden` (cross-user tampering or admin access)

#### 2. Garage-Wide Fleet Fuel Intelligence
- **Method:** `GET /api/analytics/garage-fuel`
- **Access:** `ROLE_NORMAL_USER`
- **Response:** `200 OK`
  ```json
  {
    "garageTotalFuelCost": 25000.00,
    "garageTotalLiters": 250.00,
    "garageAverageKmpl": 15.1,
    "vehicles": [
      {
        "vehicleId": 1,
        "plateNumber": "TN01-AB-1234",
        "make": "Honda",
        "model": "City",
        "averageKmpl": 14.8,
        "efficiencyBadge": "ECO_CHAMPION"
      }
    ]
  }
  ```

---

### M. Vehicle Reliability Engineering, Component Failure Risk & Chronic Defect APIs (M17)

#### 1. Vehicle Reliability Engineering Report
- **Method:** `GET /api/vehicles/{id}/reliability`
- **Access:** `ROLE_NORMAL_USER` (Vehicle Owner only)
- **Response:** `200 OK`
  ```json
  {
    "vehicleId": 1,
    "plateNumber": "TN07-RELIAB01",
    "make": "Toyota",
    "model": "Innova",
    "year": 2020,
    "currentOdometer": 45000,
    "vriScore": 92,
    "reliabilityGrade": "EXCELLENT",
    "totalServiceVisits": 4,
    "totalServiceSpend": 18500.00,
    "mdbfKm": 11250.0,
    "mtbsDays": 120.5,
    "unscheduledBreakdownCount": 1,
    "unscheduledBreakdownSpend": 4500.00,
    "correctiveServiceRatio": 24.3,
    "routineMaintenanceCount": 3,
    "routineMaintenanceSpend": 14000.00,
    "subsystemBreakdowns": [
      {
        "subsystem": "BRAKING_TIRES",
        "displayName": "Braking & Tires",
        "recordCount": 1,
        "totalCost": 4500.00,
        "spendPercentage": 24.3,
        "lastRepairDate": "2025-08-15"
      },
      {
        "subsystem": "POWERTRAIN_ENGINE",
        "displayName": "Powertrain & Engine",
        "recordCount": 3,
        "totalCost": 14000.00,
        "spendPercentage": 75.7,
        "lastRepairDate": "2025-06-10"
      }
    ],
    "chronicDefectAlerts": [],
    "workshops": [
      {
        "workshopName": "Toyota Authorized Dealership",
        "visitCount": 3,
        "totalSpend": 14000.00,
        "averageCostPerVisit": 4666.67,
        "meanReturnIntervalDays": 130.0
      }
    ],
    "serviceAccelerationStatus": "STABLE",
    "disclaimer": "Reliability classification, subsystem identification, chronic-defect detection, and VRI are deterministic rule-based analytical models derived from existing service and maintenance records, not OEM diagnostic or sensor-confirmed failure data."
  }
  ```
- **Error Responses:** `403 Forbidden` (cross-user tampering or admin access)

#### 2. Garage-Wide Fleet Reliability Matrix
- **Method:** `GET /api/analytics/garage-reliability`
- **Access:** `ROLE_NORMAL_USER`
- **Response:** `200 OK`
  ```json
  {
    "averageGarageVri": 88,
    "fleetReliabilityGrade": "GOOD",
    "fleetMdbfKm": 9800.5,
    "fleetTotalBreakdownSpend": 12000.00,
    "fleetTotalServiceVisits": 8,
    "mostReliableVehicle": {
      "vehicleId": 1,
      "plateNumber": "TN07-RELIAB01",
      "make": "Toyota",
      "model": "Innova",
      "year": 2020,
      "vriScore": 92,
      "reliabilityGrade": "EXCELLENT",
      "totalVisits": 4,
      "mdbfKm": 11250.0,
      "breakdownRisk": "LOW"
    },
    "highestRiskVehicle": {
      "vehicleId": 2,
      "plateNumber": "TN07-OTHER99",
      "make": "Hyundai",
      "model": "Verna",
      "year": 2021,
      "vriScore": 65,
      "reliabilityGrade": "MODERATE",
      "totalVisits": 4,
      "mdbfKm": 6200.0,
      "breakdownRisk": "MODERATE"
    },
    "vehicleSummaries": [ ... ],
    "subsystemSpendDistribution": [ ... ],
    "recommendations": [
      "Overall fleet reliability is in a healthy operating window. Continue scheduled preventive servicing."
    ],
    "disclaimer": "Reliability classification, subsystem identification, chronic-defect detection, and VRI are deterministic rule-based analytical models derived from existing service and maintenance records, not OEM diagnostic or sensor-confirmed failure data."
  }
  ```
- **Error Responses:** `403 Forbidden` (admin access)

---

### N. Vehicle Operational Readiness, Journey Risk & Fleet Mission Dispatch APIs (M18)

#### 1. Vehicle Trip Readiness & Journey Simulation Report
- **Method:** `GET /api/vehicles/{id}/readiness?tripDistanceKm={distance}&tripDays={days}&drivingRegime={regime}`
- **Access:** `ROLE_NORMAL_USER` (Vehicle Owner only; Admin blocked with 403 Forbidden)
- **Parameters:**
  - `tripDistanceKm` (optional, default: `500.0`, range: `10.0` to `10000.0`)
  - `tripDays` (optional, default: `2`, range: `1` to `30`)
  - `drivingRegime` (optional, default: `HIGHWAY_CRUISE`, values: `HIGHWAY_CRUISE`, `MIXED_BALANCED`, `CITY_CONGESTED`, `MOUNTAIN_SEVERE`)
- **Response:** `200 OK`
  ```json
  {
    "vehicleId": 1,
    "vehicleName": "2021 Toyota Camry",
    "licensePlate": "TN09-READY01",
    "categoryName": "Sedan",
    "currentMileage": 30000,
    "tripDistanceKm": 600.0,
    "tripDays": 2,
    "drivingRegime": "HIGHWAY_CRUISE",
    "postTripProjectedMileage": 30600.0,
    "tripReadinessIndex": 92,
    "readinessBand": "MISSION_READY",
    "readinessSummary": "Vehicle demonstrates high operational readiness for the planned journey. Consumable margins are healthy.",
    "hasMidTripBreach": false,
    "midTripBreachAlerts": [],
    "consumableMargins": [
      {
        "subsystemName": "Engine Oil & Filter",
        "benchmarkIntervalKm": 10000,
        "benchmarkIntervalDays": 180,
        "kmSinceLastService": 2000,
        "daysSinceLastService": 30,
        "remainingMarginKm": 8000,
        "remainingMarginPercent": 80.0,
        "postTripMarginKm": 7400,
        "willBreachMidTrip": false,
        "breachAtTripKm": null,
        "statusBand": "OPTIMAL"
      }
    ],
    "fuelStaging": {
      "tripDistanceKm": 600.0,
      "estimatedConsumptionPer100Km": 7.0,
      "estimatedFuelNeededLiters": 42.0,
      "estimatedFuelCost": 63.00,
      "estimatedCruisingRangeKm": 714.0,
      "estimatedFuelStopsRequired": 0,
      "fuelType": "PETROL",
      "regimeAdjustmentDescription": "Highway Cruise (0.90x consumption factor)",
      "isHistoricalDataAvailable": true
    },
    "checklist": [
      {
        "category": "CONSUMABLE",
        "title": "Engine Oil & Filter Verified",
        "description": "Sufficient reserve margin (8000 km available; 7400 km after trip).",
        "severity": "PASSED",
        "isActionRequired": false
      }
    ],
    "criticalActionCount": 0,
    "advisoryCount": 0,
    "passedCount": 4,
    "analyticalDisclaimer": "The Trip Readiness Index (TRI) is an analytical readiness estimate based on recorded vehicle history, deterministic software rules, and benchmark model assumptions. It is NOT an engineering or mechanical safety guarantee, and does not guarantee that the vehicle will safely complete any journey. Always conduct a thorough physical pre-trip inspection.",
    "benchmarkAssumptionsNote": "Benchmark maintenance intervals are configurable model assumptions (Engine Oil: 10,000 km / 180 days; Brakes: 20,000 km / 365 days; Coolant: 40,000 km / 730 days; Tires/Suspension: 30,000 km / 540 days), not universal OEM manufacturer specifications. Missing historical records produce conservative estimates.",
    "hasHistoricalRecords": true
  }
  ```
- **Error Responses:** `403 Forbidden` (cross-user tampering or admin access)

#### 2. Garage-Wide Fleet Mission Dispatch Report
- **Method:** `GET /api/analytics/garage-dispatch?tripDistanceKm={distance}&tripDays={days}&drivingRegime={regime}`
- **Access:** `ROLE_NORMAL_USER` (Admin blocked with 403 Forbidden)
- **Response:** `200 OK`
  ```json
  {
    "tripDistanceKm": 600.0,
    "tripDays": 2,
    "drivingRegime": "HIGHWAY_CRUISE",
    "optimalVehicleId": 1,
    "optimalVehicleName": "2021 Toyota Camry",
    "dispatchRecommendationSummary": "Optimal vehicle selected: 2021 Toyota Camry with 92% readiness index.",
    "candidates": [
      {
        "vehicleId": 1,
        "vehicleName": "2021 Toyota Camry",
        "licensePlate": "TN09-READY01",
        "categoryName": "Sedan",
        "currentMileage": 30000,
        "tripReadinessIndex": 92,
        "readinessBand": "MISSION_READY",
        "estimatedTripFuelCost": 63.00,
        "estimatedFuelNeededLiters": 42.0,
        "criticalIssuesCount": 0,
        "hasMidTripBreach": false,
        "dispatchRecommendation": "OPTIMAL_CHOICE",
        "recommendationRationale": "Recommended dispatch vehicle: Highest readiness index (92%) with $63.00 projected fuel cost."
      }
    ],
    "totalActiveVehiclesEvaluated": 1,
    "missionReadyVehiclesCount": 1,
    "analyticalDisclaimer": "The Trip Readiness Index (TRI) is an analytical readiness estimate based on recorded vehicle history, deterministic software rules, and benchmark model assumptions. It is NOT an engineering or mechanical safety guarantee, and does not guarantee that the vehicle will safely complete any journey. Always conduct a thorough physical pre-trip inspection."
  }
  ```
- **Error Responses:** `403 Forbidden` (admin access)

---

### T. Milestone 19 — Vehicle Maintenance Deficit Index & Deferred Backlog Debt Engine (`/api/vehicles/{id}/maintenance-deficit`, `/api/analytics/garage-maintenance-deficit`)
*Accessible by: Authenticated Vehicle Owner (`ROLE_NORMAL_USER`)*  
*Two-Tier Security: Cross-user access returns `403 Forbidden`; `ROLE_ADMIN` returns `403 Forbidden`*

#### 1. Vehicle Maintenance Deficit Report
- **Method:** `GET /api/vehicles/{id}/maintenance-deficit`
- **Access:** `ROLE_NORMAL_USER` (Vehicle Owner only)
- **Response:** `200 OK`
  ```json
  {
    "vehicleId": 1,
    "plateNumber": "TN09-MDI01",
    "make": "Honda",
    "model": "Civic",
    "year": 2022,
    "currentOdometer": 15000,
    "replacementAssetValue": 19125.00,
    "deferredMaintenanceDebt": 95.00,
    "maintenanceDeficitIndex": 0.50,
    "deficitStatus": "OPTIMAL",
    "compoundNeglectCostExposure": 522.50,
    "inactionMultiplier": 5.50,
    "totalNearTermExposure": 110.00,
    "total30DayMaintenanceLiability": 205.00,
    "backlogItemCount": 1,
    "nearTermItemCount": 1,
    "backlogItems": [
      {
        "id": "BREACH-ENGINE_OIL_AND_FILTER",
        "subsystem": "Engine Oil & Filter",
        "taskTitle": "Engine Oil & Filter Interval Exhausted",
        "originType": "BREACHED_INTERVAL",
        "daysOverdue": 20,
        "mileageOverdueKm": 5000.0,
        "directRemediationCost": 95.00,
        "neglectCascadeMultiplier": 5.5,
        "compoundNeglectCostExposure": 522.50,
        "primaryConsequence": "Severe oil sludge, camshaft scoring, turbocharger bearing failure, or catastrophic engine seizure",
        "secondaryConsequences": [
          "Turbocharger bearing oil starvation",
          "Timing chain tensioner failure",
          "Piston ring carbon packing and blow-by"
        ],
        "riskMitigationEfficiency": 7.37,
        "priority": "URGENT_REMEDIATION",
        "sourceDescription": "Historical logbook confirms interval exhaustion without scheduled remediation."
      }
    ],
    "nearTermItems": [
      {
        "id": "NEAR-1",
        "subsystem": "General Maintenance & Inspection",
        "taskTitle": "Spark Plug Replacement",
        "daysUntilDue": 15,
        "dueOdometerKm": null,
        "projectedCost": 110.00,
        "status": "UPCOMING_SCHEDULED",
        "advisoryNote": "Scheduled on 2026-09-20 (15 days remaining). Budgetary projection."
      }
    ],
    "triageRoadmap": [
      {
        "triageRank": 1,
        "id": "BREACH-ENGINE_OIL_AND_FILTER",
        "taskTitle": "Engine Oil & Filter Interval Exhausted",
        "subsystem": "Engine Oil & Filter",
        "immediateCost": 95.00,
        "preventedExposure": 522.50,
        "netSavings": 427.50,
        "rmeScore": 7.37,
        "triageAction": "Immediate Remediation Required: Book technician service for Engine Oil & Filter",
        "urgencyRationale": "Immediate action avoids 5.5x cascade exposure; net savings of $427.50."
      }
    ],
    "executiveSummary": "Honda Civic (TN09-MDI01) operates within OPTIMAL MDI limits (0.5%) with $95.00 in deferred maintenance debt.",
    "analyticalDisclaimer": "The Maintenance Deficit Index (MDI) and Compound Cascade Multipliers are MyGarage analytical modeling heuristics derived from historical vehicle logs and configurable benchmark cost assumptions. They do not represent official ISO 55000 / NASA metrics or manufacturer-certified engineering warranties, nor do they guarantee that secondary mechanical damage will occur within a specific timeline. Always consult a certified mechanic.",
    "benchmarkPolicyNote": "Benchmark maintenance costs and intervals are authoritative configurable model assumptions (Engine Oil: $95.00, 10,000 km / 180 days; Brakes: $180.00, 20,000 km / 365 days; Coolant: $140.00, 40,000 km / 730 days; Tires/Suspension: $160.00, 30,000 km / 540 days; Major PMS: $320.00, 40,000 km / 730 days; General Maintenance fallback: $110.00, 15,000 km / 365 days). Recorded costs take precedence when present and positive; unpriced or negative costs strictly fallback to these domain benchmarks."
  }
  ```
- **Error Responses:** `403 Forbidden` (cross-user tampering or admin access)

#### 2. Garage Fleet Maintenance Deficit Matrix
- **Method:** `GET /api/analytics/garage-maintenance-deficit`
- **Access:** `ROLE_NORMAL_USER` (Admin blocked with 403 Forbidden)
- **Response:** `200 OK`
  ```json
  {
    "totalFleetRAV": 38250.00,
    "totalFleetDeferredDebt": 95.00,
    "garageFleetMDI": 0.25,
    "fleetDeficitStatus": "OPTIMAL",
    "totalCompoundExposure": 522.50,
    "totalNearTermExposure": 110.00,
    "totalFleet30DayLiability": 205.00,
    "totalVehicles": 2,
    "criticalVehiclesCount": 0,
    "deficientVehiclesCount": 0,
    "fairVehiclesCount": 0,
    "optimalVehiclesCount": 2,
    "vehicleSummaries": [
      {
        "vehicleId": 1,
        "plateNumber": "TN09-MDI01",
        "make": "Honda",
        "model": "Civic",
        "year": 2022,
        "currentOdometer": 15000,
        "rav": 19125.00,
        "deferredDebt": 95.00,
        "mdi": 0.50,
        "deficitStatus": "OPTIMAL",
        "compoundExposure": 522.50,
        "nearTermExposure": 110.00,
        "total30DayLiability": 205.00,
        "backlogCount": 1,
        "topTriageAction": "Immediate Remediation Required: Book technician service for Engine Oil & Filter"
      }
    ],
    "priorityTriageQueue": [],
    "fleetExecutiveAdvisory": "Fleet maintenance posture is OPTIMAL. Backlog liabilities and compound risks are under active control.",
    "analyticalDisclaimer": "The Maintenance Deficit Index (MDI) and Compound Cascade Multipliers are MyGarage analytical modeling heuristics...",
    "benchmarkPolicyNote": "Benchmark maintenance costs and intervals are authoritative configurable model assumptions..."
  }
  ```
- **Error Responses:** `403 Forbidden` (admin access)
