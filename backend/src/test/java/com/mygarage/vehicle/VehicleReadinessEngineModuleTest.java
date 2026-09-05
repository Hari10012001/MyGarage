package com.mygarage.vehicle;

import com.mygarage.dto.response.*;
import com.mygarage.model.*;
import com.mygarage.model.enums.FuelType;
import com.mygarage.model.enums.MaintenanceStatus;
import com.mygarage.model.enums.Role;
import com.mygarage.repository.*;
import com.mygarage.service.VehicleReadinessService;
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
class VehicleReadinessEngineModuleTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VehicleReadinessService vehicleReadinessService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private VehicleCategoryRepository vehicleCategoryRepository;

    @Autowired
    private ServiceRecordRepository serviceRecordRepository;

    @Autowired
    private MaintenanceRecordRepository maintenanceRecordRepository;

    @Autowired
    private FuelRecordRepository fuelRecordRepository;

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
        testUser.setEmail("readiness_user@garage.com");
        testUser.setPasswordHash("hash");
        testUser.setFullName("Readiness Tester");
        testUser.setRole(Role.NORMAL_USER);
        testUser = userRepository.save(testUser);

        otherUser = new User();
        otherUser.setEmail("other_readiness@garage.com");
        otherUser.setPasswordHash("hash");
        otherUser.setFullName("Other Tester");
        otherUser.setRole(Role.NORMAL_USER);
        otherUser = userRepository.save(otherUser);

        adminUser = new User();
        adminUser.setEmail("admin_readiness@garage.com");
        adminUser.setPasswordHash("hash");
        adminUser.setFullName("Admin Tester");
        adminUser.setRole(Role.ADMIN);
        adminUser = userRepository.save(adminUser);

        userVehicle = new Vehicle();
        userVehicle.setUser(testUser);
        userVehicle.setCategory(testCategory);
        userVehicle.setMake("Toyota");
        userVehicle.setModel("Camry");
        userVehicle.setYear(2021);
        userVehicle.setPlateNumber("TN09-READY01");
        userVehicle.setFuelType("PETROL");
        userVehicle.setCurrentOdometer(30000);
        userVehicle = vehicleRepository.save(userVehicle);

        otherVehicle = new Vehicle();
        otherVehicle.setUser(otherUser);
        otherVehicle.setCategory(testCategory);
        otherVehicle.setMake("Hyundai");
        otherVehicle.setModel("Verna");
        otherVehicle.setYear(2019);
        otherVehicle.setPlateNumber("TN09-OTHER01");
        otherVehicle.setFuelType("PETROL");
        otherVehicle.setCurrentOdometer(50000);
        otherVehicle = vehicleRepository.save(otherVehicle);
    }

    private ServiceRecord createService(Vehicle v, String type, String desc, int odo, LocalDate date) {
        ServiceRecord sr = new ServiceRecord();
        sr.setVehicle(v);
        sr.setServiceType(type);
        sr.setDescription(desc);
        sr.setOdometerAtService(odo);
        sr.setServiceDate(date);
        sr.setCost(BigDecimal.valueOf(150.00));
        return serviceRecordRepository.save(sr);
    }

    private MaintenanceRecord createMaintenance(Vehicle v, String title, LocalDate schedDate, MaintenanceStatus status) {
        MaintenanceRecord mr = new MaintenanceRecord();
        mr.setVehicle(v);
        mr.setTitle(title);
        mr.setDescription("Test maintenance task");
        mr.setScheduledDate(schedDate);
        mr.setStatus(status);
        mr.setCost(BigDecimal.valueOf(80.00));
        return maintenanceRecordRepository.save(mr);
    }

    private FuelRecord createFuel(Vehicle v, LocalDate date, double qty, double costPerL, double kmpl) {
        FuelRecord fr = new FuelRecord();
        fr.setVehicle(v);
        fr.setFuelDate(date);
        fr.setFuelType(FuelType.PETROL);
        fr.setQuantityLitres(BigDecimal.valueOf(qty));
        fr.setCostPerLitre(BigDecimal.valueOf(costPerL).setScale(2, java.math.RoundingMode.HALF_UP));
        fr.setTotalCost(BigDecimal.valueOf(qty * costPerL).setScale(2, java.math.RoundingMode.HALF_UP));
        fr.setEstimatedMileageKmpl(BigDecimal.valueOf(kmpl));
        return fuelRecordRepository.save(fr);
    }

    // 1. Mission Ready vehicle returns high TRI
    @Test
    void testEvaluateReadiness_MissionReadyVehicle_ReturnsHighTri() {
        createService(userVehicle, "Full Synthetic Oil Service", "Engine oil and filter change", 29000, LocalDate.now().minusDays(10));
        createService(userVehicle, "Brake Pad Replacement", "Front and rear brake pads", 28000, LocalDate.now().minusDays(20));
        createService(userVehicle, "Coolant Flush", "Cooling system radiator flush", 27000, LocalDate.now().minusDays(30));
        createService(userVehicle, "Tire Rotation & Balancing", "All tires rotated and aligned", 29500, LocalDate.now().minusDays(5));
        createFuel(userVehicle, LocalDate.now().minusDays(2), 40.0, 1.50, 14.5);

        VehicleTripReadinessReportDTO report = vehicleReadinessService.evaluateVehicleReadiness(
                userVehicle.getVehicleId(), 300.0, 1, "HIGHWAY_CRUISE", testUser.getUserId()
        );

        assertThat(report).isNotNull();
        assertThat(report.tripReadinessIndex()).isGreaterThanOrEqualTo(90);
        assertThat(report.readinessBand()).isEqualTo("MISSION_READY");
        assertThat(report.hasMidTripBreach()).isFalse();
        assertThat(report.analyticalDisclaimer()).contains("NOT an engineering or mechanical safety guarantee");
    }

    // 2. Overdue maintenance applies maintenance penalty
    @Test
    void testEvaluateReadiness_OverdueMaintenance_AppliesMaintenancePenalty() {
        createMaintenance(userVehicle, "Transmission Fluid Flush", LocalDate.now().minusDays(15), MaintenanceStatus.OVERDUE);

        VehicleTripReadinessReportDTO report = vehicleReadinessService.evaluateVehicleReadiness(
                userVehicle.getVehicleId(), 200.0, 1, "HIGHWAY_CRUISE", testUser.getUserId()
        );

        assertThat(report.tripReadinessIndex()).isLessThan(90);
        assertThat(report.criticalActionCount()).isGreaterThanOrEqualTo(1);
        assertThat(report.checklist()).anyMatch(c -> c.severity().equals("CRITICAL") && c.title().contains("Overdue"));
    }

    // 3. Mid-trip interval breach detects exact breach km
    @Test
    void testEvaluateReadiness_MidTripIntervalBreach_DetectsExactBreachKm() {
        // Oil interval benchmark is 10,000 km.
        // Last serviced at 22,000 km. Current is 30,000 km. Km since = 8,000 km. Remaining margin = 2,000 km.
        createService(userVehicle, "Oil Change", "Engine oil", 22000, LocalDate.now().minusDays(60));

        // Trip is 3,000 km -> will exceed 2,000 km margin at km 2,000!
        VehicleTripReadinessReportDTO report = vehicleReadinessService.evaluateVehicleReadiness(
                userVehicle.getVehicleId(), 3000.0, 3, "HIGHWAY_CRUISE", testUser.getUserId()
        );

        assertThat(report.hasMidTripBreach()).isTrue();
        assertThat(report.midTripBreachAlerts()).anyMatch(a -> a.contains("Engine Oil") && a.contains("km 2000"));

        ConsumableReserveMarginDTO oilMargin = report.consumableMargins().stream()
                .filter(cm -> cm.subsystemName().contains("Oil"))
                .findFirst().orElseThrow();

        assertThat(oilMargin.willBreachMidTrip()).isTrue();
        assertThat(oilMargin.breachAtTripKm()).isEqualTo(2000);
        assertThat(oilMargin.statusBand()).isEqualTo("BREACHED");
    }

    // 4. No breach when trip distance is short
    @Test
    void testEvaluateReadiness_NoBreach_WhenTripDistanceIsShort() {
        createService(userVehicle, "Oil Change", "Engine oil", 22000, LocalDate.now().minusDays(60));

        // Trip is only 500 km -> fits within 2,000 km margin
        VehicleTripReadinessReportDTO report = vehicleReadinessService.evaluateVehicleReadiness(
                userVehicle.getVehicleId(), 500.0, 1, "HIGHWAY_CRUISE", testUser.getUserId()
        );

        ConsumableReserveMarginDTO oilMargin = report.consumableMargins().stream()
                .filter(cm -> cm.subsystemName().contains("Oil"))
                .findFirst().orElseThrow();

        assertThat(oilMargin.willBreachMidTrip()).isFalse();
        assertThat(oilMargin.postTripMarginKm()).isEqualTo(1500);
    }

    // 5. Consumable margins engine oil calculated accurately
    @Test
    void testEvaluateReadiness_ConsumableMargins_EngineOilCalculatedAccurately() {
        createService(userVehicle, "Oil Change", "Synthetic motor oil", 25000, LocalDate.now().minusDays(40));

        VehicleTripReadinessReportDTO report = vehicleReadinessService.evaluateVehicleReadiness(
                userVehicle.getVehicleId(), 400.0, 2, "MIXED_BALANCED", testUser.getUserId()
        );

        ConsumableReserveMarginDTO oilMargin = report.consumableMargins().stream()
                .filter(cm -> cm.subsystemName().contains("Oil"))
                .findFirst().orElseThrow();

        assertThat(oilMargin.kmSinceLastService()).isEqualTo(5000);
        assertThat(oilMargin.remainingMarginKm()).isEqualTo(5000);
        assertThat(oilMargin.remainingMarginPercent()).isEqualTo(50.0);
    }

    // 6. Consumable margins for Brakes, Coolant, Tires computed
    @Test
    void testEvaluateReadiness_ConsumableMargins_BrakesCoolantTiresComputed() {
        VehicleTripReadinessReportDTO report = vehicleReadinessService.evaluateVehicleReadiness(
                userVehicle.getVehicleId(), 500.0, 2, "HIGHWAY_CRUISE", testUser.getUserId()
        );

        assertThat(report.consumableMargins()).hasSize(4);
        assertThat(report.consumableMargins()).extracting(ConsumableReserveMarginDTO::subsystemName)
                .containsExactlyInAnyOrder(
                        "Engine Oil & Filter",
                        "Braking System",
                        "Cooling System & Fluids",
                        "Tires & Suspension"
                );
    }

    // 7. Fuel staging highway regime applies discount
    @Test
    void testEvaluateReadiness_FuelStaging_HighwayRegimeAppliesDiscount() {
        createFuel(userVehicle, LocalDate.now().minusDays(3), 40.0, 1.50, 10.0); // 10 km/L = 10.0 L/100km

        VehicleTripReadinessReportDTO highwayReport = vehicleReadinessService.evaluateVehicleReadiness(
                userVehicle.getVehicleId(), 1000.0, 2, "HIGHWAY_CRUISE", testUser.getUserId()
        );

        // 10.0 * 0.90 = 9.0 L/100km
        assertThat(highwayReport.fuelStaging().estimatedConsumptionPer100Km()).isEqualTo(9.0);
        assertThat(highwayReport.fuelStaging().estimatedFuelNeededLiters()).isEqualTo(90.0);
    }

    // 8. Fuel staging city regime applies penalty
    @Test
    void testEvaluateReadiness_FuelStaging_CityRegimeAppliesPenalty() {
        createFuel(userVehicle, LocalDate.now().minusDays(3), 40.0, 1.50, 10.0); // 10.0 L/100km

        VehicleTripReadinessReportDTO cityReport = vehicleReadinessService.evaluateVehicleReadiness(
                userVehicle.getVehicleId(), 1000.0, 2, "CITY_CONGESTED", testUser.getUserId()
        );

        // 10.0 * 1.15 = 11.5 L/100km
        assertThat(cityReport.fuelStaging().estimatedConsumptionPer100Km()).isEqualTo(11.5);
        assertThat(cityReport.fuelStaging().estimatedFuelNeededLiters()).isEqualTo(115.0);
    }

    // 9. Fuel staging calculates accurate trip cost
    @Test
    void testEvaluateReadiness_FuelStaging_CalculatesAccurateTripCost() {
        createFuel(userVehicle, LocalDate.now().minusDays(3), 50.0, 2.00, 10.0); // 10.0 L/100km @ $2.00/L

        VehicleTripReadinessReportDTO report = vehicleReadinessService.evaluateVehicleReadiness(
                userVehicle.getVehicleId(), 500.0, 1, "MIXED_BALANCED", testUser.getUserId()
        );

        // 500 km @ 10 L/100km = 50.0 L * $2.00 = $100.00
        assertThat(report.fuelStaging().estimatedFuelNeededLiters()).isEqualTo(50.0);
        assertThat(report.fuelStaging().estimatedFuelCost()).isEqualByComparingTo(BigDecimal.valueOf(100.00));
    }

    // 10. Cruising range estimates minimum fuel stops
    @Test
    void testEvaluateReadiness_CruisingRange_EstimatesFuelStops() {
        createFuel(userVehicle, LocalDate.now().minusDays(3), 40.0, 1.50, 10.0); // 10 L/100km

        // 2000 km trip on ~50L tank (range ~500 km, safe range ~425 km) -> requires at least 3-4 stops
        VehicleTripReadinessReportDTO report = vehicleReadinessService.evaluateVehicleReadiness(
                userVehicle.getVehicleId(), 2000.0, 3, "HIGHWAY_CRUISE", testUser.getUserId()
        );

        assertThat(report.fuelStaging().estimatedFuelStopsRequired()).isGreaterThanOrEqualTo(3);
    }

    // 11. Pre-trip checklist generates critical items
    @Test
    void testEvaluateReadiness_PreTripChecklist_GeneratesCriticalItems() {
        createMaintenance(userVehicle, "Critical Brake Rotor Check", LocalDate.now().minusDays(2), MaintenanceStatus.OVERDUE);

        VehicleTripReadinessReportDTO report = vehicleReadinessService.evaluateVehicleReadiness(
                userVehicle.getVehicleId(), 500.0, 1, "HIGHWAY_CRUISE", testUser.getUserId()
        );

        assertThat(report.checklist()).anyMatch(c -> c.severity().equals("CRITICAL") && c.isActionRequired());
    }

    // 12. Pre-trip checklist generates passed items
    @Test
    void testEvaluateReadiness_PreTripChecklist_GeneratesPassedItems() {
        createService(userVehicle, "Engine Oil", "Oil service", 29500, LocalDate.now().minusDays(2));

        VehicleTripReadinessReportDTO report = vehicleReadinessService.evaluateVehicleReadiness(
                userVehicle.getVehicleId(), 200.0, 1, "HIGHWAY_CRUISE", testUser.getUserId()
        );

        assertThat(report.checklist()).anyMatch(c -> c.severity().equals("PASSED") && c.title().contains("Oil"));
    }

    // 13. High mileage and vehicle age apply stress penalty
    @Test
    void testEvaluateReadiness_HighMileageAgeStress_AppliesPenalties() {
        Vehicle vintageVehicle = new Vehicle();
        vintageVehicle.setUser(testUser);
        vintageVehicle.setCategory(testCategory);
        vintageVehicle.setMake("Old");
        vintageVehicle.setModel("Cruiser");
        vintageVehicle.setYear(2005); // 20+ years old
        vintageVehicle.setPlateNumber("TN09-VINT01");
        vintageVehicle.setCurrentOdometer(250000); // > 200k km
        vintageVehicle = vehicleRepository.save(vintageVehicle);

        VehicleTripReadinessReportDTO report = vehicleReadinessService.evaluateVehicleReadiness(
                vintageVehicle.getVehicleId(), 1500.0, 4, "HIGHWAY_CRUISE", testUser.getUserId()
        );

        // Should receive both age stress (>10y & >1000km) and high mileage stress (>200k km)
        assertThat(report.tripReadinessIndex()).isLessThanOrEqualTo(87);
    }

    // 14. TRI score clamped between 0 and 100
    @Test
    void testEvaluateReadiness_TriScore_ClampedBetweenZeroAndOneHundred() {
        // Massive penalties: multiple overdue tasks
        for (int i = 0; i < 5; i++) {
            createMaintenance(userVehicle, "Severe Overdue " + i, LocalDate.now().minusDays(20 + i), MaintenanceStatus.OVERDUE);
        }

        VehicleTripReadinessReportDTO report = vehicleReadinessService.evaluateVehicleReadiness(
                userVehicle.getVehicleId(), 5000.0, 10, "MOUNTAIN_SEVERE", testUser.getUserId()
        );

        assertThat(report.tripReadinessIndex()).isGreaterThanOrEqualTo(0);
        assertThat(report.tripReadinessIndex()).isLessThanOrEqualTo(100);
        assertThat(report.readinessBand()).isEqualTo("HIGH_RISK");
    }

    // 15. Fleet dispatch selects optimal vehicle accurately
    @Test
    void testEvaluateFleetDispatch_SelectsOptimalVehicleAccurately() {
        // userVehicle is healthy
        createService(userVehicle, "Synthetic Oil", "Oil and filter", 29000, LocalDate.now().minusDays(5));
        createFuel(userVehicle, LocalDate.now().minusDays(2), 40.0, 1.40, 15.0);

        // Add a second vehicle for testUser with overdue maintenance
        Vehicle secondVehicle = new Vehicle();
        secondVehicle.setUser(testUser);
        secondVehicle.setCategory(testCategory);
        secondVehicle.setMake("Honda");
        secondVehicle.setModel("Civic");
        secondVehicle.setYear(2018);
        secondVehicle.setPlateNumber("TN09-CIVIC01");
        secondVehicle.setCurrentOdometer(80000);
        secondVehicle = vehicleRepository.save(secondVehicle);
        createMaintenance(secondVehicle, "Overdue Timing Belt", LocalDate.now().minusDays(10), MaintenanceStatus.OVERDUE);

        GarageFleetDispatchReportDTO dispatch = vehicleReadinessService.evaluateFleetDispatch(
                600.0, 2, "HIGHWAY_CRUISE", testUser.getUserId()
        );

        assertThat(dispatch.candidates()).hasSize(2);
        assertThat(dispatch.optimalVehicleId()).isEqualTo(userVehicle.getVehicleId());
        assertThat(dispatch.candidates().get(0).dispatchRecommendation()).isEqualTo("OPTIMAL_CHOICE");
    }

    // 16. Fleet dispatch marks high-risk vehicles correctly
    @Test
    void testEvaluateFleetDispatch_MarksHighRiskVehiclesCorrectly() {
        createMaintenance(userVehicle, "Urgent Brake Pad Change", LocalDate.now().minusDays(5), MaintenanceStatus.OVERDUE);

        GarageFleetDispatchReportDTO dispatch = vehicleReadinessService.evaluateFleetDispatch(
                800.0, 2, "HIGHWAY_CRUISE", testUser.getUserId()
        );

        assertThat(dispatch.candidates()).anyMatch(c -> c.dispatchRecommendation().equals("HIGH_RISK"));
    }

    // 17. Fleet dispatch on empty garage returns graceful empty report
    @Test
    void testEvaluateFleetDispatch_EmptyGarage_ReturnsGracefulEmptyReport() {
        User emptyUser = new User();
        emptyUser.setEmail("empty_garage@garage.com");
        emptyUser.setPasswordHash("hash");
        emptyUser.setFullName("Empty Garage User");
        emptyUser.setRole(Role.NORMAL_USER);
        emptyUser = userRepository.save(emptyUser);

        GarageFleetDispatchReportDTO dispatch = vehicleReadinessService.evaluateFleetDispatch(
                500.0, 2, "HIGHWAY_CRUISE", emptyUser.getUserId()
        );

        assertThat(dispatch.totalActiveVehiclesEvaluated()).isEqualTo(0);
        assertThat(dispatch.candidates()).isEmpty();
        assertThat(dispatch.optimalVehicleId()).isNull();
    }

    // 18. Zero records applies conservative defaults safely
    @Test
    void testEvaluateReadiness_ZeroRecords_AppliesConservativeDefaults() {
        Vehicle blankVehicle = new Vehicle();
        blankVehicle.setUser(testUser);
        blankVehicle.setCategory(testCategory);
        blankVehicle.setMake("Nissan");
        blankVehicle.setModel("Sunny");
        blankVehicle.setYear(2022);
        blankVehicle.setPlateNumber("TN09-BLANK01");
        blankVehicle.setCurrentOdometer(10000);
        blankVehicle = vehicleRepository.save(blankVehicle);

        VehicleTripReadinessReportDTO report = vehicleReadinessService.evaluateVehicleReadiness(
                blankVehicle.getVehicleId(), 500.0, 2, "HIGHWAY_CRUISE", testUser.getUserId()
        );

        assertThat(report).isNotNull();
        assertThat(report.hasHistoricalRecords()).isFalse();
        assertThat(report.fuelStaging().isHistoricalDataAvailable()).isFalse();
    }

    // 19. Parameter boundary clamps negative distance
    @Test
    void testEvaluateReadiness_ParameterBoundary_ClampsNegativeDistance() {
        VehicleTripReadinessReportDTO report = vehicleReadinessService.evaluateVehicleReadiness(
                userVehicle.getVehicleId(), -150.0, 2, "HIGHWAY_CRUISE", testUser.getUserId()
        );

        assertThat(report.tripDistanceKm()).isEqualTo(10.0);
    }

    // 20. Parameter boundary clamps excessive distance
    @Test
    void testEvaluateReadiness_ParameterBoundary_ClampsExcessiveDistance() {
        VehicleTripReadinessReportDTO report = vehicleReadinessService.evaluateVehicleReadiness(
                userVehicle.getVehicleId(), 50000.0, 2, "HIGHWAY_CRUISE", testUser.getUserId()
        );

        assertThat(report.tripDistanceKm()).isEqualTo(10000.0);
    }

    // 21. Web MVC unauthorized access redirects with flash error
    @Test
    @WithMockUser(username = "readiness_user@garage.com", roles = {"NORMAL_USER"})
    void testSecurity_WebMvc_UnauthorizedAccess_RedirectsWithFlashError() throws Exception {
        mockMvc.perform(get("/vehicles/" + otherVehicle.getVehicleId() + "/readiness"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/vehicles"))
                .andExpect(flash().attributeExists("errorMsg"));
    }

    // 22. Web MVC Admin access blocked (throws 403 Forbidden)
    @Test
    @WithMockUser(username = "admin_readiness@garage.com", roles = {"ADMIN"})
    void testSecurity_WebMvc_AdminAccess_Throws403() throws Exception {
        mockMvc.perform(get("/vehicles/" + userVehicle.getVehicleId() + "/readiness"))
                .andExpect(status().isForbidden());
    }

    // 23. REST API unauthorized access returns 403 Forbidden
    @Test
    @WithMockUser(username = "readiness_user@garage.com", roles = {"NORMAL_USER"})
    void testSecurity_RestApi_UnauthorizedAccess_Returns403Forbidden() throws Exception {
        mockMvc.perform(get("/api/vehicles/" + otherVehicle.getVehicleId() + "/readiness"))
                .andExpect(status().isForbidden());
    }

    // 24. REST API Admin access returns 403 Forbidden
    @Test
    @WithMockUser(username = "admin_readiness@garage.com", roles = {"ADMIN"})
    void testSecurity_RestApi_AdminAccess_Returns403Forbidden() throws Exception {
        mockMvc.perform(get("/api/vehicles/" + userVehicle.getVehicleId() + "/readiness"))
                .andExpect(status().isForbidden());
    }

    // 25. REST API owner access returns 200 OK with report
    @Test
    @WithMockUser(username = "readiness_user@garage.com", roles = {"NORMAL_USER"})
    void testSecurity_RestApi_OwnerAccess_Returns200OkWithReport() throws Exception {
        mockMvc.perform(get("/api/vehicles/" + userVehicle.getVehicleId() + "/readiness")
                        .param("tripDistanceKm", "600.0")
                        .param("tripDays", "2")
                        .param("drivingRegime", "HIGHWAY_CRUISE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicleId").value(userVehicle.getVehicleId()))
                .andExpect(jsonPath("$.tripDistanceKm").value(600.0))
                .andExpect(jsonPath("$.tripReadinessIndex").isNumber())
                .andExpect(jsonPath("$.readinessBand").isString())
                .andExpect(jsonPath("$.analyticalDisclaimer").exists());
    }
}
