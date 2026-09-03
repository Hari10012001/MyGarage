package com.mygarage.repository;

import com.mygarage.model.ServiceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceRecordRepository extends JpaRepository<ServiceRecord, Long> {

    List<ServiceRecord> findByVehicleVehicleIdOrderByServiceDateDesc(Long vehicleId);

    // Ownership check
    Optional<ServiceRecord> findByServiceIdAndVehicleUserUserId(Long serviceId, Long userId);

    // Search by service type or garage
    @Query("SELECT s FROM ServiceRecord s WHERE s.vehicle.vehicleId = :vehicleId AND " +
           "(LOWER(s.serviceType) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(s.garageName) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<ServiceRecord> searchByVehicleAndKeyword(@Param("vehicleId") Long vehicleId, @Param("keyword") String keyword);

    // For dashboard - recent records for user
    @Query("SELECT s FROM ServiceRecord s WHERE s.vehicle.user.userId = :userId ORDER BY s.serviceDate DESC")
    List<ServiceRecord> findRecentByUserId(@Param("userId") Long userId, org.springframework.data.domain.Pageable pageable);

    long countByVehicleUserUserId(Long userId);
    long countByVehicleVehicleId(Long vehicleId);
}
