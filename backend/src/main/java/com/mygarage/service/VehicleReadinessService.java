package com.mygarage.service;

import com.mygarage.dto.response.GarageFleetDispatchReportDTO;
import com.mygarage.dto.response.VehicleTripReadinessReportDTO;

public interface VehicleReadinessService {

    /**
     * Evaluates the operational trip readiness of a specific vehicle for a simulated journey.
     * Enforces two-tier ownership validation.
     *
     * @param vehicleId Vehicle identifier
     * @param tripDistanceKm Simulated trip distance in kilometers (clamped 10.0 to 10,000.0 km)
     * @param tripDays Simulated journey duration in days (clamped 1 to 30)
     * @param drivingRegime Driving regime (HIGHWAY_CRUISE, MIXED_BALANCED, CITY_CONGESTED, MOUNTAIN_SEVERE)
     * @param userId Authenticated user identifier
     * @return VehicleTripReadinessReportDTO containing readiness scores, consumable margins, fuel staging, and checklist
     */
    VehicleTripReadinessReportDTO evaluateVehicleReadiness(Long vehicleId, Double tripDistanceKm, Integer tripDays, String drivingRegime, Long userId);

    /**
     * Evaluates and compares all active vehicles in the user's garage for an upcoming journey,
     * designating the optimal mission dispatch candidate.
     *
     * @param tripDistanceKm Simulated trip distance in kilometers (clamped 10.0 to 10,000.0 km)
     * @param tripDays Simulated journey duration in days (clamped 1 to 30)
     * @param drivingRegime Driving regime
     * @param userId Authenticated user identifier
     * @return GarageFleetDispatchReportDTO containing ranked candidate vehicles and dispatch recommendations
     */
    GarageFleetDispatchReportDTO evaluateFleetDispatch(Double tripDistanceKm, Integer tripDays, String drivingRegime, Long userId);
}
