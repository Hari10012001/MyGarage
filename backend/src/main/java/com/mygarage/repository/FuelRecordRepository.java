package com.mygarage.repository;

import com.mygarage.model.FuelRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FuelRecordRepository extends JpaRepository<FuelRecord, Long> {

    List<FuelRecord> findByVehicleVehicleIdOrderByFuelDateDesc(Long vehicleId);

    // Ownership check
    Optional<FuelRecord> findByFuelIdAndVehicleUserUserId(Long fuelId, Long userId);

    // Get previous fuel record for mileage calculation
    @Query("SELECT f FROM FuelRecord f WHERE f.vehicle.vehicleId = :vehicleId " +
           "AND f.fuelDate < :date ORDER BY f.fuelDate DESC")
    List<FuelRecord> findPreviousRecord(@Param("vehicleId") Long vehicleId, @Param("date") LocalDate date, org.springframework.data.domain.Pageable pageable);

    // Filter by date range
    @Query("SELECT f FROM FuelRecord f WHERE f.vehicle.vehicleId = :vehicleId " +
           "AND f.fuelDate BETWEEN :from AND :to ORDER BY f.fuelDate DESC")
    List<FuelRecord> findByVehicleAndDateRange(@Param("vehicleId") Long vehicleId,
                                               @Param("from") LocalDate from, @Param("to") LocalDate to);

    // Total fuel cost for user
    @Query("SELECT COALESCE(SUM(f.totalCost), 0) FROM FuelRecord f WHERE f.vehicle.user.userId = :userId")
    java.math.BigDecimal sumTotalCostByUserId(@Param("userId") Long userId);

    long countByVehicleUserUserId(Long userId);
    long countByVehicleVehicleId(Long vehicleId);
}
