package com.mygarage.dto.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * M20: Comprehensive vehicle fiscal budget, burn rate, volatility, and cash flow forecast report.
 */
public record VehicleFiscalBudgetReportDTO(
        Long vehicleId,
        String vehicleName,
        String plateNumber,
        String currency,                        // Always "$"
        BigDecimal rollingMonthlyBurnRate,
        BigDecimal dailyBurnRate,
        BigDecimal distanceBurnRatePer100Km,     // Can be null if distance metric unavailable
        BigDecimal fuelBurnRate,
        BigDecimal maintenanceBurnRate,
        BigDecimal repairBurnRate,
        BigDecimal expenditureVolatilityIndex,  // EVRI 0-100
        String volatilityTier,                  // STABLE, MODERATE, ELEVATED, VOLATILE
        BigDecimal recommendedLiquidityBuffer,
        BigDecimal historicalPeakSingleSpend,
        BigDecimal projectedNextMonthExpense,
        BigDecimal projectedNextQuarterExpense,
        BigDecimal projectedNextYearExpense,
        List<HistoricalSpendPointDTO> historicalSpendTrend,
        List<MonthlyCashFlowPointDTO> twelveMonthForecast,
        FiscalDataConfidenceDTO dataConfidence,
        String disclaimer
) {}
