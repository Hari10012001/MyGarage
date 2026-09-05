package com.mygarage.dto.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * M20: Garage-wide consolidated fleet fiscal budget matrix and cash flow calendar.
 */
public record GarageFiscalBudgetMatrixDTO(
        Long userId,
        int totalVehicles,
        String currency,                        // Always "$"
        BigDecimal totalPortfolioMonthlyBurn,
        BigDecimal totalPortfolioAnnualProjected,
        BigDecimal totalRecommendedFleetBuffer,
        List<FleetVehicleBudgetItemDTO> vehicleBreakdown,
        List<MonthlyCashFlowPointDTO> consolidatedFleetForecast,
        String disclaimer
) {}
