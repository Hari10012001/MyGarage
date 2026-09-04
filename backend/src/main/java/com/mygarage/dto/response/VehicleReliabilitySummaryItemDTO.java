package com.mygarage.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleReliabilitySummaryItemDTO {
    private Long vehicleId;
    private String plateNumber;
    private String make;
    private String model;
    private Integer year;
    private int vriScore;
    private String reliabilityGrade;
    private int totalVisits;
    private Double mdbfKm;
    private String breakdownRisk; // LOW, MODERATE, ELEVATED, HIGH
}
