# STUDENT VIVA PREPARATION GUIDE (TANGLISH) — MyGarage (APPJFS19)

**Project Title:** MyGarage — A Vehicle Service History, Fuel Record and Maintenance Tracking Platform  
**System ID:** APPJFS19  
**Target Audience:** Final Year Engineering Students (B.E. / B.Tech CSE / IT)

---

## 1. Project Overview & Pitch (30 Seconds Intro)

> *"Good morning Sir/Mam. My project name is **MyGarage**. It is a full-stack web application built using **Java 21, Spring Boot 3.3.5, Spring Security 6, Spring Data JPA, MySQL 8, and Thymeleaf with Bootstrap 5**.*  
> *In India, most vehicle owners maintain service bills in paper files, note fuel expenses in WhatsApp, and forget periodic maintenance dates. When selling the car or claiming warranty, history missing aagum.*  
> *MyGarage centralized digital logbook create pannum. User multiple vehicles add pannalam, service history track pannalam, consecutive fuel fill-ups vachu automatic mileage (km/L) calculate aagum, and scheduled maintenance ku automated `OVERDUE`, `DUE TODAY`, `UPCOMING` alerts kedaikkum.*  
> *It also features strict role-based access control with two roles: **NORMAL_USER** and **ADMIN**, with two-tier ownership protection and zero data leakage."*

---

## 2. Key Architecture Concepts (Viva Questions & Answers)

### Q1: What is the Architecture of your application?
**Answer:**  
*"Namma project **Layered Architecture (3-Tier)** follow pannudhu:*
1. **Presentation Layer:** Thymeleaf HTML5 templates + Bootstrap 5 responsive UI + REST API Controllers (`@RestController` under `/api/**`).
2. **Business / Service Layer:** Spring `@Service` classes (`VehicleService`, `ServiceRecordService`, `FuelRecordService`, `MaintenanceService`, `DashboardService`, `UserService`). All business logic, ownership checks, and `@Transactional` boundaries inga dhaan irukku.
3. **Data Access / Persistence Layer:** Spring Data JPA Repositories extending `JpaRepository` interacting with **MySQL 8.0** database through Hibernate ORM."*

---

### Q2: Why did you choose Java 21 and Spring Boot 3?
**Answer:**  
*"Java 21 is an official **LTS (Long Term Support)** release with modern features like pattern matching, record types, and virtual threads. Spring Boot 3.3.5 requires Java 17+ and provides Jakarta EE 10 standards, auto-configuration, production-ready actuator endpoints, and seamless Spring Security 6 integration."*

---

### Q3: How is Security and Authentication implemented?
**Answer:**  
*"Spring Security 6.x use panrom:*
- **BCrypt Hashing:** Plaintext passwords database-la save aagadhu. BCrypt with cost factor 10 hashing use panrom.
- **Form-based Session Authentication:** User login panna udaney HTTP Session create aagi `JSESSIONID` cookie generate aagum.
- **Two Roles Only:** `NORMAL_USER` and `ADMIN`. Admin self-register panna mudiyadhu, database-la pre-seeded with hashed credentials.
- **SecurityFilterChain Matcher Order:** Specific `/api/admin/**` and `/admin/**` matchers first check aagum, then broad `/api/**` and `/dashboard/**` check aagum.
- **CSRF Protection:** All state-changing POST, PUT, DELETE requests require valid CSRF tokens (`_csrf`)."

---

### Q4: Explain Two-Tier Ownership Protection.
**Answer:**  
*"Idhu romba critical viva point Sir:*
- **Tier 1 (Vehicle Level):** Whenever a user tries to access `/vehicles/{id}`, query checks `WHERE vehicle_id = :id AND user_id = :userId`. User A cannot access User B's vehicle.
- **Tier 2 (Record Level):** When accessing a service, fuel, or maintenance record directly by record ID (e.g. `/api/services/{id}`), system verifies `record.getVehicle().getUser().getUserId().equals(currentUser.getUserId())`.
- Idhanaala URL parameter manipulation panni vera user oda records yaarum view, edit, or delete panna mudiyadhu. System returns `403 Forbidden`."*

---

### Q5: How is Fuel Mileage ($\text{km/L}$) calculated?
**Answer:**  
*"FuelRecord entity-la `odometerAtFill` store aagudhu.  
Consecutive fill-ups ku naduvula:*
$$\text{Distance Traveled} = \text{Current Odometer} - \text{Previous Odometer}$$
$$\text{Estimated Mileage} = \frac{\text{Distance Traveled}}{\text{Quantity in Litres}}$$
*Current fill previous fill-oda odometer-ah vida kammiya irundha exception throw aagi validation trigger aagum."*

---

### Q6: How does the Maintenance Urgency Status work?
**Answer:**  
*"Maintenance task-ku AI thevai illa, clean deterministic business logic use panrom:*
- `completedDate != null` $\rightarrow$ **`COMPLETED`**
- `scheduledDate < today` $\rightarrow$ **`OVERDUE`** (Red alert on Dashboard)
- `scheduledDate == today` $\rightarrow$ **`DUE_TODAY`** (Yellow alert)
- `scheduledDate > today` $\rightarrow$ **`UPCOMING`** (Blue badge)"*

---

### Q7: What are the 6 Database Tables?
**Answer:**  
1. `users` — Authentication, roles, BCrypt password hash.
2. `vehicle_categories` — Standard categories (Sedan, SUV, Bike, etc.).
3. `vehicles` — User vehicles, plates, odometer.
4. `service_records` — Service logs, garage names, costs.
5. `fuel_records` — Fuel fills, volume, rate, calculated mileage.
6. `maintenance_records` — Reminders, due dates, urgency status.

*Referential Integrity:* When a vehicle is deleted, JPA `CascadeType.ALL` and `orphanRemoval = true` automatically deletes associated service, fuel, and maintenance records in a single transactional unit."*

---

## 3. Demo Steps for External Examiner

1. **Step 1:** Run `scripts\start.bat`. Open `http://localhost:8080`.
2. **Step 2:** Show Landing page with automotive branding.
3. **Step 3:** Click Register $\rightarrow$ Register new user `viva_user@test.com`.
4. **Step 4:** Show Login $\rightarrow$ Dashboard displays clean empty state (0 vehicles, 0 spend).
5. **Step 5:** Add a Vehicle (e.g. Honda City, Sedan, 12,000 km).
6. **Step 6:** Add a Service Record (`Full Service`, ₹4,500).
7. **Step 7:** Add 2 Fuel Records to demonstrate dynamic fuel mileage calculation.
8. **Step 8:** Add a Maintenance Reminder with past date $\rightarrow$ Point out `OVERDUE` red banner on Dashboard.
9. **Step 9:** Click `Mark Complete` $\rightarrow$ Show task converted to `COMPLETED`.
10. **Step 10:** Show Vehicle Timeline $\rightarrow$ Chronological history of all events.
11. **Step 11:** Logout $\rightarrow$ Login as Admin (`admin@mygarage.com` / `Admin@123`).
12. **Step 12:** Show Admin Dashboard (system stats, user search, category management). Show that Admin cannot spy on personal garage records.
13. **Step 13:** Demonstrate test coverage: run `scripts\run-tests.bat` showing **218/218 tests PASSING**.

---

**Good luck for your final year viva! You are 100% prepared! 🚀**