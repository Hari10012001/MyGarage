package com.mygarage.repository;

import com.mygarage.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    // All vehicles for a specific user
    List<Vehicle> findByUserUserIdOrderByCreatedAtDesc(Long userId);

    // Ownership check
    Optional<Vehicle> findByVehicleIdAndUserUserId(Long vehicleId, Long userId);

    // Plate uniqueness for a user
    boolean existsByPlateNumberIgnoreCaseAndUserUserId(String plateNumber, Long userId);

    // Search by make/model/plate for user
    @Query("SELECT v FROM Vehicle v WHERE v.user.userId = :userId AND " +
           "(LOWER(v.make) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(v.model) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(v.plateNumber) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Vehicle> searchByUserAndKeyword(@Param("userId") Long userId, @Param("keyword") String keyword);

    long countByUserUserId(Long userId);
    long count();
}
