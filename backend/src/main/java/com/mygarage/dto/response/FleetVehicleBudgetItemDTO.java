package com.mygarage.dto.response;

import java.math.BigDecimal;

/**
 * M20: Fleet breakdown item for vehicle fiscal budget allocation.
 */
public record FleetVehicleBudgetItemDTO(
        Long vehicleId,
        String vehicleName,
        String plateNumber,
        BigDecimal monthlyBurnRate,
        BigDecimal annualProjectedExpense,
        BigDecimal recommendedLiquidityBuffer,
        BigDecimal budgetSharePercentage,       // % of garage burn
        String volatilityTier
) {}
