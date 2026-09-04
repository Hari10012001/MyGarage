package com.mygarage.fuel;

import com.mygarage.dto.response.GarageFuelIntelligenceDTO;
import com.mygarage.dto.response.VehicleFuelAnalyticsDTO;
import com.mygarage.model.FuelRecord;
import com.mygarage.model.User;
import com.mygarage.model.Vehicle;
import com.mygarage.model.enums.FuelType;
import com.mygarage.model.enums.Role;
import com.mygarage.repository.FuelRecordRepository;
import com.mygarage.repository.UserRepository;
import com.mygarage.repository.VehicleRepository;
import com.mygarage.service.FuelAnalyticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
class FuelAnalyticsModuleTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FuelAnalyticsService fuelAnalyticsService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private com.mygarage.repository.VehicleCategoryRepository vehicleCategoryRepository;

    @Autowired
    private FuelRecordRepository fuelRecordRepository;

    private User testUser;
    private User adminUser;
    private User otherUser;
    private Vehicle userVehicle;
    private Vehicle otherVehicle;
    private com.mygarage.model.VehicleCategory testCategory;

    @BeforeEach
    void setUp() {
        testCategory = new com.mygarage.model.VehicleCategory();
        testCategory.setName("Category_" + java.util.UUID.randomUUID().toString().substring(0, 8));
        testCategory.setIcon("car-front");
        testCategory = vehicleCategoryRepository.save(testCategory);

        testUser = new User();
        testUser.setEmail("testuser@garage.com");
        testUser.setPasswordHash("hash");
        testUser.setFullName("Test User");
        testUser.setRole(Role.NORMAL_USER);
        testUser = userRepository.save(testUser);

        otherUser = new User();
        otherUser.setEmail("other@garage.com");
        otherUser.setPasswordHash("hash");
        otherUser.setFullName("Other User");
        otherUser.setRole(Role.NORMAL_USER);
        otherUser = userRepository.save(otherUser);

        adminUser = new User();
        adminUser.setEmail("admin@garage.com");
        adminUser.setPasswordHash("hash");
        adminUser.setFullName("Admin User");
        adminUser.setRole(Role.ADMIN);
        adminUser = userRepository.save(adminUser);

        userVehicle = new Vehicle();
        userVehicle.setUser(testUser);
        userVehicle.setCategory(testCategory);
        userVehicle.setMake("Toyota");
        userVehicle.setModel("Camry");
        userVehicle.setYear(2020);
        userVehicle.setPlateNumber("TN01-1234");
        userVehicle.setFuelType("PETROL");
        userVehicle = vehicleRepository.save(userVehicle);

        otherVehicle = new Vehicle();
        otherVehicle.setUser(otherUser);
        otherVehicle.setCategory(testCategory);
        otherVehicle.setMake("Honda");
        otherVehicle.setModel("City");
        otherVehicle.setYear(2019);
        otherVehicle.setPlateNumber("TN02-5678");
        otherVehicle.setFuelType("PETROL");
        otherVehicle = vehicleRepository.save(otherVehicle);
    }

    private FuelRecord createRecord(Vehicle v, LocalDate date, double kmpl, double cost, double litres) {
        FuelRecord r = new FuelRecord();
        r.setVehicle(v);
        r.setFuelDate(date);
        r.setEstimatedMileageKmpl(BigDecimal.valueOf(kmpl));
        r.setTotalCost(BigDecimal.valueOf(cost));
        r.setQuantityLitres(BigDecimal.valueOf(litres));
        r.setCostPerLitre(BigDecimal.valueOf(cost / litres));
        r.setFuelType(com.mygarage.model.enums.FuelType.PETROL);
        return fuelRecordRepository.save(r);
    }

    // --- Web MVC Tests ---

    @Test
    @WithMockUser(username = "testuser@garage.com", roles = "NORMAL_USER")
    void getFuelAnalytics_happyPath_returns200AndVehicleReport() throws Exception {
        createRecord(userVehicle, LocalDate.now().minusDays(10), 14.5, 2000, 20);
        createRecord(userVehicle, LocalDate.now().minusDays(2), 15.0, 2100, 21);

        mockMvc.perform(get("/vehicles/" + userVehicle.getVehicleId() + "/fuel-analytics"))
                .andExpect(status().isOk())
                .andExpect(view().name("fuel/analytics"))
                .andExpect(model().attributeExists("report"))
                .andExpect(model().attributeExists("vehicle"));
    }

    @Test
    @WithMockUser(username = "testuser@garage.com", roles = "NORMAL_USER")
    void getFuelAnalytics_crossUserTamper_redirectsWithError() throws Exception {
        mockMvc.perform(get("/vehicles/" + otherVehicle.getVehicleId() + "/fuel-analytics"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/vehicles"))
                .andExpect(flash().attributeExists("errorMsg"));
    }

    @Test
    @WithMockUser(username = "admin@garage.com", roles = "ADMIN")
    void getFuelAnalytics_adminUser_returns403() throws Exception {
        mockMvc.perform(get("/vehicles/" + userVehicle.getVehicleId() + "/fuel-analytics"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getFuelAnalytics_unauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/vehicles/" + userVehicle.getVehicleId() + "/fuel-analytics"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "testuser@garage.com", roles = "NORMAL_USER")
    void getFuelAnalytics_vehicleWithNoFuelRecords_returns200WithInsufficientData() throws Exception {
        mockMvc.perform(get("/vehicles/" + userVehicle.getVehicleId() + "/fuel-analytics"))
                .andExpect(status().isOk())
                .andExpect(view().name("fuel/analytics"))
                .andExpect(model().attributeExists("report"));
        
        VehicleFuelAnalyticsDTO report = fuelAnalyticsService.getVehicleFuelAnalytics(userVehicle.getVehicleId(), testUser.getUserId());
        assertThat(report.isInsufficientData()).isTrue();
    }

    @Test
    @WithMockUser(username = "testuser@garage.com", roles = "NORMAL_USER")
    void getFuelAnalytics_vehicleWithOneFuelRecord_returns200PartialData() throws Exception {
        createRecord(userVehicle, LocalDate.now(), 10.0, 1000, 10);
        VehicleFuelAnalyticsDTO report = fuelAnalyticsService.getVehicleFuelAnalytics(userVehicle.getVehicleId(), testUser.getUserId());
        assertThat(report.isInsufficientData()).isTrue();
        assertThat(report.getTotalFillUps()).isEqualTo(1);
    }

    @Test
    @WithMockUser(username = "testuser@garage.com", roles = "NORMAL_USER")
    void getGarageFuel_happyPath_returns200WithFleetSummary() throws Exception {
        mockMvc.perform(get("/vehicles/fuel-analytics"))
                .andExpect(status().isOk())
                .andExpect(view().name("fuel/garage-fuel"))
                .andExpect(model().attributeExists("summary"));
    }

    @Test
    @WithMockUser(username = "admin@garage.com", roles = "ADMIN")
    void getGarageFuel_adminUser_returns403() throws Exception {
        mockMvc.perform(get("/vehicles/fuel-analytics"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getGarageFuel_unauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/vehicles/fuel-analytics"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "testuser@garage.com", roles = "NORMAL_USER")
    void getGarageFuel_noFuelRecordsAtAll_returns200WithNoDataState() throws Exception {
        GarageFuelIntelligenceDTO summary = fuelAnalyticsService.getGarageFuelIntelligence(testUser.getUserId());
        assertThat(summary.isNoFuelData()).isTrue();
    }

    @Test
    @WithMockUser(username = "testuser@garage.com", roles = "NORMAL_USER")
    void getFuelAnalytics_vehicleWithImprovingMileage_trajectoryClassifiedCorrectly() throws Exception {
        LocalDate today = LocalDate.now();
        // Prior 30d (avg ~10.0)
        createRecord(userVehicle, today.minusDays(45), 10.0, 1000, 10);
        createRecord(userVehicle, today.minusDays(40), 10.0, 1000, 10);
        // Current 30d (avg ~15.0) -> +50%
        createRecord(userVehicle, today.minusDays(15), 15.0, 1000, 10);
        createRecord(userVehicle, today.minusDays(5), 15.0, 1000, 10);

        VehicleFuelAnalyticsDTO report = fuelAnalyticsService.getVehicleFuelAnalytics(userVehicle.getVehicleId(), testUser.getUserId());
        assertThat(report.getMileageTrajectory()).isEqualTo("IMPROVING");
        assertThat(report.getMileageTrendArrow()).isEqualTo("↑");
        assertThat(report.getMileageDeltaPercentage()).isGreaterThan(5.0);
    }

    @Test
    @WithMockUser(username = "testuser@garage.com", roles = "NORMAL_USER")
    void getFuelAnalytics_vehicleWithDegradingMileage_trajectoryIsDegrading() throws Exception {
        LocalDate today = LocalDate.now();
        // Prior 30d (avg ~15.0)
        createRecord(userVehicle, today.minusDays(45), 15.0, 1000, 10);
        createRecord(userVehicle, today.minusDays(40), 15.0, 1000, 10);
        // Current 30d (avg ~10.0) -> -33%
        createRecord(userVehicle, today.minusDays(15), 10.0, 1000, 10);
        createRecord(userVehicle, today.minusDays(5), 10.0, 1000, 10);

        VehicleFuelAnalyticsDTO report = fuelAnalyticsService.getVehicleFuelAnalytics(userVehicle.getVehicleId(), testUser.getUserId());
        assertThat(report.getMileageTrajectory()).isEqualTo("DEGRADING");
        assertThat(report.getMileageTrendArrow()).isEqualTo("↓");
        assertThat(report.getMileageDeltaPercentage()).isLessThan(-5.0);
    }

    // --- REST API Tests ---

    @Test
    @WithMockUser(username = "testuser@garage.com", roles = "NORMAL_USER")
    void apiGetVehicleFuelAnalytics_returns200JsonReport() throws Exception {
        createRecord(userVehicle, LocalDate.now(), 15.0, 2000, 20);
        createRecord(userVehicle, LocalDate.now().minusDays(5), 14.0, 1800, 18);

        mockMvc.perform(get("/api/vehicles/" + userVehicle.getVehicleId() + "/fuel-analytics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicleId").value(userVehicle.getVehicleId()))
                .andExpect(jsonPath("$.totalFillUps").value(2));
    }

    @Test
    @WithMockUser(username = "testuser@garage.com", roles = "NORMAL_USER")
    void apiGetVehicleFuelAnalytics_crossUserTamper_returns403() throws Exception {
        mockMvc.perform(get("/api/vehicles/" + otherVehicle.getVehicleId() + "/fuel-analytics"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@garage.com", roles = "ADMIN")
    void apiGetVehicleFuelAnalytics_adminUser_returns403() throws Exception {
        mockMvc.perform(get("/api/vehicles/" + userVehicle.getVehicleId() + "/fuel-analytics"))
                .andExpect(status().isForbidden());
    }

    @Test
    void apiGetVehicleFuelAnalytics_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/vehicles/" + userVehicle.getVehicleId() + "/fuel-analytics"))
                .andExpect(status().is3xxRedirection()); // Default spring security redirects to login
    }

    @Test
    @WithMockUser(username = "testuser@garage.com", roles = "NORMAL_USER")
    void apiGetVehicleFuelAnalytics_insufficientData_returns200WithFlag() throws Exception {
        mockMvc.perform(get("/api/vehicles/" + userVehicle.getVehicleId() + "/fuel-analytics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.insufficientData").value(true));
    }

    @Test
    @WithMockUser(username = "testuser@garage.com", roles = "NORMAL_USER")
    void apiGetGarageFuel_returns200JsonSummary() throws Exception {
        createRecord(userVehicle, LocalDate.now(), 15.0, 2000, 20);
        mockMvc.perform(get("/api/analytics/garage-fuel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.garageLifetimeFuelCost").exists());
    }

    @Test
    @WithMockUser(username = "admin@garage.com", roles = "ADMIN")
    void apiGetGarageFuel_adminUser_returns403() throws Exception {
        mockMvc.perform(get("/api/analytics/garage-fuel"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "testuser@garage.com", roles = "NORMAL_USER")
    void apiGetGarageFuel_multipleVehicles_badgesAssignedCorrectly() throws Exception {
        Vehicle userVehicle2 = new Vehicle();
        userVehicle2.setUser(testUser);
        userVehicle2.setCategory(testCategory);
        userVehicle2.setYear(2021);
        userVehicle2.setMake("Ford");
        userVehicle2.setModel("Figo");
        userVehicle2.setPlateNumber("TN03-9999");
        userVehicle2.setFuelType("PETROL");
        final Vehicle savedVehicle2 = vehicleRepository.save(userVehicle2);

        createRecord(userVehicle, LocalDate.now(), 20.0, 1000, 10); // Better mileage
        createRecord(savedVehicle2, LocalDate.now(), 10.0, 2000, 20); // Worse mileage, higher cost

        GarageFuelIntelligenceDTO summary = fuelAnalyticsService.getGarageFuelIntelligence(testUser.getUserId());
        
        assertThat(summary.getVehicleSummaries()).hasSize(2);
        
        // Find the one with mostFuelEfficient badge
        boolean foundMostEfficient = summary.getVehicleSummaries().stream()
                .anyMatch(v -> v.isMostFuelEfficient() && v.getVehicleId().equals(userVehicle.getVehicleId()));
        assertThat(foundMostEfficient).isTrue();

        boolean foundHighestConsumer = summary.getVehicleSummaries().stream()
                .anyMatch(v -> v.isHighestLitresConsumed() && v.getVehicleId().equals(savedVehicle2.getVehicleId()));
        assertThat(foundHighestConsumer).isTrue();
    }

    // --- Service Unit Logic Tests ---

    @Test
    @WithMockUser(username = "testuser@garage.com", roles = "NORMAL_USER")
    void computeRollingMileage_filtersNullKmplValues_correctlyAverages() {
        createRecord(userVehicle, LocalDate.now(), 10.0, 1000, 10);
        
        FuelRecord nullMileageRecord = new FuelRecord();
        nullMileageRecord.setVehicle(userVehicle);
        nullMileageRecord.setFuelDate(LocalDate.now().minusDays(1));
        nullMileageRecord.setTotalCost(BigDecimal.valueOf(1000));
        nullMileageRecord.setQuantityLitres(BigDecimal.valueOf(10));
        nullMileageRecord.setCostPerLitre(BigDecimal.valueOf(100));
        nullMileageRecord.setFuelType(com.mygarage.model.enums.FuelType.PETROL);
        // NO estimatedMileageKmpl set
        fuelRecordRepository.save(nullMileageRecord);
        
        createRecord(userVehicle, LocalDate.now().minusDays(2), 20.0, 1000, 10);

        VehicleFuelAnalyticsDTO report = fuelAnalyticsService.getVehicleFuelAnalytics(userVehicle.getVehicleId(), testUser.getUserId());
        
        // Avg should be (10+20)/2 = 15.0, null is ignored
        assertThat(report.getOverallAvgMileageKmpl()).isEqualByComparingTo("15.00");
    }

    @Test
    @WithMockUser(username = "testuser@garage.com", roles = "NORMAL_USER")
    void computePeriodDelta_improvingMileage_returnsPositiveDelta() {
        LocalDate today = LocalDate.now();
        createRecord(userVehicle, today.minusDays(40), 10.0, 1000, 10);
        createRecord(userVehicle, today.minusDays(10), 15.0, 1000, 10);

        VehicleFuelAnalyticsDTO report = fuelAnalyticsService.getVehicleFuelAnalytics(userVehicle.getVehicleId(), testUser.getUserId());
        assertThat(report.getMileageDeltaPercentage()).isEqualTo(50.0);
        assertThat(report.getMileageTrajectory()).isEqualTo("IMPROVING");
    }

    @Test
    @WithMockUser(username = "testuser@garage.com", roles = "NORMAL_USER")
    void computePeriodDelta_degradingMileage_returnsNegativeDelta() {
        LocalDate today = LocalDate.now();
        createRecord(userVehicle, today.minusDays(40), 20.0, 1000, 10);
        createRecord(userVehicle, today.minusDays(10), 10.0, 1000, 10);

        VehicleFuelAnalyticsDTO report = fuelAnalyticsService.getVehicleFuelAnalytics(userVehicle.getVehicleId(), testUser.getUserId());
        assertThat(report.getMileageDeltaPercentage()).isEqualTo(-50.0);
        assertThat(report.getMileageTrajectory()).isEqualTo("DEGRADING");
    }

    @Test
    @WithMockUser(username = "testuser@garage.com", roles = "NORMAL_USER")
    void computePriceVolatility_identicalPrices_returnsZeroStdDev() {
        createRecord(userVehicle, LocalDate.now(), 15.0, 1000, 10); // costPerLitre = 100
        createRecord(userVehicle, LocalDate.now().minusDays(1), 15.0, 1000, 10); // costPerLitre = 100

        VehicleFuelAnalyticsDTO report = fuelAnalyticsService.getVehicleFuelAnalytics(userVehicle.getVehicleId(), testUser.getUserId());
        assertThat(report.getPriceVolatilityStdDev()).isEqualByComparingTo("0.00");
        assertThat(report.getPriceVolatilityLabel()).isEqualTo("LOW");
    }

    @Test
    @WithMockUser(username = "testuser@garage.com", roles = "NORMAL_USER")
    void computeFillFrequency_twoFillUps_returnsCorrectDayGap() {
        createRecord(userVehicle, LocalDate.now().minusDays(15), 15.0, 1000, 10);
        createRecord(userVehicle, LocalDate.now(), 15.0, 1000, 10);

        VehicleFuelAnalyticsDTO report = fuelAnalyticsService.getVehicleFuelAnalytics(userVehicle.getVehicleId(), testUser.getUserId());
        assertThat(report.getAvgDaysBetweenFillUps()).isEqualTo(15.0);
    }
}
