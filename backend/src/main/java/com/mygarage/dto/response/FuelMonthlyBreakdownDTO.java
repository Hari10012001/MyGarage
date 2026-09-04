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
public class FuelMonthlyBreakdownDTO {
    private int year;
    private int month;
    private String monthLabel; // e.g., "Jan 2026"
    private BigDecimal totalCost;
    private BigDecimal totalLitres;
    private BigDecimal avgMileageKmpl;
    private int fillUps;
}
