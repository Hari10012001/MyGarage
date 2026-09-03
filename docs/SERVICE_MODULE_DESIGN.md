# SERVICE MODULE DESIGN & SPECIFICATION — MyGarage (APPJFS19)

**Project Name:** MyGarage — A Vehicle Service History, Fuel Record and Maintenance Tracking Platform  
**System ID:** APPJFS19  
**Module:** Milestone 5 (M5) — Service Module  
**Author:** Senior Full Stack Architect & Engineering Team  
**Last Updated:** 2026-09-03  
**Status:** **100% IMPLEMENTED, TESTED & VERIFIED**

---

## 1. Module Overview & Architecture

The Service Module enables vehicle owners to log, monitor, update, and review the full maintenance and service history for every vehicle in their garage. It implements strict two-tier ownership guarding and historical ordering.

### Key Architectural Pillars:
1. **Strict Two-Tier Ownership Hierarchy:**
   ```
   Authenticated User (NORMAL_USER)
      ↓ owns
   Vehicle (findByVehicleIdAndUserUserId)
      ↓ owns
   ServiceRecord (findByServiceIdAndVehicleUserUserId)
   ```
   A user can never view, add, edit, or delete a service record for a vehicle they do not own. URL tampering across mismatched vehicles or users is intercepted and blocked at the service layer.
2. **Chronological Service Ordering:** All service records are retrieved in descending order of service date (`serviceDate DESC`), ensuring the latest service visits always appear first.
3. **Automatic Odometer Progression:** When logging or editing a service record with an odometer reading higher than the vehicle's current odometer, the vehicle's `currentOdometer` is automatically updated.
4. **Cascade Deletion Integrity:** When a vehicle is removed, all associated service records are automatically deleted via JPA orphan removal and cascade rules (zero orphaned records in MySQL).
5. **Administrative Isolation:** The `ADMIN` role is blocked (`403 Forbidden`) from user vehicle service endpoints, preserving strict segregation of concerns.

---

## 2. Web MVC Architecture & Routes

| HTTP Method | Route | Controller Method | Access Role | Description |
|---|---|---|---|---|
| `GET` | `/vehicles/{id}?tab=service` | `viewVehicle` | `NORMAL_USER` | Displays vehicle details with service history table, empty-state banner, and action buttons. |
| `GET` | `/vehicles/{id}/services` | `listServices` | `NORMAL_USER` | Convenience redirect to `/vehicles/{id}?tab=service`. |
| `GET` | `/vehicles/{id}/services/add` | `addForm` | `NORMAL_USER` | Form to log a new service record for the vehicle. |
| `POST` | `/vehicles/{id}/services/add` | `addService` | `NORMAL_USER` | Validates input, saves record, updates vehicle odometer if higher, redirects with flash message. |
| `GET` | `/vehicles/{id}/services/{sId}/edit` | `editForm` | `NORMAL_USER` | Pre-populates form with service details. Validates two-tier ownership. |
| `POST` | `/vehicles/{id}/services/{sId}/edit` | `updateService`| `NORMAL_USER` | Updates service attributes; updates vehicle odometer if higher. |
| `POST` | `/vehicles/{id}/services/{sId}/delete` | `deleteService` | `NORMAL_USER` | Deletes the service record. Validates two-tier ownership. CSRF protected. |

---

## 3. REST API Specification

| Method | Endpoint | Request Body | Response Status | Description |
|---|---|---|---|---|
| `GET` | `/api/vehicles/{id}/services` | None (Optional `?search=`) | `200 OK` / `400 Bad Request` | Returns JSON array of service records for vehicle. Blocks cross-user access. |
| `GET` | `/api/services/{id}` | None | `200 OK` / `400 Bad Request` | Returns service record details. Blocks cross-user access. |
| `GET` | `/api/vehicles/{vid}/services/{sid}` | None | `200 OK` / `400 Bad Request` | Returns service record with vehicle ownership verification. |
| `POST` | `/api/vehicles/{id}/services` | `ServiceRecordRequest` JSON | `201 Created` / `400 Bad Request` | Logs new service record; updates odometer if higher. |
| `PUT` | `/api/services/{id}` | `ServiceRecordRequest` JSON | `200 OK` / `400 Bad Request` | Updates service record. Blocks cross-user mutations. |
| `PUT` | `/api/vehicles/{vid}/services/{sid}` | `ServiceRecordRequest` JSON | `200 OK` / `400 Bad Request` | Vehicle-scoped service record update. |
| `DELETE` | `/api/services/{id}` | None | `200 OK` / `400 Bad Request` | Deletes service record. Blocks cross-user deletions. |
| `DELETE` | `/api/vehicles/{vid}/services/{sid}` | None | `200 OK` / `400 Bad Request` | Vehicle-scoped service record deletion. |

---

## 4. Server-Side Validation Rules (`ServiceRecordRequest.java`)

- `serviceDate`: `@NotNull(message = "Service date is required")`
- `serviceType`: `@NotBlank(message = "Service type is required")`, `@Size(max = 100)`
- `garageName`: `@Size(max = 100)`
- `cost`: `@DecimalMin(value = "0.0", message = "Cost cannot be negative")`, `@Digits(integer = 8, fraction = 2)`
- `odometerAtService`: `@Min(value = 0, message = "Odometer cannot be negative")`
- `description`: `@Size(max = 500)`
- `notes`: `@Size(max = 1000)`

---

## 5. Jackson Serialization & Entity Design

- In `ServiceRecord.java`, the `vehicle` association is annotated with `@JsonIgnore` to prevent recursive Jackson serialization and `LazyInitializationException` outside transactions.
- A public getter `public Long getVehicleId()` is exposed, producing clean and standard JSON payloads:
  ```json
  {
    "serviceId": 1,
    "vehicleId": 5,
    "serviceDate": "2026-09-01",
    "serviceType": "30,000 km Periodic Service",
    "garageName": "Toyota Care",
    "cost": 6500.00,
    "odometerAtService": 31200,
    "description": "Engine oil, brake fluid, oil filter"
  }
  ```

---

## 6. Verification & Automated Test Coverage

The dedicated test suite [`ServiceModuleTest.java`](file:///d:/HARIHARAN%20P/000_JAVA%20FULL%20STACK%20-%20Final%20Year%20Main_Projects%20-%202026_2027/019_MyGarage%20-%20A%20Vehicle%20Service%20History,%20Fuel%20Record%20and%20Maintenance%20Tracking%20Platform/backend/src/test/java/com/mygarage/service/ServiceModuleTest.java) executes **25 tests**:

1. Web: Service History Listing for Vehicle — `PASS`
2. Web: Empty Service History Display — `PASS`
3. Web: Render Add Service Record Form — `PASS`
4. Web: Add Service Record Success (Valid Data & Odometer Update) — `PASS`
5. Web: Add Service Validation Failure (Missing Required Fields) — `PASS`
6. Web: Negative Cost is Rejected — `PASS`
7. Web: Negative Odometer is Rejected — `PASS`
8. Web: Service History Ordering (serviceDate DESC) — `PASS`
9. Web: Render Edit Service Record Form — `PASS`
10. Web: Update Service Record Success — `PASS`
11. Web: Delete Service Record Success — `PASS`
12. Ownership: Cross-User Service Creation Blocked — `PASS`
13. Ownership: Cross-User Service Mutation Blocked — `PASS`
14. Ownership: Cross-User Service Deletion Blocked — `PASS`
15. Ownership: Service Mismatched Vehicle ID Blocked — `PASS`
16. REST: GET `/api/vehicles/{id}/services` returns list — `PASS`
17. REST: GET `/api/services/{id}` returns single record — `PASS`
18. REST: Cross-User GET `/api/services/{id}` returns 400 Bad Request — `PASS`
19. REST: POST `/api/vehicles/{id}/services` Success (201 Created) — `PASS`
20. REST: Cross-User POST `/api/vehicles/{id}/services` returns 400 — `PASS`
21. REST: PUT `/api/services/{id}` Updates Record — `PASS`
22. REST: DELETE `/api/services/{id}` Deletes Record — `PASS`
23. Security: ADMIN Role Cannot Access User Service Endpoints (403 Forbidden) — `PASS`
24. Security: Unauthenticated Access is Redirected to `/login` — `PASS`
25. Cascade: Vehicle Deletion Cascades and Removes Service Records — `PASS`

**Full Regression Test Suite:** `mvn clean test "-Dspring.profiles.active=test"`:
- **Total Tests Executed:** **88**
- **Failures:** **0**
- **Errors:** **0**
- **Build Status:** **`BUILD SUCCESS`**