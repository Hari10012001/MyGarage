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
    @Query("SELECT m FROM MaintenanceRecord m JOIN FETCH m.vehicle WHERE m.vehicle.user.userId = :userId " +
           "AND m.status IN ('OVERDUE', 'DUE_TODAY') ORDER BY m.scheduledDate ASC")
    List<MaintenanceRecord> findAlertsForUser(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(m.cost), 0) FROM MaintenanceRecord m WHERE m.vehicle.user.userId = :userId")
    java.math.BigDecimal sumCostByUserId(@Param("userId") Long userId);

    // Count by status for a user
    long countByVehicleUserUserIdAndStatus(Long userId, MaintenanceStatus status);

    // Cross-vehicle user queries
    @Query("SELECT m FROM MaintenanceRecord m JOIN FETCH m.vehicle v WHERE v.user.userId = :userId ORDER BY m.scheduledDate ASC")
    List<MaintenanceRecord> findAllByUserId(@Param("userId") Long userId);

    @Query("SELECT m FROM MaintenanceRecord m JOIN FETCH m.vehicle v WHERE v.user.userId = :userId AND m.status = :status ORDER BY m.scheduledDate ASC")
    List<MaintenanceRecord> findAllByUserIdAndStatus(@Param("userId") Long userId, @Param("status") MaintenanceStatus status);

    @Query("SELECT COALESCE(SUM(m.cost), 0) FROM MaintenanceRecord m WHERE m.vehicle.vehicleId = :vehicleId")
    java.math.BigDecimal sumCostByVehicleId(@Param("vehicleId") Long vehicleId);

    long countByVehicleUserUserId(Long userId);
    long countByVehicleVehicleId(Long vehicleId);
    long countByVehicleVehicleIdAndStatus(Long vehicleId, MaintenanceStatus status);
    long countByStatus(MaintenanceStatus status);

    // M14: Check duplicate pending tasks for predictive milestones
    boolean existsByVehicleVehicleIdAndTitleIgnoreCaseAndStatusIn(Long vehicleId, String title, List<MaintenanceStatus> statuses);

    Optional<MaintenanceRecord> findFirstByVehicleVehicleIdAndTitleIgnoreCaseAndStatusIn(Long vehicleId, String title, List<MaintenanceStatus> statuses);
}
