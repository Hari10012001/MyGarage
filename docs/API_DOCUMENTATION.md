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

#### 1. List Services for Vehicle
- **Method:** `GET /api/vehicles/{vehicleId}/services`
- **Response:** `200 OK` (sorted by serviceDate descending)

#### 2. Get Service by ID
- **Method:** `GET /api/services/{id}`
- **Response:** `200 OK`

#### 3. Create Service Record
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

#### 4. Update Service Record
- **Method:** `PUT /api/services/{id}`
- **Response:** `200 OK`

#### 5. Delete Service Record
- **Method:** `DELETE /api/services/{id}`
- **Response:** `200 OK`

---

### C. Fuel Records (`/api/vehicles/{vehicleId}/fuel`, `/api/fuel/{id}`)
*Accessible by: `ROLE_NORMAL_USER`*

#### 1. List Fuel Records for Vehicle
- **Method:** `GET /api/vehicles/{vehicleId}/fuel`
- **Response:** `200 OK`

#### 2. Create Fuel Record
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

#### 3. Update Fuel Record
- **Method:** `PUT /api/fuel/{id}`
- **Response:** `200 OK`

#### 4. Delete Fuel Record
- **Method:** `DELETE /api/fuel/{id}`
- **Response:** `200 OK`

---

### D. Maintenance Tasks (`/api/vehicles/{vehicleId}/maintenance`, `/api/maintenance/{id}`)
*Accessible by: `ROLE_NORMAL_USER`*

#### 1. List Maintenance for Vehicle
- **Method:** `GET /api/vehicles/{vehicleId}/maintenance`
- **Response:** `200 OK`

#### 2. Create Maintenance Task
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

#### 3. Mark Task Completed
- **Method:** `PATCH /api/vehicles/{vehicleId}/maintenance/{id}/complete`
- **Response:** `200 OK`

#### 4. Update Maintenance Task
- **Method:** `PUT /api/vehicles/{vehicleId}/maintenance/{id}`
- **Response:** `200 OK`

#### 5. Delete Maintenance Task
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

### G. System Administration (`/api/admin/**`)
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