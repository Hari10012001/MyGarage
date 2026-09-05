package com.mygarage.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record VehicleMaintenanceDeficitReportDTO(
    Long vehicleId,
    String plateNumber,
    String make,
    String model,
    Integer year,
    Integer currentOdometer,
    BigDecimal replacementAssetValue,
    BigDecimal deferredMaintenanceDebt,
    Double maintenanceDeficitIndex,
    String deficitStatus,
    BigDecimal compoundNeglectCostExposure,
    Double inactionMultiplier,
    BigDecimal totalNearTermExposure,
    BigDecimal total30DayMaintenanceLiability,
    int backlogItemCount,
    int nearTermItemCount,
    List<DeferredBacklogItemDTO> backlogItems,
    List<NearTermExposureItemDTO> nearTermItems,
    List<BacklogTriageRoadmapItemDTO> triageRoadmap,
    String executiveSummary,
    String analyticalDisclaimer,
    String benchmarkPolicyNote
) {}
