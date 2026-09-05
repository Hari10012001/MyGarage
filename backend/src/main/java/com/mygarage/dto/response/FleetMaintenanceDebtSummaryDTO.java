package com.mygarage.dto.response;

import java.math.BigDecimal;

public record FleetMaintenanceDebtSummaryDTO(
    Long vehicleId,
    String plateNumber,
    String make,
    String model,
    Integer year,
    Integer currentOdometer,
    BigDecimal rav,
    BigDecimal deferredDebt,
    Double mdi,
    String deficitStatus,
    BigDecimal compoundExposure,
    BigDecimal nearTermExposure,
    BigDecimal total30DayLiability,
    int backlogCount,
    String topTriageAction
) {}
