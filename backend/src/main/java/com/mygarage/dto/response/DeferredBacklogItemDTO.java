package com.mygarage.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record DeferredBacklogItemDTO(
    String id,
    String subsystem,
    String taskTitle,
    String originType,
    int daysOverdue,
    Double mileageOverdueKm,
    BigDecimal directRemediationCost,
    Double neglectCascadeMultiplier,
    BigDecimal compoundNeglectCostExposure,
    String primaryConsequence,
    List<String> secondaryConsequences,
    Double riskMitigationEfficiency,
    String priority,
    String sourceDescription
) {}
