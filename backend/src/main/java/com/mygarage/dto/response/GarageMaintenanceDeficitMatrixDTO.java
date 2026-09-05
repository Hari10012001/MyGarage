package com.mygarage.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record GarageMaintenanceDeficitMatrixDTO(
    BigDecimal totalFleetRAV,
    BigDecimal totalFleetDeferredDebt,
    Double garageFleetMDI,
    String fleetDeficitStatus,
    BigDecimal totalCompoundExposure,
    BigDecimal totalNearTermExposure,
    BigDecimal totalFleet30DayLiability,
    int totalVehicles,
    int criticalVehiclesCount,
    int deficientVehiclesCount,
    int fairVehiclesCount,
    int optimalVehiclesCount,
    List<FleetMaintenanceDebtSummaryDTO> vehicleSummaries,
    List<BacklogTriageRoadmapItemDTO> priorityTriageQueue,
    String fleetExecutiveAdvisory,
    String analyticalDisclaimer,
    String benchmarkPolicyNote
) {}
