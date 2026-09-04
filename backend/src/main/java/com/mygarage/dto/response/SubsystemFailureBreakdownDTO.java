package com.mygarage.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubsystemFailureBreakdownDTO {
    private String subsystem;
    private String displayName;
    private int recordCount;
    private BigDecimal totalCost;
    private Double spendPercentage;
    private LocalDate lastRepairDate;
}
