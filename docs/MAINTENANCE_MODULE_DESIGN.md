# MAINTENANCE MODULE DESIGN & SPECIFICATION — MyGarage (APPJFS19)

**Project Name:** MyGarage — A Vehicle Service History, Fuel Record and Maintenance Tracking Platform  
**System ID:** APPJFS19  
**Module:** Milestone 7 (M7) — Maintenance Module  
**Author:** Senior Full Stack Architect & Engineering Team  
**Last Updated:** 2026-09-03  
**Status:** **100% IMPLEMENTED, TESTED & VERIFIED**

---

## 1. Module Overview & Architectural Pillars

The Maintenance Module provides vehicle owners with proactive preventive maintenance scheduling, real-time status transitions (`OVERDUE`, `DUE_TODAY`, `UPCOMING`, `COMPLETED`), completion tracking, and alert aggregation for the user dashboard and vehicle management screens.

### Key Architectural Pillars:
1. **Strict Two-Tier Ownership Model:**
   ```
   Authenticated User (NORMAL_USER)
      ↓ owns
   Vehicle (findByVehicleIdAndUserUserId)
      ↓ owns
   MaintenanceRecord (findByMaintenanceIdAndVehicleUserUserId)
   ```
   Cross-user task viewing, additions, edits, completion toggling, or deletions are strictly prevented at the service layer. URL and API ID tampering across mismatched vehicles is blocked with `IllegalArgumentException`.
2. **Deterministic Status Logic (Real Business Rules):**
   - Maintenance status is dynamically evaluated based on scheduled and completed dates:
     ```
     if completedDate != null          -> COMPLETED
     else if scheduledDate < today     -> OVERDUE
     else if scheduledDate == today    -> DUE_TODAY
     else                              -> UPCOMING
     ```
   - Dynamically recomputed upon retrieval to ensure tasks naturally transition from `UPCOMING` to `DUE_TODAY` and `OVERDUE` over calendar time without requiring cron jobs.
3. **One-Click Task Completion:**
   - Tasks can be transitioned to `COMPLETED` from the vehicle detail view via a dedicated button (`POST /vehicles/{vid}/maintenance/{mid}/complete`), automatically populating the completion date and updating task status.
4. **Cascade Deletion Integrity:**
   - Deleting a parent vehicle cascades and cleanly removes all child maintenance records (zero orphaned records in MySQL).
5. **Administrative Isolation:**
   - The `ADMIN` role is blocked (`403 Forbidden`) from user maintenance endpoints.

---

## 2. Web MVC Architecture & Routes

| HTTP Method | Route | Controller Method | Access Role | Description |
|---|---|---|---|---|
| `GET` | `/vehicles/{id}?tab=maintenance` | `viewVehicle` | `NORMAL_USER` | Displays vehicle details with maintenance table, status badges, empty state, and action buttons. |
| `GET` | `/vehicles/{id}/maintenance` | `listMaintenance` | `NORMAL_USER` | Convenience redirect to `/vehicles/{id}?tab=maintenance`. |
| `GET` | `/vehicles/{id}/maintenance/add` | `addForm` | `NORMAL_USER` | Form to schedule a new maintenance task. |
| `POST` | `/vehicles/{id}/maintenance/add` | `addMaintenance` | `NORMAL_USER` | Validates input, evaluates status, saves record, redirects with flash message. |
| `GET` | `/vehicles/{id}/maintenance/{mId}/edit` | `editForm` | `NORMAL_USER` | Pre-populates form with task attributes. Verifies two-tier ownership. |
| `POST` | `/vehicles/{id}/maintenance/{mId}/edit` | `updateMaintenance`| `NORMAL_USER` | Updates maintenance attributes and re-evaluates status. |
| `POST` | `/vehicles/{id}/maintenance/{mId}/complete` | `markCompleted` | `NORMAL_USER` | Marks task as completed with current date. CSRF protected. |
| `POST` | `/vehicles/{id}/maintenance/{mId}/delete` | `deleteMaintenance` | `NORMAL_USER` | Deletes maintenance task with CSRF verification. |

---

## 3. REST API Specification

| Method | Endpoint | Request Body | Response Status | Description |
|---|---|---|---|---|
| `GET` | `/api/vehicles/{id}/maintenance` | None | `200 OK` / `400 Bad Request` | Returns JSON array of maintenance tasks for vehicle with refreshed status. |
| `GET` | `/api/maintenance/{id}` | None | `200 OK` / `400 Bad Request` | Returns maintenance task details by ID. |
| `GET` | `/api/vehicles/{vid}/maintenance/{mid}` | None | `200 OK` / `400 Bad Request` | Returns maintenance task with vehicle ownership verification. |
| `POST` | `/api/vehicles/{id}/maintenance` | `MaintenanceRequest` JSON | `201 Created` / `400 Bad Request` | Creates new maintenance task; sets computed status. |
| `PUT` | `/api/maintenance/{id}` | `MaintenanceRequest` JSON | `200 OK` / `400 Bad Request` | Updates maintenance task. |
| `PUT` | `/api/vehicles/{vid}/maintenance/{mid}` | `MaintenanceRequest` JSON | `200 OK` / `400 Bad Request` | Vehicle-scoped maintenance task update. |
| `PATCH` / `POST` | `/api/maintenance/{id}/complete` | None (Optional `?completedDate=`) | `200 OK` / `400 Bad Request` | Marks task as completed with specified or current date. |
| `PATCH` / `POST` | `/api/vehicles/{vid}/maintenance/{mid}/complete` | None (Optional `?completedDate=`) | `200 OK` / `400 Bad Request` | Vehicle-scoped mark completed. |
| `DELETE` | `/api/maintenance/{id}` | None | `200 OK` / `400 Bad Request` | Deletes maintenance task. |
| `DELETE` | `/api/vehicles/{vid}/maintenance/{mid}` | None | `200 OK` / `400 Bad Request` | Vehicle-scoped maintenance task deletion. |

---

## 4. Server-Side Validation Rules (`MaintenanceRequest.java`)

- `title`: `@NotBlank(message = "Task title is required")`, `@Size(max = 100)`
- `description`: `@Size(max = 500)`
- `scheduledDate`: `@NotNull(message = "Scheduled date is required")`
- `completedDate`: Optional `LocalDate`
- `cost`: `@DecimalMin(value = "0.0", message = "Cost cannot be negative")`, `@Digits(integer = 8, fraction = 2)`
- `notes`: `@Size(max = 1000)`

---

## 5. Jackson Serialization & Entity Design

- In `MaintenanceRecord.java`, the `vehicle` association is annotated with `@JsonIgnore` and exposes `public Long getVehicleId()` to prevent lazy proxy serialization errors and circular references:
  ```json
  {
    "maintenanceId": 1,
    "vehicleId": 7,
    "title": "Brake Fluid Flush",
    "description": "Check and flush hydraulic brake lines",
    "scheduledDate": "2026-09-18",
    "completedDate": null,
    "status": "UPCOMING",
    "cost": 1500.00,
    "notes": null
  }
  ```

---

## 6. Verification & Automated Test Coverage

The dedicated test suite [`MaintenanceModuleTest.java`](file:///d:/HARIHARAN%20P/000_JAVA%20FULL%20STACK%20-%20Final%20Year%20Main_Projects%20-%202026_2027/019_MyGarage%20-%20A%20Vehicle%20Service%20History,%20Fuel%20Record%20and%20Maintenance%20Tracking%20Platform/backend/src/test/java/com/mygarage/maintenance/MaintenanceModuleTest.java) executes **30 tests**:

1. Web: Maintenance Task Listing for Vehicle — `PASS`
2. Web: Empty State Display — `PASS`
3. Web: Render Add Maintenance Form — `PASS`
4. Web: Add Maintenance Task Success — `PASS`
5. Web: Validation Failure (Missing Required Title or Scheduled Date) — `PASS`
6. Web: Negative Cost is Rejected — `PASS`
7. Status: Computed as OVERDUE for Past Scheduled Date — `PASS`
8. Status: Computed as DUE_TODAY for Today's Scheduled Date — `PASS`
9. Status: Computed as UPCOMING for Future Scheduled Date — `PASS`
10. Web: Mark Maintenance Completed — `PASS`
11. Web: Render Edit Maintenance Form — `PASS`
12. Web: Update Maintenance Task Success — `PASS`
13. Web: Delete Maintenance Task Success — `PASS`
14. Ownership: Same-User Access Allowed — `PASS`
15. Ownership: Cross-User Maintenance View Blocked — `PASS`
16. Ownership: Cross-User Maintenance Creation Blocked — `PASS`
17. Ownership: Cross-User Maintenance Update Blocked — `PASS`
18. Ownership: Cross-User Maintenance Complete Blocked — `PASS`
19. Ownership: Cross-User Maintenance Delete Blocked — `PASS`
20. Ownership: Mismatched Vehicle/Maintenance ID Blocked — `PASS`
21. Security: Unauthenticated Access Redirects to `/login` — `PASS`
22. Security: ADMIN Role Cannot Access User Maintenance Endpoints (403 Forbidden) — `PASS`
23. REST: GET `/api/vehicles/{id}/maintenance` Returns List — `PASS`
24. REST: GET `/api/maintenance/{id}` Returns Single Record — `PASS`
25. REST: POST `/api/vehicles/{id}/maintenance` Creates Record (201 Created) — `PASS`
26. REST: PUT `/api/maintenance/{id}` Updates Record (200 OK) — `PASS`
27. REST: PATCH `/api/maintenance/{id}/complete` Marks Task Completed — `PASS`
28. REST: POST `/api/maintenance/{id}/complete` Marks Task Completed — `PASS`
29. REST: DELETE `/api/maintenance/{id}` Deletes Record (200 OK) — `PASS`
30. Ownership & Cascade: REST Cross-User Blocked & Vehicle Cascade Deletion — `PASS`

**Full Regression Test Suite:** `mvn clean test "-Dspring.profiles.active=test"`:
- **Total Tests Executed:** **146**
- **Failures:** **0**
- **Errors:** **0**
- **Build Status:** **`BUILD SUCCESS`**