package com.mygarage.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * M21: Comprehensive single-workshop intelligence report.
 */
public record WorkshopAnalyticsReportDTO(
        String workshopKey,
        String workshopName,
        int totalVisits,
        int warrantyVisitCount,
        BigDecimal totalSpend,
        BigDecimal averageVisitCost,
        BigDecimal workshopPriceIndex,
        String pricingCategory,
        BigDecimal vendorReworkProbability,
        int reworkEventCount,
        boolean isPreliminaryData,
        BigDecimal workshopValueScore,
        String valueTier,
        List<WorkshopSubsystemMetricDTO> subsystemMetrics,
        List<WorkshopReworkEventDTO> reworkEvents,
        List<String> vehiclesServiced,
        LocalDate firstVisitDate,
        LocalDate mostRecentVisitDate,
        String recommendation,
        String disclaimer
) {}
