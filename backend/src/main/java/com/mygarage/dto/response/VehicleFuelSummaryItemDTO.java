package com.mygarage.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleFuelSummaryItemDTO {
    private Long vehicleId;
    private String plateNumber;
    private String displayName;
    private String fuelType;
    private int totalFillUps;
    private BigDecimal totalFuelCost;
    private BigDecimal totalLitres;
    private BigDecimal avgMileageKmpl;

    // Automated Badges
    private boolean mostFuelEfficient;
    private boolean lowestFuelCost;
    private boolean highestLitresConsumed;
}
