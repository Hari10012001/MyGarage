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
public class GarageReliabilityMatrixDTO {
    private int averageGarageVri;
    private String fleetReliabilityGrade;
    private Double fleetMdbfKm;
    private BigDecimal fleetTotalBreakdownSpend;
    private int fleetTotalServiceVisits;

    private VehicleReliabilitySummaryItemDTO mostReliableVehicle;
    private VehicleReliabilitySummaryItemDTO highestRiskVehicle;

    private List<VehicleReliabilitySummaryItemDTO> vehicleSummaries;
    private List<SubsystemFailureBreakdownDTO> subsystemSpendDistribution;
    private List<String> recommendations;
    private String disclaimer;
}
