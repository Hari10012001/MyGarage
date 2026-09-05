package com.mygarage.dto.response;

import java.math.BigDecimal;

public record TripFuelStagingDTO(
    Double tripDistanceKm,
    Double estimatedConsumptionPer100Km,
    Double estimatedFuelNeededLiters,
    BigDecimal estimatedFuelCost,
    Double estimatedCruisingRangeKm,
    Integer estimatedFuelStopsRequired,
    String fuelType,
    String regimeAdjustmentDescription,
    Boolean isHistoricalDataAvailable
) {}
