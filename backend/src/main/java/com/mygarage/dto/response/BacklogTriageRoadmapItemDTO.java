package com.mygarage.dto.response;

import java.math.BigDecimal;

public record BacklogTriageRoadmapItemDTO(
    int triageRank,
    String id,
    String taskTitle,
    String subsystem,
    BigDecimal immediateCost,
    BigDecimal preventedExposure,
    BigDecimal netSavings,
    Double rmeScore,
    String triageAction,
    String urgencyRationale
) {}
