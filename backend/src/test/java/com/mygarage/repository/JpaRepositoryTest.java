package com.mygarage.repository;

import com.mygarage.model.*;
import com.mygarage.model.enums.FuelType;
import com.mygarage.model.enums.MaintenanceStatus;
import com.mygarage.model.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Deep JPA and Entity Mapping Verification Test Suite (M2).
 * Verifies all 6 entities, constraints, foreign keys, cascade rules, and ownership queries.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class JpaRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VehicleCategoryRepository categoryRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private ServiceRecordRepository serviceRecordRepository;

    @Autowired
    private FuelRecordRepository fuelRecordRepository;

    @Autowired
    private MaintenanceRecordRepository maintenanceRecordRepository;

    private User testUser1;
    private User testUser2;
    private VehicleCategory sedanCat;
    private Vehicle vehicle1;

    @BeforeEach
    void setUp() {
        // User 1
        testUser1 = new User();
        testUser1.setFullName("Hariharan P");
        testUser1.setEmail("hari@example.com");
        testUser1.setPasswordHash("$2a$10$dummyhashfortestingonly123456789012345678901234567890");
        testUser1.setPhone("9876543210");
        testUser1.setRole(Role.NORMAL_USER);
        testUser1.setActive(true);
        testUser1 = em.persistAndFlush(testUser1);

        // User 2 (for cross-user ownership tests)
        testUser2 = new User();
        testUser2.setFullName("Other Owner");
        testUser2.setEmail("other@example.com");
        testUser2.setPasswordHash("$2a$10$dummyhashfortestingonly123456789012345678901234567890");
        testUser2.setRole(Role.NORMAL_USER);
        testUser2.setActive(true);
        testUser2 = em.persistAndFlush(testUser2);

        // Category
        sedanCat = new VehicleCategory();
        sedanCat.setName("Sedan");
        sedanCat.setIcon("🚗");
        sedanCat.setDescription("4-door passenger car");
        sedanCat = em.persistAndFlush(sedanCat);

        // Vehicle for User 1
        vehicle1 = new Vehicle();
        vehicle1.setUser(testUser1);
        vehicle1.setCategory(sedanCat);
        vehicle1.setMake("Honda");
        vehicle1.setModel("City");
        vehicle1.setYear(2022);
        vehicle1.setColor("White");
        vehicle1.setFuelType("PETROL");
        vehicle1.setPlateNumber("TN01AB1234");
        vehicle1.setCurrentOdometer(24000);
        vehicle1 = em.persistAndFlush(vehicle1);
    }

    @Test
    @DisplayName("1. User Entity: Persist, retrieve, unique email")
    void testUserEntity() {
        Optional<User> found = userRepository.findByEmail("hari@example.com");
        assertTrue(found.isPresent());
        assertEquals("Hariharan P", found.get().getFullName());
        assertEquals(Role.NORMAL_USER, found.get().getRole());
        assertTrue(found.get().isActive());
        assertNotNull(found.get().getCreatedAt());

        assertTrue(userRepository.existsByEmail("hari@example.com"));
        assertFalse(userRepository.existsByEmail("nonexistent@example.com"));
    }

    @Test
    @DisplayName("2. Vehicle Category Entity: Persist, retrieve, count")
    void testVehicleCategoryEntity() {
        Optional<VehicleCategory> cat = categoryRepository.findByNameIgnoreCase("sedan");
        assertTrue(cat.isPresent());
        assertEquals("Sedan", cat.get().getName());

        long count = vehicleRepository.countByCategoryCategoryId(sedanCat.getCategoryId());
        assertEquals(1, count);
    }

    @Test
    @DisplayName("3. Vehicle Ownership: User 1 owns vehicle, User 2 cannot access")
    void testVehicleOwnership() {
        // User 1 owns vehicle1
        Optional<Vehicle> user1Vehicle = vehicleRepository.findByVehicleIdAndUserUserId(
                vehicle1.getVehicleId(), testUser1.getUserId());
        assertTrue(user1Vehicle.isPresent());

        // User 2 cannot access User 1's vehicle
        Optional<Vehicle> user2Vehicle = vehicleRepository.findByVehicleIdAndUserUserId(
                vehicle1.getVehicleId(), testUser2.getUserId());
        assertTrue(user2Vehicle.isEmpty(), "User 2 must NOT have access to User 1's vehicle");
    }

    @Test
    @DisplayName("4. ServiceRecord: Persist, check foreign keys, ownership query")
    void testServiceRecordMappingAndOwnership() {
        ServiceRecord service = new ServiceRecord();
        service.setVehicle(vehicle1);
        service.setServiceDate(LocalDate.now().minusDays(10));
        service.setServiceType("Full Service");
        service.setDescription("Engine oil and oil filter changed");
        service.setGarageName("Authorized Honda Service Center");
        service.setCost(new BigDecimal("2450.00"));
        service.setOdometerAtService(24500);
        service = em.persistAndFlush(service);

        // Query by vehicle
        List<ServiceRecord> list = serviceRecordRepository.findByVehicleVehicleIdOrderByServiceDateDesc(vehicle1.getVehicleId());
        assertEquals(1, list.size());
        assertEquals(new BigDecimal("2450.00"), list.get(0).getCost());

        // Ownership query
        Optional<ServiceRecord> foundUser1 = serviceRecordRepository.findByServiceIdAndVehicleUserUserId(
                service.getServiceId(), testUser1.getUserId());
        assertTrue(foundUser1.isPresent());

        Optional<ServiceRecord> foundUser2 = serviceRecordRepository.findByServiceIdAndVehicleUserUserId(
                service.getServiceId(), testUser2.getUserId());
        assertTrue(foundUser2.isEmpty(), "User 2 must NOT access User 1's service record");
    }

    @Test
    @DisplayName("5. FuelRecord: Quantity, cost precision, previous record query")
    void testFuelRecordMappingAndMileageQuery() {
        FuelRecord f1 = new FuelRecord();
        f1.setVehicle(vehicle1);
        f1.setFuelDate(LocalDate.now().minusDays(14));
        f1.setFuelType(FuelType.PETROL);
        f1.setQuantityLitres(new BigDecimal("35.50"));
        f1.setCostPerLitre(new BigDecimal("102.50"));
        f1.setOdometerAtFill(23000);
        em.persistAndFlush(f1);

        FuelRecord f2 = new FuelRecord();
        f2.setVehicle(vehicle1);
        f2.setFuelDate(LocalDate.now().minusDays(2));
        f2.setFuelType(FuelType.PETROL);
        f2.setQuantityLitres(new BigDecimal("38.00"));
        f2.setCostPerLitre(new BigDecimal("103.00"));
        f2.setOdometerAtFill(23650);
        f2.setEstimatedMileageKmpl(new BigDecimal("17.11"));
        em.persistAndFlush(f2);

        List<FuelRecord> records = fuelRecordRepository.findByVehicleVehicleIdOrderByFuelDateDesc(vehicle1.getVehicleId());
        assertEquals(2, records.size());
        assertEquals(new BigDecimal("17.11"), records.get(0).getEstimatedMileageKmpl());

        // Ownership check
        Optional<FuelRecord> foundUser1 = fuelRecordRepository.findByFuelIdAndVehicleUserUserId(
                f2.getFuelId(), testUser1.getUserId());
        assertTrue(foundUser1.isPresent());

        Optional<FuelRecord> foundUser2 = fuelRecordRepository.findByFuelIdAndVehicleUserUserId(
                f2.getFuelId(), testUser2.getUserId());
        assertTrue(foundUser2.isEmpty(), "User 2 must NOT access User 1's fuel record");
    }

    @Test
    @DisplayName("6. MaintenanceRecord: Status, alert query, ownership check")
    void testMaintenanceRecordMappingAndAlerts() {
        MaintenanceRecord m1 = new MaintenanceRecord();
        m1.setVehicle(vehicle1);
        m1.setTitle("Brake Pad Inspection");
        m1.setScheduledDate(LocalDate.now().minusDays(2));
        m1.setStatus(MaintenanceStatus.OVERDUE);
        em.persistAndFlush(m1);

        MaintenanceRecord m2 = new MaintenanceRecord();
        m2.setVehicle(vehicle1);
        m2.setTitle("Tyre Rotation");
        m2.setScheduledDate(LocalDate.now().plusDays(20));
        m2.setStatus(MaintenanceStatus.UPCOMING);
        em.persistAndFlush(m2);

        List<MaintenanceRecord> alerts = maintenanceRecordRepository.findAlertsForUser(testUser1.getUserId());
        assertEquals(1, alerts.size());
        assertEquals("Brake Pad Inspection", alerts.get(0).getTitle());

        // Ownership check
        Optional<MaintenanceRecord> foundUser1 = maintenanceRecordRepository.findByMaintenanceIdAndVehicleUserUserId(
                m1.getMaintenanceId(), testUser1.getUserId());
        assertTrue(foundUser1.isPresent());

        Optional<MaintenanceRecord> foundUser2 = maintenanceRecordRepository.findByMaintenanceIdAndVehicleUserUserId(
                m1.getMaintenanceId(), testUser2.getUserId());
        assertTrue(foundUser2.isEmpty(), "User 2 must NOT access User 1's maintenance task");
    }

    @Test
    @DisplayName("7. Cascade Delete: Vehicle deletion cascades to child records")
    void testCascadeDeleteVehicleRemovesChildRecords() {
        // Create service, fuel, and maintenance records attached to vehicle1
        ServiceRecord service = new ServiceRecord();
        service.setServiceDate(LocalDate.now());
        service.setServiceType("Oil Change");
        vehicle1.addServiceRecord(service);

        FuelRecord fuel = new FuelRecord();
        fuel.setFuelDate(LocalDate.now());
        fuel.setFuelType(FuelType.PETROL);
        fuel.setQuantityLitres(new BigDecimal("30.00"));
        fuel.setCostPerLitre(new BigDecimal("100.00"));
        vehicle1.addFuelRecord(fuel);

        MaintenanceRecord maint = new MaintenanceRecord();
        maint.setTitle("Battery Check");
        maint.setScheduledDate(LocalDate.now().plusDays(5));
        vehicle1.addMaintenanceRecord(maint);

        vehicle1 = em.persistAndFlush(vehicle1);

        Long serviceId = service.getServiceId();
        Long fuelId = fuel.getFuelId();
        Long maintId = maint.getMaintenanceId();
        Long vehicleId = vehicle1.getVehicleId();

        // Delete vehicle1
        vehicleRepository.delete(vehicle1);
        em.flush();
        em.clear();

        // Vehicle must be gone
        assertTrue(vehicleRepository.findById(vehicleId).isEmpty());

        // All child records must be cascade-deleted (no orphan records!)
        assertTrue(serviceRecordRepository.findById(serviceId).isEmpty(), "ServiceRecord must be deleted with vehicle");
        assertTrue(fuelRecordRepository.findById(fuelId).isEmpty(), "FuelRecord must be deleted with vehicle");
        assertTrue(maintenanceRecordRepository.findById(maintId).isEmpty(), "MaintenanceRecord must be deleted with vehicle");

        // Category and User must still exist
        assertTrue(categoryRepository.findById(sedanCat.getCategoryId()).isPresent());
        assertTrue(userRepository.findById(testUser1.getUserId()).isPresent());
    }
}