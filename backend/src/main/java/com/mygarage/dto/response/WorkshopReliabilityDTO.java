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
public class WorkshopReliabilityDTO {
    private String workshopName;
    private int visitCount;
    private BigDecimal totalSpend;
    private BigDecimal averageCostPerVisit;
    private Double meanReturnIntervalDays;
}
