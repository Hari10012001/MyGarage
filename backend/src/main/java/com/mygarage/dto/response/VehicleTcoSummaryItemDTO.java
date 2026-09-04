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
public class VehicleTcoSummaryItemDTO {
    private Long vehicleId;
    private String plateNumber;
    private String make;
    private String model;
    private String categoryName;
    private BigDecimal estimatedResidualValue;
    private BigDecimal annualizedOperatingCost;
    private double repairToResidualValueRatio;
    private String advisoryStatus;
}
