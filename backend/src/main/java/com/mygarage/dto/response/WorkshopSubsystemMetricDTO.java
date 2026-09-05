package com.mygarage.dto.response;

import java.math.BigDecimal;

/**
 * M21: Subsystem-level cost benchmarking and price variance against MyGarage reference benchmarks.
 */
public record WorkshopSubsystemMetricDTO(
        String subsystem,
        String displayName,
        int serviceCount,
        BigDecimal totalSpend,
        BigDecimal averageCost,
        BigDecimal referenceBenchmarkCost,
        BigDecimal costVariancePercentage
) {}
