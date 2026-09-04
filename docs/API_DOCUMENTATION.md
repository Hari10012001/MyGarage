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