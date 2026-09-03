package com.mygarage.repository;

import com.mygarage.model.VehicleCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleCategoryRepository extends JpaRepository<VehicleCategory, Long> {
    Optional<VehicleCategory> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
    @Query("SELECT DISTINCT c FROM VehicleCategory c LEFT JOIN FETCH c.vehicles ORDER BY c.name ASC")
    List<VehicleCategory> findAllByOrderByNameAsc();

    @Query("SELECT vc, COUNT(v) FROM VehicleCategory vc LEFT JOIN vc.vehicles v GROUP BY vc ORDER BY vc.name ASC")
    List<Object[]> findAllWithVehicleCount();
}
