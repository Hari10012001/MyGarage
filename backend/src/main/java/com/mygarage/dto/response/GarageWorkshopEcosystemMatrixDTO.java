package com.mygarage.dto.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * M21: Fleet-wide workshop ecosystem matrix and vendor concentration report.
 */
public record GarageWorkshopEcosystemMatrixDTO(
        Long userId,
        int totalUniqueWorkshops,
        int totalFleetServiceVisits,
        BigDecimal totalFleetServiceSpend,
        String topPreferredWorkshopName,
        BigDecimal fleetHhiIndex,
        String concentrationTier,
        List<WorkshopAnalyticsReportDTO> rankedWorkshops,
        List<String> ecosystemInsights,
        String disclaimer
) {}
