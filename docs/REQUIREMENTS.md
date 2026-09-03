# REQUIREMENTS.md — MyGarage (APPJFS19)

## 1. Overview

**Project Name:** MyGarage
**Full Title:** MyGarage – A Vehicle Service History, Fuel Record and Maintenance Tracking Platform Using Java, Spring Boot, REST APIs and MySQL
**Project ID:** APPJFS19
**Academic Year:** 2026–2027
**Domain:** Web Application / Java Full Stack

MyGarage is a centralized web application that helps individual vehicle owners track every important aspect of their vehicles — service records, fuel expenses, and scheduled maintenance — in one easy-to-use platform.

---

## 2. Problem Statement

Vehicle owners in India typically manage:
- Service bills in physical files or plastic bags
- Fuel expenses in phone notes or memory
- Maintenance reminders in handwritten diaries
- Repair history only when they can remember it

Over months and years this information becomes scattered, lost, or forgotten. When selling a vehicle, getting insurance, or claiming warranty, this missing history costs money and time.

---

## 3. Existing System

- **Paper bills / physical files** — prone to damage, loss, fire
- **Phone notes / WhatsApp** — unstructured, hard to search
- **Memory** — unreliable for dates, costs, odometer readings
- **Excel sheets** — no reminders, no role separation, no mobile view
- **Generic expense apps** — not vehicle-specific, no service categories
- **Manufacturer apps** — only work for one brand, not all vehicles

---

## 4. Problems with Existing System

1. No single place for all vehicle-related records
2. Easy to lose paper bills
3. No automatic maintenance reminders
4. Cannot track multiple vehicles together
5. No history timeline view
6. Fuel efficiency cannot be easily calculated
7. Cannot share/present history when selling vehicle
8. No search or filter capability

---

## 5. Proposed System — MyGarage

A web application where any vehicle owner can:
- Register and log in securely
- Add one or more vehicles (car, bike, truck, etc.)
- Log every service visit with cost, garage name, and notes
- Record every fuel fill-up with quantity, cost, and odometer
- Track maintenance tasks with due dates and status
- View a complete history timeline per vehicle
- See dashboard statistics for quick insights
- Admin manages users and vehicle categories

---

## 6. Advantages of Proposed System

1. All vehicle data in one place — no paper needed
2. Multiple vehicles per user
3. Maintenance due/overdue alerts on dashboard
4. Fuel mileage calculation (distance / fuel quantity)
5. Search and filter across records
6. Timeline view showing full vehicle life history
7. Admin can manage categories and monitor usage
8. Works on mobile browsers (Bootstrap responsive)
9. Zero cost to run (localhost)
10. Secure (password hashing, role-based access, ownership protection)

---

## 7. Disadvantages / Limitations

1. Runs on localhost — not deployed to internet (out of scope)
2. No email reminders for maintenance (no email service)
3. No fuel price tracking (no external API)
4. No GPS or OBD integration
5. No mobile app — browser-based only
6. Admin cannot impersonate/edit individual user records
7. No file/image upload for bills in MVP
8. No export to PDF/Excel in MVP

---

## 8. Objectives

1. Build a complete Java Full Stack web application using Spring Boot, REST APIs, MySQL, and Bootstrap
2. Implement secure authentication with two roles (USER, ADMIN)
3. Implement CRUD operations for Vehicles, Service Records, Fuel Records, Maintenance Records
4. Implement simple business logic for maintenance status (OVERDUE / DUE / UPCOMING / COMPLETED)
5. Implement dashboard with real statistics (no hardcoded data)
6. Implement search and filter for records
7. Implement ownership protection — users see only their own data
8. Create a clean, responsive, professional-looking UI
9. Document the project completely for viva and GitHub portfolio
10. Write automated tests and smoke tests

---

## 9. Scope

**In Scope:**
- User registration and login (Spring Security)
- User profile management
- Vehicle CRUD (add, edit, delete, view)
- Service record CRUD per vehicle
- Fuel record CRUD per vehicle
- Maintenance record CRUD per vehicle with status logic
- Dashboard with real statistics
- Vehicle history timeline
- Search and filter
- Admin: user management, vehicle category management, system statistics
- Role-based access control (USER / ADMIN)
- Ownership protection
- Input validation and error handling
- Smoke tests and automated tests
- Documentation (README, Tanglish guide, API docs)
- BAT scripts (start, stop, setup-db, run-tests, health-check)

**Out of Scope:**
- Email / SMS notifications
- GPS / OBD integration
- Fuel price APIs
- Maps
- Payment gateway
- AI / ML / predictive maintenance
- Mobile app
- Cloud deployment
- Microservices / Kafka / Redis / Docker
- File/image upload (deferred to future)
- PDF/Excel export (deferred to future)

---

## 10. User Roles

### NORMAL USER
- Self-register
- Login / Logout
- View and edit own profile
- Add, edit, view, delete own vehicles
- Add, edit, view, delete service records for own vehicles
- Add, edit, view, delete fuel records for own vehicles
- Add, edit, view, delete maintenance records for own vehicles
- View vehicle history timeline
- View personal dashboard statistics

### ADMIN (seeded in database, cannot self-register as admin)
- Login / Logout
- View admin dashboard with system-wide statistics
- View and manage all registered users (activate/deactivate)
- Manage vehicle categories (add, edit, delete)
- View all vehicle records (read-only monitoring)
- View all service, fuel, maintenance record counts

---

## 11. Modules

| # | Module | Description |
|---|--------|-------------|
| 1 | User Module | Register, login, profile, password change |
| 2 | Vehicle Module | Add/edit/delete/view vehicles, categorized |
| 3 | Service Module | Log service visits, garages, costs, parts |
| 4 | Fuel Module | Log fuel fill-ups, mileage calculation |
| 5 | Maintenance Module | Track tasks, due dates, status logic |
| 6 | Dashboard & Reports | Statistics, charts, overdue alerts |
| 7 | Admin Module | User management, category management, monitoring |

---

## 12. Functional Requirements

### User Module
- FR-U01: User can register with name, email, password, phone
- FR-U02: Email must be unique
- FR-U03: Password stored hashed (BCrypt)
- FR-U04: User can login with email and password
- FR-U05: User can view and update own profile
- FR-U06: User can change password (with current password verification)
- FR-U07: User can logout
- FR-U08: Default role is NORMAL_USER on registration

### Vehicle Module
- FR-V01: User can add a vehicle with plate number, make, model, year, category, color, current odometer
- FR-V02: Plate number must be unique per user
- FR-V03: User can edit own vehicle details
- FR-V04: User can delete own vehicle (cascades to all records)
- FR-V05: User can view list of own vehicles
- FR-V06: User can view full details of one vehicle
- FR-V07: Vehicle belongs to a category (managed by admin)

### Service Module
- FR-S01: User can add a service record to any of their own vehicles
- FR-S02: Service record includes: date, service type, description, garage name, cost, odometer, next service due date (optional), notes
- FR-S03: User can edit own service records
- FR-S04: User can delete own service records
- FR-S05: User can view all service records for a vehicle
- FR-S06: Service records sorted by date descending by default

### Fuel Module
- FR-F01: User can add a fuel record to any of their own vehicles
- FR-F02: Fuel record includes: date, fuel type, quantity (litres), cost per litre, total cost, odometer, notes
- FR-F03: System calculates mileage if previous odometer reading is available
- FR-F04: User can edit own fuel records
- FR-F05: User can delete own fuel records
- FR-F06: User can view all fuel records for a vehicle

### Maintenance Module
- FR-M01: User can add a maintenance record to any of their own vehicles
- FR-M02: Maintenance record includes: title, description, scheduled date, completion date (optional), status, cost (optional), notes
- FR-M03: Status logic: if scheduledDate < today AND not completed → OVERDUE; if scheduledDate = today AND not completed → DUE_TODAY; if scheduledDate > today AND not completed → UPCOMING; if completedDate is set → COMPLETED
- FR-M04: User can edit own maintenance records
- FR-M05: User can delete own maintenance records
- FR-M06: User can mark maintenance as completed

### Dashboard Module
- FR-D01: User dashboard shows: total vehicles, total service records, total fuel records, upcoming/overdue maintenance count
- FR-D02: Dashboard shows recent service records (last 5)
- FR-D03: Dashboard shows overdue and due-today maintenance alerts
- FR-D04: Admin dashboard shows: total users, total vehicles, total service records, total fuel records, total maintenance records
- FR-D05: All statistics are read from database — no hardcoding

### Admin Module
- FR-A01: Admin can view all registered users
- FR-A02: Admin can activate/deactivate user accounts
- FR-A03: Admin can add, edit, delete vehicle categories
- FR-A04: Admin can view system-wide statistics
- FR-A05: Admin cannot modify user personal records (view only)

### Search & Filter
- FR-SF01: User can search service records by service type, garage name
- FR-SF02: User can filter fuel records by date range, fuel type
- FR-SF03: User can filter maintenance records by status
- FR-SF04: User can search vehicles by make, model, plate number

---

## 13. Non-Functional Requirements

| # | Requirement | Detail |
|---|-------------|--------|
| NFR-01 | Performance | Pages load within 3 seconds on localhost |
| NFR-02 | Security | BCrypt password hashing, Spring Security, ownership checks |
| NFR-03 | Usability | Responsive Bootstrap UI — works on laptop and mobile browser |
| NFR-04 | Reliability | Graceful error handling — no raw stack traces to user |
| NFR-05 | Maintainability | Clean package structure, meaningful names, comments |
| NFR-06 | Portability | Runs on Windows with JDK 21 + MySQL 8 |
| NFR-07 | Testability | JUnit tests for service layer, smoke tests for APIs |
| NFR-08 | Availability | Runs on localhost — no downtime requirement |

---

## 14. MVP (Minimum Viable Product)

The MVP must include:
- User register + login + logout
- Add, edit, view, delete at least one vehicle
- Add, edit, view, delete at least one service record
- Add, edit, view, delete at least one fuel record
- Add, edit, view, delete at least one maintenance record with status logic
- Dashboard with real statistics
- Admin login and user list
- Ownership protection working correctly

---

## 15. Future Enhancements (Post-MVP)

1. Email reminders for overdue maintenance
2. Bill/receipt image upload
3. PDF report export per vehicle
4. Fuel price history (manual entry)
5. Service reminder push notification (browser)
6. Multi-language support (Tamil, Hindi)
7. Vehicle resale report
8. Insurance renewal tracking
9. Vehicle comparison
10. Internet deployment (Heroku-free or Render.com)

---

## 16. Out of Scope (Hard Rules)

- AI / ML / deep learning
- IoT / GPS / OBD integration
- Cloud deployment
- Paid APIs
- SMS / WhatsApp / Email services
- Payment gateway
- Microservices / Kafka / Redis / Kubernetes
- Docker (unless genuinely required)
- Predictive maintenance algorithms
- Fuel price APIs / Maps APIs
