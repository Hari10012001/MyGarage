package com.mygarage.dto.response;

/**
 * M20: Non-persistent data confidence rating and evidence metadata for fiscal forecasts.
 */
public record FiscalDataConfidenceDTO(
        String confidenceRating,                // HIGH, MEDIUM, LOW, BASELINE_ONLY
        int fuelRecordCount,
        int serviceRecordCount,
        int maintenanceRecordCount,
        int historyMonthsEvaluated,
        String fuelEstimationTier,              // TRAILING_90_DAYS, LIFETIME_AVERAGE, CATEGORY_BENCHMARK
        boolean distanceMetricAvailable,
        String odometerStatus                   // NORMAL, ROLLBACK_DETECTED, ZERO_DELTA, NO_DATA
) {}
