package com.mygarage.dto.response;

import java.util.List;

public record VehicleTripReadinessReportDTO(
    Long vehicleId,
    String vehicleName,
    String licensePlate,
    String categoryName,
    Integer currentMileage,
    Double tripDistanceKm,
    Integer tripDays,
    String drivingRegime,
    Double postTripProjectedMileage,
    Integer tripReadinessIndex,             // Clamped 0 - 100
    String readinessBand,                    // MISSION_READY, GOOD_CONDITION, CAUTION_REQUIRED, HIGH_RISK
    String readinessSummary,
    Boolean hasMidTripBreach,
    List<String> midTripBreachAlerts,
    List<ConsumableReserveMarginDTO> consumableMargins,
    TripFuelStagingDTO fuelStaging,
    List<PreTripChecklistItemDTO> checklist,
    Integer criticalActionCount,
    Integer advisoryCount,
    Integer passedCount,
    String analyticalDisclaimer,             // Explicit legal/engineering disclaimer
    String benchmarkAssumptionsNote,         // Configurable model assumptions explanation
    Boolean hasHistoricalRecords            // True if prior service/maintenance logs exist
) {}
