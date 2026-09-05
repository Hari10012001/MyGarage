package com.mygarage.dto.response;

import java.math.BigDecimal;

public record FleetDispatchCandidateDTO(
    Long vehicleId,
    String vehicleName,
    String licensePlate,
    String categoryName,
    Integer currentMileage,
    Integer tripReadinessIndex,
    String readinessBand,
    BigDecimal estimatedTripFuelCost,
    Double estimatedFuelNeededLiters,
    Integer criticalIssuesCount,
    Boolean hasMidTripBreach,
    String dispatchRecommendation,       // OPTIMAL_CHOICE, VIABLE_ALTERNATIVE, HIGH_RISK, NOT_RECOMMENDED
    String recommendationRationale
) {}
