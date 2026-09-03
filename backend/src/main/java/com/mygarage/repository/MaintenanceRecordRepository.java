package com.mygarage.repository;

import com.mygarage.model.MaintenanceRecord;
import com.mygarage.model.enums.MaintenanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MaintenanceRecordRepository extends JpaRepository<MaintenanceRecord, Long> {

    List<MaintenanceRecord> findByVehicleVehicleIdOrderByScheduledDateAsc(Long vehicleId);

    List<MaintenanceRecord> findByVehicleVehicleIdAndStatusOrderByScheduledDateAsc(Long vehicleId, MaintenanceStatus status);

    // Ownership check
    Optional<MaintenanceRecord> findByMaintenanceIdAndVehicleUserUserId(Long maintenanceId, Long userId);

    // Alerts for dashboard - overdue and due today for user
    @Query("SELECT m FROM MaintenanceRecord m WHERE m.vehicle.user.userId = :userId " +
           "AND m.status IN ('OVERDUE', 'DUE_TODAY') ORDER BY m.scheduledDate ASC")
    List<MaintenanceRecord> findAlertsForUser(@Param("userId") Long userId);

    // Count by status for a user
    long countByVehicleUserUserIdAndStatus(Long userId, MaintenanceStatus status);

    long countByVehicleUserUserId(Long userId);
    long countByVehicleVehicleId(Long vehicleId);
    long countByStatus(MaintenanceStatus status);
}
