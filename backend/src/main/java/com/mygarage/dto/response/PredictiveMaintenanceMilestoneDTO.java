package com.mygarage.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PredictiveMaintenanceMilestoneDTO {

    private int intervalKm;               // e.g. 5000, 10000, 20000, 40000
    private int targetOdometer;           // currentOdometer + kilometersRemaining
    private int kilometersRemaining;       // targetOdometer - currentOdometer
    private String serviceName;           // e.g. "Engine Oil & Filter Service"
    private String description;           // Service scope description
    private String urgency;               // "OVERDUE", "DUE_NOW", "DUE_SOON", "UPCOMING"
    private LocalDate estimatedDueDate;   // Calendar date projected from daily velocity
    private long daysRemaining;           // Days remaining until projected date
    private BigDecimal estimatedCost;     // Estimated cost based on vehicle category
    private boolean isScheduled;          // True if a pending maintenance task with matching title already exists
    private Long existingMaintenanceId;   // ID of existing task if already scheduled
}
