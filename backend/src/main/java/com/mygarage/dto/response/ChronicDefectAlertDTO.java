package com.mygarage.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChronicDefectAlertDTO {
    private String subsystem;
    private String subsystemDisplayName;
    private int recurringCount;
    private Long dayGap;
    private Integer odometerGap;
    private String severity; // LOW, MODERATE, HIGH, CRITICAL
    private String diagnosticNote;
}
