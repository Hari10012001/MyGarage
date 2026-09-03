# FUEL MODULE DESIGN & SPECIFICATION — MyGarage (APPJFS19)

**Project Name:** MyGarage — A Vehicle Service History, Fuel Record and Maintenance Tracking Platform  
**System ID:** APPJFS19  
**Module:** Milestone 6 (M6) — Fuel Module  
**Author:** Senior Full Stack Architect & Engineering Team  
**Last Updated:** 2026-09-03  
**Status:** **100% IMPLEMENTED, TESTED & VERIFIED**

---

## 1. Module Overview & Architectural Pillars

The Fuel Module provides vehicle owners with comprehensive fuel log tracking, automatic total cost calculations, odometer progression tracking, and estimated fuel efficiency (mileage in km/L) analytics.

### Architectural Pillars:
1. **Strict Two-Tier Ownership Model:**
   ```
   Authenticated User (NORMAL_USER)
      ↓ owns
   Vehicle (findByVehicleIdAndUserUserId)
      ↓ owns
   FuelRecord (findByFuelIdAndVehicleUserUserId)
   ```
   Cross-user fuel access, additions, mutations, or deletions are intercepted at the service layer and rejected with `IllegalArgumentException`. URL tampering across mismatched vehicles is prevented.
2. **Dynamic Estimated Mileage Calculation:**
   - Mileage is calculated using the distance delta between the current record's odometer reading and the chronologically preceding fuel fill:
     $$\text{Distance} = \text{Odometer}_{\text{current}} - \text{Odometer}_{\text{previous}}$$
     $$\text{Estimated Mileage} = \frac{\text{Distance}}{\text{Quantity}_{\text{current}}}$$
   - Clearly labeled in the UI as `km/L (est.)` to indicate it is an operational estimate based on user-entered odometer readings.
3. **Automatic Total Cost Computation:**
   - Computed on `@PrePersist` and `@PreUpdate` lifecycle events:
     $$\text{Total Cost} = \text{Quantity (Litres)} \times \text{Cost per Litre}$$
   - Interactive client-side JavaScript (`calcTotal()`) dynamically updates the total cost field as the user types in the form.
4. **Odometer Progression:**
   - Whenever a fuel record is logged or updated with an odometer reading higher than the vehicle's current odometer, the parent vehicle's `currentOdometer` is automatically updated.
5. **Cascade Deletion Integrity:**
   - Deleting a parent vehicle cascades and cleanly removes all child fuel records (zero orphaned records).
6. **Administrative Isolation:**
   - The `ADMIN` role is blocked (`403 Forbidden`) from user fuel endpoints.

---

## 2. Web MVC Architecture & Routes

| HTTP Method | Route | Controller Method | Access Role | Description |
|---|---|---|---|---|
| `GET` | `/vehicles/{id}?tab=fuel` | `viewVehicle` | `NORMAL_USER` | Displays vehicle details with fuel history table, estimated mileage badges, empty state, and action buttons. |
| `GET` | `/vehicles/{id}/fuel` | `listFuel` | `NORMAL_USER` | Convenience redirect to `/vehicles/{id}?tab=fuel`. |
| `GET` | `/vehicles/{id}/fuel/add` | `addForm` | `NORMAL_USER` | Form to log a fuel fill-up with fuel type dropdown and dynamic cost calculator. |
| `POST` | `/vehicles/{id}/fuel/add` | `addFuel` | `NORMAL_USER` | Validates input, calculates estimated mileage, updates vehicle odometer, redirects with flash message. |
| `GET` | `/vehicles/{id}/fuel/{fId}/edit` | `editForm` | `NORMAL_USER` | Pre-populates form with fuel fill attributes. Verifies two-tier ownership. |
| `POST` | `/vehicles/{id}/fuel/{fId}/edit` | `updateFuel` | `NORMAL_USER` | Updates fuel attributes and recalculates estimated mileage. |
| `POST` | `/vehicles/{id}/fuel/{fId}/delete` | `deleteFuel` | `NORMAL_USER` | Deletes the fuel record with CSRF verification. |

---

## 3. REST API Specification

| Method | Endpoint | Request Body | Response Status | Description |
|---|---|---|---|---|
| `GET` | `/api/vehicles/{id}/fuel` | None (Optional `?from=&to=`) | `200 OK` / `400 Bad Request` | Returns JSON array of fuel records for vehicle. Blocks cross-user access. |
| `GET` | `/api/fuel/{id}` | None | `200 OK` / `400 Bad Request` | Returns fuel record details by ID. |
| `GET` | `/api/vehicles/{vid}/fuel/{fid}` | None | `200 OK` / `400 Bad Request` | Returns fuel record with vehicle ownership verification. |
| `POST` | `/api/vehicles/{id}/fuel` | `FuelRecordRequest` JSON | `201 Created` / `400 Bad Request` | Logs new fuel fill, calculates estimated mileage, updates vehicle odometer. |
| `PUT` | `/api/fuel/{id}` | `FuelRecordRequest` JSON | `200 OK` / `400 Bad Request` | Updates fuel record. Blocks cross-user mutations. |
| `PUT` | `/api/vehicles/{vid}/fuel/{fid}` | `FuelRecordRequest` JSON | `200 OK` / `400 Bad Request` | Vehicle-scoped fuel record update. |
| `DELETE` | `/api/fuel/{id}` | None | `200 OK` / `400 Bad Request` | Deletes fuel record. Blocks cross-user deletions. |
| `DELETE` | `/api/vehicles/{vid}/fuel/{fid}` | None | `200 OK` / `400 Bad Request` | Vehicle-scoped fuel record deletion. |

---

## 4. Server-Side Validation Rules (`FuelRecordRequest.java`)

- `fuelDate`: `@NotNull(message = "Fuel date is required")`
- `fuelType`: `@NotNull(message = "Fuel type is required")` (Enum: `PETROL`, `DIESEL`, `CNG`, `ELECTRIC`, `HYBRID`, `LPG`, `OTHER`)
- `quantityLitres`: `@NotNull`, `@DecimalMin(value = "0.1", message = "Quantity must be greater than 0")`, `@Digits(integer = 6, fraction = 2)`
- `costPerLitre`: `@NotNull`, `@DecimalMin(value = "0.01", message = "Cost per litre must be positive")`, `@Digits(integer = 6, fraction = 2)`
- `odometerAtFill`: `@Min(value = 0, message = "Odometer cannot be negative")`
- `notes`: `@Size(max = 500)`

---

## 5. Jackson Serialization & Entity Design

- In `FuelRecord.java`, the `vehicle` association is annotated with `@JsonIgnore` and exposes `public Long getVehicleId()` to prevent lazy proxy serialization issues and circular references:
  ```json
  {
    "fuelId": 2,
    "vehicleId": 6,
    "fuelDate": "2026-08-20",
    "fuelType": "PETROL",
    "quantityLitres": 20.00,
    "costPerLitre": 102.00,
    "totalCost": 2040.00,
    "odometerAtFill": 5400,
    "estimatedMileageKmpl": 20.00,
    "notes": "Updated Highway Drive"
  }
  ```

---

## 6. Verification & Automated Test Coverage

The dedicated test suite [`FuelModuleTest.java`](file:///d:/HARIHARAN%20P/000_JAVA%20FULL%20STACK%20-%20Final%20Year%20Main_Projects%20-%202026_2027/019_MyGarage%20-%20A%20Vehicle%20Service%20History,%20Fuel%20Record%20and%20Maintenance%20Tracking%20Platform/backend/src/test/java/com/mygarage/fuel/FuelModuleTest.java) executes **28 tests**:

1. Web: Fuel History Listing for Vehicle — `PASS`
2. Web: Empty Fuel History Display — `PASS`
3. Web: Render Add Fuel Record Form — `PASS`
4. Web: Add Fuel Record Success (Valid Data & Odometer Update) — `PASS`
5. Web: Add Fuel Validation Failure (Missing Required Fields) — `PASS`
6. Web: Negative Quantity is Rejected — `PASS`
7. Web: Negative Rate is Rejected — `PASS`
8. Web: Negative Odometer is Rejected — `PASS`
9. Web: Fuel Record Linked to Correct Vehicle — `PASS`
10. Web: Fuel Ordering (fuelDate DESC) — `PASS`
11. Mileage: Estimated Mileage Calculation (Distance / Litres) — `PASS`
12. Web: Render Edit Fuel Record Form — `PASS`
13. Web: Update Fuel Record Success — `PASS`
14. Web: Delete Fuel Record Success — `PASS`
15. Ownership: Same-User Access Allowed — `PASS`
16. Ownership: Cross-User Fuel History Blocked — `PASS`
17. Ownership: Cross-User Fuel Creation Blocked — `PASS`
18. Ownership: Cross-User Fuel Mutation Blocked — `PASS`
19. Ownership: Cross-User Fuel Deletion Blocked — `PASS`
20. Ownership: Mismatched Vehicle/Fuel ID Blocked — `PASS`
21. Security: Unauthenticated Access Redirects to `/login` — `PASS`
22. Security: ADMIN Role Cannot Access User Fuel Endpoints (403 Forbidden) — `PASS`
23. REST: GET `/api/vehicles/{id}/fuel` Returns List — `PASS`
24. REST: GET `/api/fuel/{id}` Returns Single Fuel Record — `PASS`
25. REST: POST `/api/vehicles/{id}/fuel` Success (201 Created) — `PASS`
26. REST: PUT `/api/fuel/{id}` Updates Record (200 OK) — `PASS`
27. REST: DELETE `/api/fuel/{id}` Deletes Record (200 OK) — `PASS`
28. REST: Cross-User Ownership Blocked & Cascade Deletion Removes Fuel Records — `PASS`

**Full Regression Test Suite:** `mvn clean test "-Dspring.profiles.active=test"`:
- **Total Tests Executed:** **116**
- **Failures:** **0**
- **Errors:** **0**
- **Build Status:** **`BUILD SUCCESS`**