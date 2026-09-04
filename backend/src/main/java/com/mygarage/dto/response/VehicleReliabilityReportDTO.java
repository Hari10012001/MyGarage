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
public class VehicleReliabilityReportDTO {
    private Long vehicleId;
    private String plateNumber;
    private String make;
    private String model;
    private Integer year;
    private Integer currentOdometer;

    private int vriScore;
    private String reliabilityGrade; // EXCELLENT, GOOD, MODERATE, POOR, CRITICAL_RISK
    private int totalServiceVisits;
    private BigDecimal totalServiceSpend;

    private Double mdbfKm;
    private Double mtbsDays;

    private int unscheduledBreakdownCount;
    private BigDecimal unscheduledBreakdownSpend;
    private Double correctiveServiceRatio; // Percentage 0.0 - 100.0

    private int routineMaintenanceCount;
    private BigDecimal routineMaintenanceSpend;

    private List<SubsystemFailureBreakdownDTO> subsystemBreakdowns;
    private List<ChronicDefectAlertDTO> chronicDefectAlerts;
    private List<WorkshopReliabilityDTO> workshops;

    private String serviceAccelerationStatus; // STABLE, IMPROVING, ACCELERATING, INSUFFICIENT_DATA
    private String disclaimer;
}
