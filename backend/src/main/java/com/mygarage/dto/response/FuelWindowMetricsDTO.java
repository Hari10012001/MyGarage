package com.mygarage.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Metrics for a single rolling time window (30 / 60 / 90 / 180 days).
 * Used in VehicleFuelAnalyticsDTO.windowMetrics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FuelWindowMetricsDTO {

    /** Window size in days (30, 60, 90, or 180). */
    private int windowDays;

    /** Number of fill-up records that fall within this window. */
    private int recordCount;

    /** Average estimated mileage (km/L) within this window; null if < 2 records. */
    private BigDecimal avgMileageKmpl;

    /** Average cost per litre (₹/L) within this window; null if no records. */
    private BigDecimal avgPricePerLitre;

    /** Total fuel cost (₹) within this window. */
    private BigDecimal totalCost;

    /** Total litres consumed within this window. */
    private BigDecimal totalLitres;

    /**
     * True when there are at least 2 fill-ups in this window,
     * meaning avgMileageKmpl is statistically meaningful.
     */
    private boolean sufficientData;
}
