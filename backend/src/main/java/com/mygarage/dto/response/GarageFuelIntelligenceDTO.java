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
public class GarageFuelIntelligenceDTO {
    private List<VehicleFuelSummaryItemDTO> vehicleSummaries;
    private BigDecimal garageLifetimeFuelCost;
    private BigDecimal garageLifetimeLitres;
    private Double garageFleetAvgMileage;
    private List<FuelMonthlyBreakdownDTO> garageMonthlyBreakdown;
    
    // True if the user has vehicles but zero fuel records across the entire garage
    private boolean noFuelData;
}
