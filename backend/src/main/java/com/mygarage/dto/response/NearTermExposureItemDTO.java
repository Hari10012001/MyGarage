package com.mygarage.dto.response;

import java.math.BigDecimal;

public record NearTermExposureItemDTO(
    String id,
    String subsystem,
    String taskTitle,
    int daysUntilDue,
    Integer dueOdometerKm,
    BigDecimal projectedCost,
    String status,
    String advisoryNote
) {}
