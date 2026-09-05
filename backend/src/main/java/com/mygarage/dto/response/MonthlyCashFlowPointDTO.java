package com.mygarage.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * M20: Single monthly forward-looking cash flow forecast point.
 */
public record MonthlyCashFlowPointDTO(
        String monthLabel,
        LocalDate monthStartDate,
        BigDecimal fuelExpense,
        BigDecimal scheduledMaintenanceExpense,
        BigDecimal projectedWearExpense,
        BigDecimal totalProjectedExpense,
        List<String> plannedActionItems
) {}
