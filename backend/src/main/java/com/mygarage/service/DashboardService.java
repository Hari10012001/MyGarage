package com.mygarage.service;

import com.mygarage.model.MaintenanceRecord;
import com.mygarage.model.ServiceRecord;
import com.mygarage.model.Vehicle;
import com.mygarage.repository.*;
import com.mygarage.model.enums.MaintenanceStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DashboardService {

    private final VehicleRepository vehicleRepository;
    private final ServiceRecordRepository serviceRecordRepository;
    private final FuelRecordRepository fuelRecordRepository;
    private final MaintenanceRecordRepository maintenanceRecordRepository;
    private final UserRepository userRepository;
    private final ServiceRecordService serviceRecordService;

    // ================================================================
    // USER DASHBOARD - all stats are REAL from database, NEVER hardcoded
    // ================================================================

    public long getUserVehicleCount(Long userId) {
        return vehicleRepository.countByUserUserId(userId);
    }

    public long getUserServiceCount(Long userId) {
        return serviceRecordRepository.countByVehicleUserUserId(userId);
    }

    public long getUserFuelCount(Long userId) {
        return fuelRecordRepository.countByVehicleUserUserId(userId);
    }

    public long getUserMaintenanceAlertCount(Long userId) {
        return maintenanceRecordRepository.countByVehicleUserUserIdAndStatus(userId, MaintenanceStatus.OVERDUE)
             + maintenanceRecordRepository.countByVehicleUserUserIdAndStatus(userId, MaintenanceStatus.DUE_TODAY);
    }

    public BigDecimal getUserTotalFuelCost(Long userId) {
        return fuelRecordRepository.sumTotalCostByUserId(userId);
    }

    public List<ServiceRecord> getRecentServiceRecords(Long userId) {
        return serviceRecordService.getRecentForUser(userId, 5);
    }

    public List<MaintenanceRecord> getMaintenanceAlerts(Long userId) {
        return maintenanceRecordRepository.findAlertsForUser(userId);
    }

    // ================================================================
    // ADMIN DASHBOARD - system-wide stats, NEVER hardcoded
    // ================================================================

    public long getAdminTotalUsers() {
        return userRepository.countByRole(com.mygarage.model.enums.Role.NORMAL_USER);
    }

    public long getAdminTotalVehicles() {
        return vehicleRepository.count();
    }

    public long getAdminTotalServiceRecords() {
        return serviceRecordRepository.count();
    }

    public long getAdminTotalFuelRecords() {
        return fuelRecordRepository.count();
    }

    public long getAdminTotalMaintenanceRecords() {
        return maintenanceRecordRepository.count();
    }

    public long getAdminOverdueMaintenanceCount() {
        // System-wide count using direct JPA count
        return maintenanceRecordRepository.countByStatus(MaintenanceStatus.OVERDUE);
    }
}
