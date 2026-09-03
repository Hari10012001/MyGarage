# VEHICLE MODULE DESIGN & SPECIFICATION — MyGarage (APPJFS19)

**Project Name:** MyGarage — A Vehicle Service History, Fuel Record and Maintenance Tracking Platform  
**System ID:** APPJFS19  
**Module:** Milestone 4 (M4) — Vehicle Module  
**Author:** Senior Full Stack Architect & Development Team  
**Last Updated:** 2026-09-03  
**Status:** **100% IMPLEMENTED, TESTED & VERIFIED**

---

## 1. Module Overview & Architecture

The Vehicle Module forms the core operational foundation of the MyGarage platform. Every service record, fuel entry, and scheduled maintenance task is strictly anchored to an individual vehicle, which in turn is strictly anchored to a single authenticated `NORMAL_USER`.

### Key Architectural Pillars:
1. **Ownership Isolation:** A user can only view, mutate, or delete vehicles that exist in their personal garage. Cross-user access is impossible through both Web MVC direct URL tampering and REST API calls.
2. **Per-Garage License Plate Uniqueness:** The database and service layers enforce uniqueness of license plates **per user garage** (`existsByPlateNumberIgnoreCaseAndUserUserId`). Different users may track vehicles with the same plate number (e.g., in enterprise fleets or family garages), but a single user cannot duplicate a plate.
3. **Cascade Deletion Integrity:** In compliance with M2 design rules, deleting a vehicle cascades and removes all dependent records (`service_records`, `fuel_records`, `maintenance_records`) ensuring zero orphaned data in the MySQL database.
4. **Administrative Isolation:** The `ADMIN` role is strictly segregated from personal garage endpoints (`/vehicles/**`, `/api/vehicles/**`), preventing administrative pollution or accidental modification of user assets.

---

## 2. Web MVC Architecture & Routes

| HTTP Method | Route | Controller Method | Access Role | Description |
|---|---|---|---|---|
| `GET` | `/vehicles` | `listVehicles` | `NORMAL_USER` | Displays user's garage. Includes search, active categories, and empty-state guidance. |
| `GET` | `/vehicles/add` | `addForm` | `NORMAL_USER` | Renders vehicle creation form populated with active `VehicleCategory` options. |
| `POST` | `/vehicles/add` | `addVehicle` | `NORMAL_USER` | Validates and persists vehicle; normalizes plate to uppercase; validates duplicate plate. |
| `GET` | `/vehicles/{id}` | `viewVehicle` | `NORMAL_USER` | Detailed view with odometer, fuel type, category badge, and service/fuel/maintenance summary. |
| `GET` | `/vehicles/{id}/edit` | `editForm` | `NORMAL_USER` | Pre-populates form for editing existing vehicle attributes. |
| `POST` | `/vehicles/{id}/edit` | `updateVehicle`| `NORMAL_USER` | Updates vehicle attributes; allows keeping current plate; checks uniqueness if plate changed. |
| `POST` | `/vehicles/{id}/delete` | `deleteVehicle` | `NORMAL_USER` | Cascades deletion of vehicle and all child records with CSRF verification. |
| `GET` | `/vehicles/{id}/timeline`| `timeline` | `NORMAL_USER` | Chronological unified timeline of all vehicle events. |

---

## 3. REST API Specification (`/api/vehicles`)

All REST endpoints operate on JSON payloads and enforce `@PreAuthorize("hasRole('NORMAL_USER')")` / `SecurityFilterChain` rules:

| Method | Endpoint | Request Body | Response Status | Description |
|---|---|---|---|---|
| `GET` | `/api/vehicles` | None (Optional `?search=`) | `200 OK` | Returns JSON array of vehicles belonging strictly to the authenticated user. |
| `GET` | `/api/vehicles/{id}` | None | `200 OK` / `400 Bad Request` | Returns vehicle details or 400 if ID belongs to another user. |
| `POST` | `/api/vehicles` | `VehicleRequest` JSON | `201 Created` / `400 Bad Request` | Creates vehicle for authenticated user. |
| `PUT` | `/api/vehicles/{id}` | `VehicleRequest` JSON | `200 OK` / `400 Bad Request` | Updates vehicle attributes. Blocks cross-user mutations. |
| `DELETE` | `/api/vehicles/{id}` | None | `200 OK` / `400 Bad Request` | Cascades delete for vehicle. Blocks cross-user deletions. |

---

## 4. Server-Side Validation Rules (`VehicleRequest.java`)

- `categoryId`: `@NotNull(message = "Category is required")`
- `plateNumber`: `@NotBlank(message = "Plate number is required")`, `@Size(min = 4, max = 20)`
- `make`: `@NotBlank(message = "Make is required")`, `@Size(max = 50)`
- `model`: `@NotBlank(message = "Model is required")`, `@Size(max = 50)`
- `year`: `@NotNull(message = "Year is required")`, `@Min(1900)`, `@Max(2030)`
- `currentOdometer`: `@Min(0, message = "Odometer cannot be negative")`
- `notes`: `@Size(max = 500)`

---

## 5. UI/UX & Responsive Design Highlights

1. **Empty State Component:** When a garage has zero vehicles, the list view renders a welcoming card with vehicle icon, helpful instructions, and a primary CTA button: *"Add Your First Vehicle"*.
2. **Real-time Search Filter:** Users can search their garage by make, model, or plate number. A clear button allows instantaneous reset.
3. **Vehicle Cards:** Cards display category emoji icon (`🚗`, `🚙`, `🏍️`), make + model header, license plate badge, manufacturing year, color, and current odometer reading.
4. **Delete Protection Modal:** Delete triggers a Bootstrap modal requiring explicit user confirmation and warning that all child service, fuel, and maintenance records will be permanently removed.
5. **CSRF Protection:** Every state-altering web form (`add`, `edit`, `delete`) embeds the Thymeleaf `_csrf` token.

---

## 6. Verification & Automated Test Coverage

The dedicated test suite [`VehicleModuleTest.java`](file:///d:/HARIHARAN%20P/000_JAVA%20FULL%20STACK%20-%20Final%20Year%20Main_Projects%20-%202026_2027/019_MyGarage%20-%20A%20Vehicle%20Service%20History,%20Fuel%20Record%20and%20Maintenance%20Tracking%20Platform/backend/src/test/java/com/mygarage/vehicle/VehicleModuleTest.java) includes **26 comprehensive tests**:

1. Web: View My Vehicles List (NORMAL_USER) — `PASS`
2. Web: Render Add Vehicle Form (NORMAL_USER) — `PASS`
3. Web: Add Vehicle Success — `PASS`
4. Web: Add Vehicle Validation Failure — `PASS`
5. Web: Duplicate Plate Number per User Garage is Rejected — `PASS`
6. Web: Same Plate Number for DIFFERENT Users is Allowed — `PASS`
7. Web: View Vehicle Details for Own Vehicle — `PASS`
8. Web: Direct URL Access to ANOTHER User's Vehicle is Forbidden — `PASS`
9. Web: Render Edit Vehicle Form for Own Vehicle — `PASS`
10. Web: Edit Form for ANOTHER User's Vehicle is Forbidden — `PASS`
11. Web: Update Vehicle Success — `PASS`
12. Web: Delete Vehicle Cascades and Removes Child Records — `PASS`
13. Web: Delete ANOTHER User's Vehicle is Forbidden — `PASS`
14. Web: Vehicle Timeline View for Own Vehicle — `PASS`
15. REST: GET `/api/vehicles` returns User-Scoped List — `PASS`
16. REST: GET `/api/vehicles/{id}` for Own Vehicle returns 200 — `PASS`
17. REST: GET `/api/vehicles/{id}` for ANOTHER User's Vehicle returns 400 — `PASS`
18. REST: POST `/api/vehicles` Success returns 201 Created — `PASS`
19. REST: POST `/api/vehicles` Validation Failure returns 400 — `PASS`
20. REST: PUT `/api/vehicles/{id}` Success returns 200 — `PASS`
21. REST: PUT `/api/vehicles/{id}` on ANOTHER User's Vehicle returns 400 — `PASS`
22. REST: DELETE `/api/vehicles/{id}` Success returns 200 — `PASS`
23. REST: DELETE `/api/vehicles/{id}` on ANOTHER User's Vehicle returns 400 — `PASS`
24. ADMIN Role Cannot Access User Vehicle Web Routes (403 Forbidden) — `PASS`
25. ADMIN Role Cannot Access User Vehicle REST Endpoints (403 Forbidden) — `PASS`
26. Unauthenticated User is Redirected to `/login` — `PASS`

**Full Regression Test Suite:** `mvn clean test "-Dspring.profiles.active=test"`:
- **Total Tests Executed:** **63**
- **Failures:** **0**
- **Errors:** **0**
- **Build Result:** **`BUILD SUCCESS`**