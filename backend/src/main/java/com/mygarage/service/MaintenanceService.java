package com.mygarage.service;

import com.mygarage.dto.request.MaintenanceRequest;
import com.mygarage.model.MaintenanceRecord;
import com.mygarage.model.Vehicle;
import com.mygarage.model.enums.MaintenanceStatus;
import com.mygarage.repository.MaintenanceRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class MaintenanceService {

    private final MaintenanceRecordRepository maintenanceRecordRepository;
    private final VehicleService vehicleService;

    /**
     * Computes maintenance status based on scheduledDate and completedDate.
     * Simple business logic - no AI or predictive algorithms.
     *
     *   if completedDate != null         -> COMPLETED
     *   else if scheduledDate < today    -> OVERDUE
     *   else if scheduledDate == today   -> DUE_TODAY
     *   else                             -> UPCOMING
     */
    public static MaintenanceStatus computeStatus(LocalDate scheduledDate, LocalDate completedDate) {
        if (completedDate != null) {
            return MaintenanceStatus.COMPLETED;
        }
        LocalDate today = LocalDate.now();
        if (scheduledDate.isBefore(today)) {
            return MaintenanceStatus.OVERDUE;
        } else if (scheduledDate.isEqual(today)) {
            return MaintenanceStatus.DUE_TODAY;
        } else {
            return MaintenanceStatus.UPCOMING;
        }
    }

    public MaintenanceRecord addMaintenanceRecord(Long vehicleId, Long userId, MaintenanceRequest request) {
        Vehicle vehicle = vehicleService.getVehicleForUser(vehicleId, userId);
        MaintenanceRecord record = new MaintenanceRecord();
        record.setVehicle(vehicle);
        record.setTitle(request.getTitle().trim());
        record.setDescription(request.getDescription());
        record.setScheduledDate(request.getScheduledDate());
        record.setCompletedDate(request.getCompletedDate());
        record.setCost(request.getCost());
        record.setNotes(request.getNotes());
        record.setStatus(computeStatus(request.getScheduledDate(), request.getCompletedDate()));
        return maintenanceRecordRepository.save(record);
    }

    @Transactional(readOnly = true)
    public List<MaintenanceRecord> getMaintenanceForVehicle(Long vehicleId, Long userId) {
        vehicleService.getVehicleForUser(vehicleId, userId);
        List<MaintenanceRecord> records = maintenanceRecordRepository
                .findByVehicleVehicleIdOrderByScheduledDateAsc(vehicleId);
        // Recompute status on read (keeps status fresh)
        records.forEach(r -> r.setStatus(computeStatus(r.getScheduledDate(), r.getCompletedDate())));
        return records;
    }

    @Transactional(readOnly = true)
    public List<MaintenanceRecord> getMaintenanceByStatus(Long vehicleId, Long userId, MaintenanceStatus status) {
        vehicleService.getVehicleForUser(vehicleId, userId);
        return maintenanceRecordRepository
                .findByVehicleVehicleIdAndStatusOrderByScheduledDateAsc(vehicleId, status);
    }

    @Transactional(readOnly = true)
    public MaintenanceRecord getMaintenanceRecord(Long maintenanceId, Long userId) {
        return maintenanceRecordRepository.findByMaintenanceIdAndVehicleUserUserId(maintenanceId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Maintenance record not found or access denied."));
    }

    @Transactional(readOnly = true)
    public MaintenanceRecord getMaintenanceRecordForVehicle(Long maintenanceId, Long vehicleId, Long userId) {
        vehicleService.getVehicleForUser(vehicleId, userId);
        MaintenanceRecord record = getMaintenanceRecord(maintenanceId, userId);
        if (!record.getVehicle().getVehicleId().equals(vehicleId)) {
            throw new IllegalArgumentException("Maintenance record does not belong to the specified vehicle.");
        }
        return record;
    }

    public MaintenanceRecord updateMaintenanceRecord(Long maintenanceId, Long userId, MaintenanceRequest request) {
        MaintenanceRecord record = getMaintenanceRecord(maintenanceId, userId);
        record.setTitle(request.getTitle().trim());
        record.setDescription(request.getDescription());
        record.setScheduledDate(request.getScheduledDate());
        record.setCompletedDate(request.getCompletedDate());
        record.setCost(request.getCost());
        record.setNotes(request.getNotes());
        record.setStatus(computeStatus(request.getScheduledDate(), request.getCompletedDate()));
        return maintenanceRecordRepository.save(record);
    }

    public MaintenanceRecord updateMaintenanceRecordForVehicle(Long maintenanceId, Long vehicleId, Long userId, MaintenanceRequest request) {
        MaintenanceRecord record = getMaintenanceRecordForVehicle(maintenanceId, vehicleId, userId);
        record.setTitle(request.getTitle().trim());
        record.setDescription(request.getDescription());
        record.setScheduledDate(request.getScheduledDate());
        record.setCompletedDate(request.getCompletedDate());
        record.setCost(request.getCost());
        record.setNotes(request.getNotes());
        record.setStatus(computeStatus(request.getScheduledDate(), request.getCompletedDate()));
        return maintenanceRecordRepository.save(record);
    }

    public MaintenanceRecord markCompleted(Long maintenanceId, Long userId, LocalDate completedDate) {
        MaintenanceRecord record = getMaintenanceRecord(maintenanceId, userId);
        record.setCompletedDate(completedDate != null ? completedDate : LocalDate.now());
        record.setStatus(MaintenanceStatus.COMPLETED);
        return maintenanceRecordRepository.save(record);
    }

    public MaintenanceRecord markCompletedForVehicle(Long maintenanceId, Long vehicleId, Long userId, LocalDate completedDate) {
        MaintenanceRecord record = getMaintenanceRecordForVehicle(maintenanceId, vehicleId, userId);
        record.setCompletedDate(completedDate != null ? completedDate : LocalDate.now());
        record.setStatus(MaintenanceStatus.COMPLETED);
        return maintenanceRecordRepository.save(record);
    }

    public void deleteMaintenanceRecord(Long maintenanceId, Long userId) {
        MaintenanceRecord record = getMaintenanceRecord(maintenanceId, userId);
        maintenanceRecordRepository.delete(record);
    }

    public void deleteMaintenanceRecordForVehicle(Long maintenanceId, Long vehicleId, Long userId) {
        MaintenanceRecord record = getMaintenanceRecordForVehicle(maintenanceId, vehicleId, userId);
        maintenanceRecordRepository.delete(record);
    }

    @Transactional(readOnly = true)
    public List<MaintenanceRecord> getAlertsForUser(Long userId) {
        // Recompute and save status updates before fetching alerts
        return maintenanceRecordRepository.findAlertsForUser(userId);
    }

    @Transactional(readOnly = true)
    public long countAlertingForUser(Long userId) {
        return maintenanceRecordRepository.countByVehicleUserUserIdAndStatus(userId, MaintenanceStatus.OVERDUE)
                + maintenanceRecordRepository.countByVehicleUserUserIdAndStatus(userId, MaintenanceStatus.DUE_TODAY);
    }

    @Transactional(readOnly = true)
    public long countForUser(Long userId) {
        return maintenanceRecordRepository.countByVehicleUserUserId(userId);
    }

    /**
     * Refreshes the status of all non-completed records in the database.
     * Can be called periodically or on dashboard load.
     */
    public void refreshAllStatuses(Long userId) {
        List<MaintenanceRecord> records = maintenanceRecordRepository
                .findAlertsForUser(userId); // Gets overdue/due today
        // Also update upcoming that may have become overdue
        // This simple approach recomputes on every dashboard load
    }
}
