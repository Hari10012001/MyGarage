package com.mygarage.vehicle;

import com.mygarage.dto.response.GarageReliabilityMatrixDTO;
import com.mygarage.dto.response.VehicleReliabilityReportDTO;
import com.mygarage.model.ServiceRecord;
import com.mygarage.model.User;
import com.mygarage.model.Vehicle;
import com.mygarage.model.VehicleCategory;
import com.mygarage.model.enums.Role;
import com.mygarage.repository.ServiceRecordRepository;
import com.mygarage.repository.UserRepository;
import com.mygarage.repository.VehicleCategoryRepository;
import com.mygarage.repository.VehicleRepository;
import com.mygarage.service.VehicleReliabilityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
class VehicleReliabilityEngineModuleTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VehicleReliabilityService vehicleReliabilityService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private VehicleCategoryRepository vehicleCategoryRepository;

    @Autowired
    private ServiceRecordRepository serviceRecordRepository;

    private User testUser;
    private User otherUser;
    private User adminUser;
    private Vehicle userVehicle;
    private Vehicle otherVehicle;
    private VehicleCategory testCategory;

    @BeforeEach
    void setUp() {
        testCategory = new VehicleCategory();
        testCategory.setName("Cat_" + UUID.randomUUID().toString().substring(0, 8));
        testCategory.setIcon("car-front");
        testCategory = vehicleCategoryRepository.save(testCategory);

        testUser = new User();
        testUser.setEmail("reliability_user@garage.com");
        testUser.setPasswordHash("hash");
        testUser.setFullName("Reliability Tester");
        testUser.setRole(Role.NORMAL_USER);
        testUser = userRepository.save(testUser);

        otherUser = new User();
        otherUser.setEmail("other_reliability@garage.com");
        otherUser.setPasswordHash("hash");
        otherUser.setFullName("Other Tester");
        otherUser.setRole(Role.NORMAL_USER);
        otherUser = userRepository.save(otherUser);

        adminUser = new User();
        adminUser.setEmail("admin_reliability@garage.com");
        adminUser.setPasswordHash("hash");
        adminUser.setFullName("Admin Tester");
        adminUser.setRole(Role.ADMIN);
        adminUser = userRepository.save(adminUser);

        userVehicle = new Vehicle();
        userVehicle.setUser(testUser);
        userVehicle.setCategory(testCategory);
        userVehicle.setMake("Toyota");
        userVehicle.setModel("Innova");
        userVehicle.setYear(2020);
        userVehicle.setPlateNumber("TN07-RELIAB01");
        userVehicle.setFuelType("DIESEL");
        userVehicle.setCurrentOdometer(45000);
        userVehicle = vehicleRepository.save(userVehicle);

        otherVehicle = new Vehicle();
        otherVehicle.setUser(otherUser);
        otherVehicle.setCategory(testCategory);
        otherVehicle.setMake("Hyundai");
        otherVehicle.setModel("Verna");
        otherVehicle.setYear(2021);
        otherVehicle.setPlateNumber("TN07-OTHER99");
        otherVehicle.setFuelType("PETROL");
        otherVehicle.setCurrentOdometer(30000);
        otherVehicle = vehicleRepository.save(otherVehicle);
    }

    private ServiceRecord createService(Vehicle v, LocalDate date, String type, String desc,
                                       double cost, Integer odo, String garage) {
        ServiceRecord sr = new ServiceRecord();
        sr.setVehicle(v);
        sr.setServiceDate(date);
        sr.setServiceType(type);
        sr.setDescription(desc);
        sr.setCost(BigDecimal.valueOf(cost));
        sr.setOdometerAtService(odo);
        sr.setGarageName(garage);
        return serviceRecordRepository.save(sr);
    }

    // 1. Zero service records: returns graceful baseline, VRI 100
    @Test
    void getReliability_zeroRecords_returnsGracefulBaseline() {
        VehicleReliabilityReportDTO report = vehicleReliabilityService.getVehicleReliability(userVehicle.getVehicleId(), testUser.getUserId());

        assertThat(report).isNotNull();
        assertThat(report.getTotalServiceVisits()).isEqualTo(0);
        assertThat(report.getVriScore()).isEqualTo(100);
        assertThat(report.getReliabilityGrade()).isEqualTo("EXCELLENT");
        assertThat(report.getMdbfKm()).isNull();
        assertThat(report.getMtbsDays()).isNull();
        assertThat(report.getServiceAccelerationStatus()).isEqualTo("INSUFFICIENT_DATA");
    }

    // 2. Single record: handles interval gracefully (MDBF/MTBS null)
    @Test
    void getReliability_singleRecord_handlesIntervalGracefully() {
        createService(userVehicle, LocalDate.now().minusMonths(3), "Regular Service", "Oil change and inspection", 2500, 10000, "Apex Auto");

        VehicleReliabilityReportDTO report = vehicleReliabilityService.getVehicleReliability(userVehicle.getVehicleId(), testUser.getUserId());

        assertThat(report.getTotalServiceVisits()).isEqualTo(1);
        assertThat(report.getMdbfKm()).isNull();
        assertThat(report.getMtbsDays()).isNull();
        assertThat(report.getVriScore()).isEqualTo(100);
    }

    // 3. Multiple records: calculates MDBF and MTBS accurately
    @Test
    void getReliability_multipleRecords_calculatesMdbfAndMtbsAccurately() {
        LocalDate base = LocalDate.of(2025, 1, 1);
        createService(userVehicle, base, "Routine PMS", "First service", 3000, 5000, "Speedy Garage");
        createService(userVehicle, base.plusDays(100), "Regular Service", "Oil change", 3500, 15000, "Speedy Garage");
        createService(userVehicle, base.plusDays(200), "Periodic Service", "General maintenance", 4000, 25000, "Speedy Garage");

        VehicleReliabilityReportDTO report = vehicleReliabilityService.getVehicleReliability(userVehicle.getVehicleId(), testUser.getUserId());

        assertThat(report.getTotalServiceVisits()).isEqualTo(3);
        // Average days between: (100 + 100) / 2 = 100.0 days
        assertThat(report.getMtbsDays()).isEqualTo(100.0);
        // Average km between: (10000 + 10000) / 2 = 10000.0 km
        assertThat(report.getMdbfKm()).isEqualTo(10000.0);
    }

    // 4. Null fields handled defensively
    @Test
    void getReliability_nullFields_handledDefensivelyWithoutExceptions() {
        ServiceRecord sr = new ServiceRecord();
        sr.setVehicle(userVehicle);
        sr.setServiceDate(LocalDate.now());
        sr.setServiceType("Inspection");
        sr.setDescription(null);
        sr.setCost(null);
        sr.setOdometerAtService(null);
        sr.setGarageName(null);
        serviceRecordRepository.save(sr);

        VehicleReliabilityReportDTO report = vehicleReliabilityService.getVehicleReliability(userVehicle.getVehicleId(), testUser.getUserId());

        assertThat(report).isNotNull();
        assertThat(report.getTotalServiceVisits()).isEqualTo(1);
        assertThat(report.getTotalServiceSpend()).isEqualTo(new BigDecimal("0.00"));
        assertThat(report.getWorkshops()).hasSize(1);
        assertThat(report.getWorkshops().get(0).getWorkshopName()).isEqualTo("Independent / Unspecified Workshop");
    }

    // 5. Zero / non-monotonic odometers guarded against negative km
    @Test
    void getReliability_nonMonotonicOdometers_ignoredInMdbfCalculation() {
        LocalDate base = LocalDate.of(2025, 1, 1);
        createService(userVehicle, base, "Service 1", "Regular", 1000, 20000, "Shop A");
        // User entered typo in odometer reading (lower than previous)
        createService(userVehicle, base.plusDays(30), "Service 2", "Regular", 1000, 15000, "Shop A");
        createService(userVehicle, base.plusDays(60), "Service 3", "Regular", 1000, 25000, "Shop A");

        VehicleReliabilityReportDTO report = vehicleReliabilityService.getVehicleReliability(userVehicle.getVehicleId(), testUser.getUserId());

        // The step from 20000 -> 15000 is non-monotonic and ignored. Step 15000 -> 25000 is 10000 km.
        assertThat(report.getMdbfKm()).isNotNull();
        assertThat(report.getMdbfKm()).isGreaterThan(0.0);
    }

    // 6. Subsystem classification: Powertrain
    @Test
    void getReliability_subsystemClassification_powertrainIdentified() {
        createService(userVehicle, LocalDate.now(), "Engine Overhaul", "Timing belt replacement and spark plugs", 15000, 40000, "Motor Works");

        VehicleReliabilityReportDTO report = vehicleReliabilityService.getVehicleReliability(userVehicle.getVehicleId(), testUser.getUserId());

        assertThat(report.getSubsystemBreakdowns()).anyMatch(s -> "POWERTRAIN_ENGINE".equals(s.getSubsystem()) && s.getRecordCount() == 1);
    }

    // 7. Subsystem classification: Transmission
    @Test
    void getReliability_subsystemClassification_transmissionIdentified() {
        createService(userVehicle, LocalDate.now(), "Clutch Replacement", "Gearbox clutch plate renewal", 12000, 42000, "Gear Master");

        VehicleReliabilityReportDTO report = vehicleReliabilityService.getVehicleReliability(userVehicle.getVehicleId(), testUser.getUserId());

        assertThat(report.getSubsystemBreakdowns()).anyMatch(s -> "TRANSMISSION_DRIVETRAIN".equals(s.getSubsystem()) && s.getRecordCount() == 1);
    }

    // 8. Subsystem classification: Braking & Tires
    @Test
    void getReliability_subsystemClassification_brakingTiresIdentified() {
        createService(userVehicle, LocalDate.now(), "Brake Service", "Replaced front brake pads and rotors", 4500, 35000, "Brake Point");

        VehicleReliabilityReportDTO report = vehicleReliabilityService.getVehicleReliability(userVehicle.getVehicleId(), testUser.getUserId());

        assertThat(report.getSubsystemBreakdowns()).anyMatch(s -> "BRAKING_TIRES".equals(s.getSubsystem()) && s.getRecordCount() == 1);
    }

    // 9. Subsystem classification: Suspension & Steering
    @Test
    void getReliability_subsystemClassification_suspensionSteeringIdentified() {
        createService(userVehicle, LocalDate.now(), "Suspension Overhaul", "Replaced front shock absorbers and struts", 8000, 38000, "Ride Comfort");

        VehicleReliabilityReportDTO report = vehicleReliabilityService.getVehicleReliability(userVehicle.getVehicleId(), testUser.getUserId());

        assertThat(report.getSubsystemBreakdowns()).anyMatch(s -> "SUSPENSION_STEERING".equals(s.getSubsystem()) && s.getRecordCount() == 1);
    }

    // 10. Subsystem classification: Electrical & Battery
    @Test
    void getReliability_subsystemClassification_electricalBatteryIdentified() {
        createService(userVehicle, LocalDate.now(), "Battery Replacement", "New alternator and battery installed", 6500, 36000, "Electro Hub");

        VehicleReliabilityReportDTO report = vehicleReliabilityService.getVehicleReliability(userVehicle.getVehicleId(), testUser.getUserId());

        assertThat(report.getSubsystemBreakdowns()).anyMatch(s -> "ELECTRICAL_BATTERY".equals(s.getSubsystem()) && s.getRecordCount() == 1);
    }

    // 11. Subsystem classification: HVAC & Body
    @Test
    void getReliability_subsystemClassification_hvacBodyIdentified() {
        createService(userVehicle, LocalDate.now(), "AC Service", "Compressor gas refill and cabin filter", 5000, 37000, "Cool Air Care");

        VehicleReliabilityReportDTO report = vehicleReliabilityService.getVehicleReliability(userVehicle.getVehicleId(), testUser.getUserId());

        assertThat(report.getSubsystemBreakdowns()).anyMatch(s -> "HVAC_BODY_AUXILIARY".equals(s.getSubsystem()) && s.getRecordCount() == 1);
    }

    // 12. Corrective Service Ratio (CSR) calculation
    @Test
    void getReliability_unscheduledVsRoutine_calculatesCsrCorrectly() {
        LocalDate base = LocalDate.of(2025, 1, 1);
        // 1 routine service (2000 cost)
        createService(userVehicle, base, "Routine Maintenance", "Regular scheduled oil change", 2000, 10000, "Shop A");
        // 1 breakdown repair (8000 cost)
        createService(userVehicle, base.plusMonths(2), "Emergency Repair", "Engine broken leak repaired", 8000, 12000, "Shop B");

        VehicleReliabilityReportDTO report = vehicleReliabilityService.getVehicleReliability(userVehicle.getVehicleId(), testUser.getUserId());

        assertThat(report.getRoutineMaintenanceCount()).isEqualTo(1);
        assertThat(report.getUnscheduledBreakdownCount()).isEqualTo(1);
        // Total spend = 10000. Unscheduled = 8000. CSR = 80.0%
        assertThat(report.getCorrectiveServiceRatio()).isEqualTo(80.0);
    }

    // 13. Chronic defect detected when same subsystem recurs within 180 days
    @Test
    void getReliability_chronicDefect_detectsRecurringSubsystemIssues() {
        LocalDate base = LocalDate.of(2025, 1, 1);
        createService(userVehicle, base, "Brake Repair", "Front brake pad noise", 3000, 10000, "Brake Hub");
        createService(userVehicle, base.plusDays(45), "Brake Inspection", "Brake caliper sticking and rotor repair", 5000, 12000, "Brake Hub");

        VehicleReliabilityReportDTO report = vehicleReliabilityService.getVehicleReliability(userVehicle.getVehicleId(), testUser.getUserId());

        assertThat(report.getChronicDefectAlerts()).isNotEmpty();
        assertThat(report.getChronicDefectAlerts().get(0).getSubsystem()).isEqualTo("BRAKING_TIRES");
        assertThat(report.getChronicDefectAlerts().get(0).getRecurringCount()).isEqualTo(2);
        assertThat(report.getChronicDefectAlerts().get(0).getSeverity()).isEqualTo("MODERATE");
    }

    // 14. Distant visits not flagged as chronic
    @Test
    void getReliability_chronicDefect_distantVisitsNotFlaggedAsChronic() {
        LocalDate base = LocalDate.of(2024, 1, 1);
        createService(userVehicle, base, "Brake Repair", "Front brake pad wear", 3000, 10000, "Brake Hub");
        // 300 days and 15000 km later (normal wear, not chronic defect)
        createService(userVehicle, base.plusDays(300), "Brake Service", "Rear brake pad replacement", 3500, 25000, "Brake Hub");

        VehicleReliabilityReportDTO report = vehicleReliabilityService.getVehicleReliability(userVehicle.getVehicleId(), testUser.getUserId());

        assertThat(report.getChronicDefectAlerts()).isEmpty();
    }

    // 15. Clean vehicle with regular PMS yields high VRI score (>= 90)
    @Test
    void getReliability_vriScoring_cleanVehicleYieldsHighScore() {
        LocalDate base = LocalDate.of(2024, 1, 1);
        createService(userVehicle, base, "Periodic Maintenance", "Scheduled routine checkup", 3000, 10000, "Authorized Dealer");
        createService(userVehicle, base.plusDays(180), "Scheduled Service", "Routine oil change and inspection", 3200, 20000, "Authorized Dealer");
        createService(userVehicle, base.plusDays(360), "Annual Service", "Minor fluids service", 3500, 30000, "Authorized Dealer");

        VehicleReliabilityReportDTO report = vehicleReliabilityService.getVehicleReliability(userVehicle.getVehicleId(), testUser.getUserId());

        assertThat(report.getVriScore()).isGreaterThanOrEqualTo(90);
        assertThat(report.getReliabilityGrade()).isEqualTo("EXCELLENT");
    }

    // 16. Vehicle with frequent breakdowns and chronic issues yields low VRI score (< 60)
    @Test
    void getReliability_vriScoring_highFailuresYieldsLowScore() {
        LocalDate base = LocalDate.of(2025, 1, 1);
        createService(userVehicle, base, "Engine Breakdown", "Engine overheating repair leak", 15000, 10000, "Emergency Shop");
        createService(userVehicle, base.plusDays(20), "Coolant Failure", "Engine radiator broken leak", 12000, 11000, "Emergency Shop");
        createService(userVehicle, base.plusDays(40), "Electrical Breakdown", "Battery dead alternator failed", 8000, 12000, "Emergency Shop");

        VehicleReliabilityReportDTO report = vehicleReliabilityService.getVehicleReliability(userVehicle.getVehicleId(), testUser.getUserId());

        assertThat(report.getVriScore()).isLessThan(60);
        assertThat(report.getReliabilityGrade()).isIn("POOR", "CRITICAL_RISK");
    }

    // 17. VRI scoring bounds: strictly clamped between 0 and 100
    @Test
    void getReliability_vriScoring_boundsStrictlyBetweenZeroAnd100() {
        LocalDate base = LocalDate.of(2025, 1, 1);
        // Inject extreme multiple chronic failures
        for (int i = 0; i < 6; i++) {
            createService(userVehicle, base.plusDays(i * 10), "Engine Breakdown", "Engine failure leak broken", 20000, 10000 + (i * 500), "Shop");
        }

        VehicleReliabilityReportDTO report = vehicleReliabilityService.getVehicleReliability(userVehicle.getVehicleId(), testUser.getUserId());

        assertThat(report.getVriScore()).isBetween(0, 100);
    }

    // 18. Aging resilience bonus awarded for older vehicle maintaining low CSR
    @Test
    void getReliability_vriScoring_agingResilienceBonusAwarded() {
        userVehicle.setYear(2017); // 9 years old
        userVehicle.setCurrentOdometer(120000);
        vehicleRepository.save(userVehicle);

        LocalDate base = LocalDate.of(2024, 1, 1);
        createService(userVehicle, base, "Scheduled Maintenance", "Regular service", 2500, 105000, "Shop");
        createService(userVehicle, base.plusDays(180), "Scheduled Maintenance", "Regular service", 2500, 115000, "Shop");

        VehicleReliabilityReportDTO report = vehicleReliabilityService.getVehicleReliability(userVehicle.getVehicleId(), testUser.getUserId());

        assertThat(report.getVriScore()).isGreaterThanOrEqualTo(90);
    }

    // 19. Workshop analytics: aggregates visits, spend, and average cost
    @Test
    void getReliability_workshopAnalytics_aggregatesVisitsAndCosts() {
        createService(userVehicle, LocalDate.of(2025, 1, 1), "Service 1", "Checkup", 4000, 10000, "Bosch Car Service");
        createService(userVehicle, LocalDate.of(2025, 4, 1), "Service 2", "Repair", 6000, 15000, "Bosch Car Service");
        createService(userVehicle, LocalDate.of(2025, 8, 1), "Service 3", "Tire rotation", 2000, 20000, "City Tyre Hub");

        VehicleReliabilityReportDTO report = vehicleReliabilityService.getVehicleReliability(userVehicle.getVehicleId(), testUser.getUserId());

        assertThat(report.getWorkshops()).hasSize(2);
        // Bosch should have 2 visits, total spend 10000, avg 5000
        assertThat(report.getWorkshops().get(0).getWorkshopName()).isEqualTo("Bosch Car Service");
        assertThat(report.getWorkshops().get(0).getVisitCount()).isEqualTo(2);
        assertThat(report.getWorkshops().get(0).getTotalSpend()).isEqualByComparingTo("10000.00");
        assertThat(report.getWorkshops().get(0).getAverageCostPerVisit()).isEqualByComparingTo("5000.00");
    }

    // 20. Workshop Mean Return Interval (MRI) calculation
    @Test
    void getReliability_workshopAnalytics_calculatesReturnInterval() {
        createService(userVehicle, LocalDate.of(2025, 1, 1), "Service 1", "Checkup", 4000, 10000, "Dealer Garage");
        createService(userVehicle, LocalDate.of(2025, 3, 2), "Service 2", "Repair", 6000, 15000, "Local Shop");

        VehicleReliabilityReportDTO report = vehicleReliabilityService.getVehicleReliability(userVehicle.getVehicleId(), testUser.getUserId());

        // Dealer Garage had visit on Jan 1, next service was Mar 2 (60 days later)
        assertThat(report.getWorkshops().stream().filter(w -> "Dealer Garage".equals(w.getWorkshopName())).findFirst())
                .hasValueSatisfying(w -> assertThat(w.getMeanReturnIntervalDays()).isEqualTo(60.0));
    }

    // 21. Service Acceleration: detects deteriorating/worsening frequency
    @Test
    void getReliability_serviceAcceleration_detectsWorseningIntervals() {
        LocalDate base = LocalDate.of(2024, 1, 1);
        // Early intervals: 120 days apart
        createService(userVehicle, base, "Routine PMS", "Checkup", 2000, 10000, "Shop");
        createService(userVehicle, base.plusDays(120), "Routine PMS", "Checkup", 2000, 15000, "Shop");
        // Recent intervals: 25 days apart (rapidly accelerating breakdown frequency)
        createService(userVehicle, base.plusDays(145), "Repair", "Broken part", 5000, 16000, "Shop");
        createService(userVehicle, base.plusDays(170), "Repair", "Broken part", 6000, 17000, "Shop");

        VehicleReliabilityReportDTO report = vehicleReliabilityService.getVehicleReliability(userVehicle.getVehicleId(), testUser.getUserId());

        assertThat(report.getServiceAccelerationStatus()).isEqualTo("ACCELERATING");
    }

    // 22. Garage portfolio matrix: multiple vehicles ranked correctly on leaderboard
    @Test
    void getGarageReliability_multipleVehicles_ranksLeaderboardCorrectly() {
        // userVehicle has 0 records -> VRI 100
        // Create second vehicle for testUser with heavy breakdown records
        Vehicle v2 = new Vehicle();
        v2.setUser(testUser);
        v2.setCategory(testCategory);
        v2.setMake("Ford");
        v2.setModel("EcoSport");
        v2.setYear(2019);
        v2.setPlateNumber("TN07-FORD88");
        v2.setFuelType("DIESEL");
        v2 = vehicleRepository.save(v2);

        LocalDate base = LocalDate.of(2025, 1, 1);
        createService(v2, base, "Breakdown", "Engine broken leak", 15000, 10000, "Shop");
        createService(v2, base.plusDays(15), "Breakdown", "Engine broken leak repair", 18000, 10500, "Shop");

        GarageReliabilityMatrixDTO matrix = vehicleReliabilityService.getGarageReliabilityMatrix(testUser.getUserId());

        assertThat(matrix).isNotNull();
        assertThat(matrix.getVehicleSummaries()).hasSize(2);
        // Most reliable should be userVehicle (VRI 100)
        assertThat(matrix.getMostReliableVehicle().getVehicleId()).isEqualTo(userVehicle.getVehicleId());
        // Highest risk should be v2
        assertThat(matrix.getHighestRiskVehicle().getVehicleId()).isEqualTo(v2.getVehicleId());
        assertThat(matrix.getRecommendations()).isNotEmpty();
    }

    // 23. Web MVC: Owner access returns HTTP 200 OK
    @Test
    @WithMockUser(username = "reliability_user@garage.com", roles = "NORMAL_USER")
    void webGetVehicleReliability_ownerAccess_renders200Ok() throws Exception {
        mockMvc.perform(get("/vehicles/{id}/reliability", userVehicle.getVehicleId()))
                .andExpect(status().isOk())
                .andExpect(view().name("vehicle/reliability"))
                .andExpect(model().attributeExists("report", "vehicle", "user"));
    }

    // 24. Web MVC & REST: Cross-user tampering redirects with error (Web) or returns 403 (REST)
    @Test
    @WithMockUser(username = "reliability_user@garage.com", roles = "NORMAL_USER")
    void tampering_crossUserAttempt_blockedAppropriately() throws Exception {
        // Web MVC cross-user tamper -> redirects to /vehicles with flash error
        mockMvc.perform(get("/vehicles/{id}/reliability", otherVehicle.getVehicleId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/vehicles"))
                .andExpect(flash().attributeExists("errorMsg"));

        // REST API cross-user tamper -> returns 403 Forbidden
        mockMvc.perform(get("/api/vehicles/{id}/reliability", otherVehicle.getVehicleId()))
                .andExpect(status().isForbidden());
    }

    // 25. Security: ROLE_ADMIN strictly blocked with HTTP 403 Forbidden from personal vehicle reliability
    @Test
    @WithMockUser(username = "admin_reliability@garage.com", roles = "ADMIN")
    void security_adminAccess_blockedWith403Forbidden() throws Exception {
        // Admin blocked from Web MVC route
        mockMvc.perform(get("/vehicles/{id}/reliability", userVehicle.getVehicleId()))
                .andExpect(status().isForbidden());

        // Admin blocked from REST API route
        mockMvc.perform(get("/api/vehicles/{id}/reliability", userVehicle.getVehicleId()))
                .andExpect(status().isForbidden());

        // Admin blocked from garage reliability REST API
        mockMvc.perform(get("/api/analytics/garage-reliability"))
                .andExpect(status().isForbidden());
    }
}
