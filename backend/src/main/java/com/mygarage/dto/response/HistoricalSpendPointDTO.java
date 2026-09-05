package com.mygarage.dto.response;

import java.math.BigDecimal;

/**
 * M20: Historical monthly expenditure point for complete calendar window.
 */
public record HistoricalSpendPointDTO(
        String monthLabel,
        BigDecimal fuelSpend,
        BigDecimal serviceSpend,
        BigDecimal maintenanceSpend,
        BigDecimal totalMonthlySpend
) {}
