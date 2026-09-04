package com.mygarage.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseForecastDTO {

    private int periodDays;                     // 30, 60, 90, 180 days
    private int projectedMileageKm;             // Estimated distance driven in horizon
    private BigDecimal projectedFuelSpend;      // Estimated fuel expense
    private BigDecimal projectedMaintenanceSpend; // Sum of milestones due within this exact horizon
    private BigDecimal totalProjectedSpend;     // Fuel + Maintenance combined
    private int dueMilestonesCount;             // Count of milestones falling inside this window
}
