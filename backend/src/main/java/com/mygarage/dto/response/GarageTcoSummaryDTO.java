package com.mygarage.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GarageTcoSummaryDTO {
    private int totalVehicles;
    private BigDecimal totalFleetResidualValue;
    private BigDecimal totalFleetAnnualOperatingCost;
    private BigDecimal totalFleetDirectCarbonTonnes;
    private int healthyRetentionCount;
    private int moderateExpenseCount;
    private int replacementWatchlistCount;
    private int disposalRecommendedCount;
    private List<VehicleTcoSummaryItemDTO> vehicles;
}
