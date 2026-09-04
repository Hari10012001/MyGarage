package com.mygarage.dto.response;

import com.mygarage.model.FuelRecord;
import com.mygarage.model.MaintenanceRecord;
import com.mygarage.model.ServiceRecord;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleDossierDTO {

    // Vehicle Details
    private Long vehicleId;
    private String plateNumber;
    private String make;
    private String model;
    private Integer year;
    private String color;
    private Integer currentOdometer;
    private String fuelType;
    private String categoryName;
    private String notes;

    // Owner Details
    private String ownerName;
    private String ownerEmail;
    private String ownerPhone;

    // Lifetime Financial Ledger
    private BigDecimal totalServiceCost;
    private BigDecimal totalFuelCost;
    private BigDecimal totalMaintenanceCost;
    private BigDecimal totalOwnershipCost;

    // Operational & Usage Metrics
    private BigDecimal totalFuelLitres;
    private Double averageMileageKmpl;
    private long totalServicesCount;
    private long totalFuelLogsCount;
    private long totalMaintenanceTasksCount;

    // History Lists
    private List<ServiceRecord> services;
    private List<FuelRecord> fuelLogs;
    private List<MaintenanceRecord> maintenanceRecords;

    // Metadata
    private LocalDateTime generatedAt;
    private String verificationStatus;
}
